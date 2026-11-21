package vn.ptit.tour.qa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC38 opt-in measurement, run explicitly with -Dtest=ModuleAPerformanceIT.
 * Real HTTP, production services/DAO, and an isolated H2 or PostgreSQL database.
 * No latency threshold is asserted: TC38 acceptance remains BLOCKED pending ADR.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.address=127.0.0.1", "spring.h2.console.enabled=false"})
@Import(ModuleAPerformanceIT.QueryCaptureConfiguration.class)
class ModuleAPerformanceIT {

    private static final int WARMUP_REQUESTS = 20;
    private static final int MEASURED_REQUESTS = 100;
    private static final String DATABASE = System.getProperty("qa.perf.database", "h2");
    private static final String POSTGRES_URL = System.getProperty("qa.perf.jdbcUrl",
            "jdbc:postgresql://127.0.0.1:15432/module_a_perf_tc38_20261118");
    private static final String H2_URL = "jdbc:h2:mem:module_a_perf_tc38_" + UUID.randomUUID()
            + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String ROUTE = "/api/v1/tour-recommendations"
            + "?interestCodes=BEACH&interestCodes=FOOD&availableOnly=true&page=1&size=10";
    private static final List<String> EXPECTED_FIRST_IDS = List.of("1000100", "1000200", "1000300",
            "1000400", "1000500", "1000600", "1000700", "1000800", "1000900", "1001000");
    private static final List<String> TABLES = List.of("app_users", "destinations", "tours",
            "tour_versions", "tour_version_destinations", "tour_interests", "tour_version_interest_tags",
            "tour_media", "tour_departures", "departure_offer_versions");
    private static final Path OUTPUT = Path.of(System.getProperty("qa.perf.output",
            "target/qa-performance/" + DATABASE)).toAbsolutePath().normalize();

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CapturingJdbcTemplate capturingJdbc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        if (!DATABASE.equals("h2") && !DATABASE.equals("postgres")) {
            throw new IllegalArgumentException("qa.perf.database must be h2 or postgres");
        }
        if (DATABASE.equals("postgres") && !POSTGRES_URL.matches(
                "jdbc:postgresql://127\\.0\\.0\\.1:15432/module_a_perf_tc38_[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("TC38 may seed only its own database on port 15432");
        }
        registry.add("spring.datasource.url", () -> DATABASE.equals("postgres") ? POSTGRES_URL : H2_URL);
        registry.add("spring.datasource.driver-class-name", () -> DATABASE.equals("postgres")
                ? "org.postgresql.Driver" : "org.h2.Driver");
        registry.add("spring.datasource.username", () -> DATABASE.equals("postgres") ? "qa_module_a" : "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("app.demo.password", () -> "");
        String testOnlySigningKey = UUID.randomUUID().toString() + UUID.randomUUID();
        registry.add("app.jwt.secret", () -> testOnlySigningKey);
    }

    /**
     * Fails if the real route loses candidates, serializes incorrect rank/price, or cannot
     * execute its exact SQL on the selected engine. A completed measurement is not an SLA PASS.
     */
    @Test
    void measureRealRecommendationsAtOneAndTenThousandTours() throws Exception {
        Files.createDirectories(OUTPUT);
        Map<String, Object> environment = environment();
        writeJson(OUTPUT.resolve("environment.json"), environment);
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
                .version(HttpClient.Version.HTTP_1_1).build()) {
            List<Map<String, Object>> datasets = new ArrayList<>();
            for (int tourCount : List.of(1_000, 10_000)) {
                datasets.add(measureDataset(client, tourCount));
                writeJson(OUTPUT.resolve("summary.json"), Map.of(
                        "testId", "TC38", "acceptanceStatus", "BLOCKED",
                        "acceptanceReason", "No approved Module A threshold/workload/environment ADR",
                        "measurementPurpose", "Observational baseline; no latency acceptance assertion",
                        "database", DATABASE, "datasets", datasets, "environmentFile", "environment.json"));
            }
            for (Map<String, Object> dataset : datasets) {
                assertEquals(0L, dataset.get("errors"), "HTTP/functional errors: " + dataset);
                assertEquals(0L, dataset.get("warmupErrors"), "Warmup errors: " + dataset);
            }
        }
    }

    private Map<String, Object> measureDataset(HttpClient client, int tourCount) throws Exception {
        Path datasetDirectory = OUTPUT.resolve(String.valueOf(tourCount));
        Files.createDirectories(datasetDirectory);
        Instant started = Instant.now();
        seed(tourCount, datasetDirectory);
        assertEquals((long) tourCount, jdbc.queryForObject("SELECT COUNT(*) FROM tours", Long.class));
        jdbc.execute("ANALYZE");
        Map<String, Object> initialCounts = counts();
        writeJson(datasetDirectory.resolve("counts-before.json"), initialCounts);
        capturingJdbc.last.set(null);
        URI endpoint = URI.create("http://127.0.0.1:" + port + ROUTE);
        List<Map<String, Object>> warmups = new ArrayList<>();
        List<Map<String, Object>> samples = new ArrayList<>();
        for (int i = 1; i <= WARMUP_REQUESTS; i++) {
            warmups.add(request(client, endpoint, tourCount, "warmup", i, datasetDirectory));
        }
        for (int i = 1; i <= MEASURED_REQUESTS; i++) {
            samples.add(request(client, endpoint, tourCount, "measured", i, datasetDirectory));
        }
        writeJson(datasetDirectory.resolve("warmup.json"), warmups);
        writeJson(datasetDirectory.resolve("measurements.json"), samples);
        writeLatencies(datasetDirectory, warmups, samples);
        QueryCapture capture = capturingJdbc.last.get();
        assertNotNull(capture, "No production candidate SQL was observed");
        assertEquals(tourCount, capture.candidateCount(), "Production DAO candidate count");
        Files.writeString(datasetDirectory.resolve("candidate.sql"), capture.sql(), StandardCharsets.UTF_8);
        writeJson(datasetDirectory.resolve("candidate-parameters.json"), capture.parameters());
        String queryHash = sha256(capture.sql().getBytes(StandardCharsets.UTF_8));
        MapSqlParameterSource parameters = new MapSqlParameterSource(capture.parameters());
        String explainPrefix = DATABASE.equals("postgres") ? "EXPLAIN (ANALYZE, BUFFERS) " : "EXPLAIN ANALYZE ";
        List<String> plan = capturingJdbc.query(explainPrefix + capture.sql(), parameters,
                (rs, rowNum) -> rs.getString(1));
        Files.writeString(datasetDirectory.resolve("explain-analyze.txt"), String.join("\n", plan),
                StandardCharsets.UTF_8);
        Map<String, Object> afterCounts = counts();
        writeJson(datasetDirectory.resolve("counts-after.json"), afterCounts);
        assertEquals(initialCounts, afterCounts, "Read-only benchmark changed table counts");
        long errors = samples.stream().filter(s -> s.get("error") != null).count();
        long warmupErrors = warmups.stream().filter(s -> s.get("error") != null).count();
        List<Double> allTimes = samples.stream().map(s -> (Double) s.get("elapsedMs")).sorted().toList();
        List<Double> successfulTimes = samples.stream().filter(s -> s.get("error") == null)
                .map(s -> (Double) s.get("elapsedMs")).sorted().toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("testId", "TC38");
        result.put("acceptanceStatus", "BLOCKED");
        result.put("measurementRunResult", errors == 0 && warmupErrors == 0 ? "COMPLETED" : "REQUEST_ERRORS");
        result.put("database", DATABASE);
        result.put("tourCount", tourCount);
        result.put("candidateCount", capture.candidateCount());
        result.put("warmupRequests", WARMUP_REQUESTS);
        result.put("measuredRequests", MEASURED_REQUESTS);
        result.put("concurrency", 1);
        result.put("request", endpoint.toString());
        result.put("startedAt", started.toString());
        result.put("finishedAt", Instant.now().toString());
        result.put("errors", errors);
        result.put("warmupErrors", warmupErrors);
        result.put("errorRatePercent", 100d * errors / MEASURED_REQUESTS);
        result.put("p50Ms", percentile(allTimes, 0.50));
        result.put("p95Ms", percentile(allTimes, 0.95));
        result.put("p99Ms", percentile(allTimes, 0.99));
        result.put("maxMs", allTimes.getLast());
        result.put("p95SuccessfulMs", successfulTimes.isEmpty() ? null : percentile(successfulTimes, 0.95));
        result.put("percentileMethod", "Nearest rank: sorted[ceil(p * N) - 1], all measured requests included");
        result.put("timingBoundary", "System.nanoTime before HttpClient.send through complete UTF-8 response body; validation excluded");
        result.put("querySha256", queryHash);
        result.put("candidateParameters", capture.parameters());
        result.put("explainCommand", explainPrefix.trim());
        result.put("explainFile", tourCount + "/explain-analyze.txt");
        result.put("limitations", "Single local server/client, warm DB, sequential workload; no production SLA or concurrency conclusion");
        writeJson(datasetDirectory.resolve("result.json"), result);
        System.out.printf("TC38 baseline %s: tours=%d candidates=%d samples=%d p95=%.3f ms errors=%d acceptance=BLOCKED%n",
                DATABASE, tourCount, capture.candidateCount(), MEASURED_REQUESTS, result.get("p95Ms"), errors);
        return result;
    }

    private Map<String, Object> request(HttpClient client, URI endpoint, int tourCount, String phase,
                                       int index, Path datasetDirectory) throws IOException {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("phase", phase);
        sample.put("index", index);
        sample.put("requestedAt", Instant.now().toString());
        sample.put("status", null);
        sample.put("error", null);
        long start = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(180))
                    .header("Accept", "application/json").GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            sample.put("elapsedMs", (System.nanoTime() - start) / 1_000_000d);
            sample.put("status", response.statusCode());
            sample.put("responseBytes", response.body().getBytes(StandardCharsets.UTF_8).length);
            sample.put("httpVersion", response.version().toString());
            if (index == 1 || (phase.equals("measured") && index == MEASURED_REQUESTS)) {
                writeJson(datasetDirectory.resolve(phase + "-response-" + index + ".json"), Map.of(
                        "method", "GET", "url", endpoint.toString(), "status", response.statusCode(),
                        "headers", response.headers().map(), "body", response.body()));
            }
            if (response.statusCode() != 200) {
                sample.put("error", "HTTP " + response.statusCode());
            } else {
                JsonNode body = mapper.readTree(response.body());
                JsonNode items = body.at("/data/items");
                int total = body.at("/data/pagination/totalItems").asInt(-1);
                sample.put("totalItems", total);
                sample.put("asOf", body.at("/meta/asOf").asText());
                List<String> ids = new ArrayList<>();
                items.forEach(item -> ids.add(item.path("tourId").asText()));
                boolean correct = body.path("success").asBoolean() && total == tourCount
                        && ids.equals(EXPECTED_FIRST_IDS) && items.size() == 10;
                for (JsonNode item : items) {
                    correct &= item.path("matchScore").asDouble(-1) == 100d
                            && item.path("priceFrom").isTextual() && item.path("priceFrom").asText().equals("2000000");
                }
                if (!correct) {
                    sample.put("error", "Candidate total/ranking/score/money mismatch; total=" + total + "; ids=" + ids);
                    writeJson(datasetDirectory.resolve(phase + "-error-" + index + ".json"), body);
                }
            }
        } catch (Exception exception) {
            sample.putIfAbsent("elapsedMs", (System.nanoTime() - start) / 1_000_000d);
            sample.put("error", exception.getClass().getName() + ": " + exception.getMessage());
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return sample;
    }

    /**
     * Isolated synthetic data: exactly N tours, N departures/offers/destination mappings.
     * Four tag patterns yield 100/50/50/0. Price=2m+(i%25)*10k. Therefore the
     * first ten rows are hand-derived IDs 1000100..1001000, all score100 and price2m.
     */
    private void seed(int tourCount, Path datasetDirectory) throws IOException {
        List<Integer> indices = IntStream.rangeClosed(1, tourCount).boxed().toList();
        StringBuilder script = new StringBuilder("-- TC38 synthetic fixture, no real customer data.\n"
                + "-- Run only in a fresh isolated module_a_perf_tc38_* database.\nBEGIN;\n");
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            executeSeed(script, "UPDATE tours SET current_published_version_id = NULL");
            executeSeed(script, "UPDATE tour_departures SET active_offer_id = NULL");
            for (String table : List.of("departure_offer_versions", "tour_departures", "tour_media",
                    "tour_version_interest_tags", "tour_version_destinations", "tour_versions", "tours",
                    "tour_interests", "destinations", "app_users")) {
                executeSeed(script, "DELETE FROM " + table);
            }
            executeSeed(script, "INSERT INTO destinations (destination_id,name,slug,destination_type,is_active) "
                    + "VALUES ('QA_PERF','TC38 synthetic destination','tc38-perf-destination','CITY',TRUE)");
            executeSeed(script, "INSERT INTO tour_interests (interest_id,code,name,is_active,display_order) VALUES "
                    + "(1,'BEACH','Beach',TRUE,1),(2,'FOOD','Food',TRUE,2),(3,'MOUNTAIN','Mountain',TRUE,3)");
            String tourSql = "INSERT INTO tours (tour_id,tour_code,slug,lifecycle_status) VALUES (?,?,?,'ACTIVE')";
            jdbc.batchUpdate(tourSql, indices, 500, (ps, i) -> {
                ps.setLong(1, 1_000_000L + i);
                ps.setString(2, "QA-PERF-" + i);
                ps.setString(3, "qa-perf-" + i);
            });
            for (int i : indices) {
                script.append("INSERT INTO tours (tour_id,tour_code,slug,lifecycle_status) VALUES (")
                        .append(1_000_000L + i).append(",'QA-PERF-").append(i).append("','qa-perf-")
                        .append(i).append("','ACTIVE');\n");
            }
            String versionSql = "INSERT INTO tour_versions (tour_version_id,tour_id,version_no,content_status,"
                    + "title,duration_days,nights) VALUES (?,?,1,'DRAFT',?,3,2)";
            jdbc.batchUpdate(versionSql, indices, 500, (ps, i) -> {
                ps.setLong(1, 2_000_000L + i);
                ps.setLong(2, 1_000_000L + i);
                ps.setString(3, "TC38 Synthetic Tour " + i);
            });
            for (int i : indices) {
                script.append("INSERT INTO tour_versions (tour_version_id,tour_id,version_no,content_status,"
                                + "title,duration_days,nights) VALUES (").append(2_000_000L + i).append(',')
                        .append(1_000_000L + i).append(",1,'DRAFT','TC38 Synthetic Tour ").append(i).append("',3,2);\n");
            }
            jdbc.batchUpdate("INSERT INTO tour_version_destinations (tour_version_id,destination_id,stop_order,is_primary) "
                    + "VALUES (?,'QA_PERF',1,TRUE)", indices, 500, (ps, i) -> ps.setLong(1, 2_000_000L + i));
            List<Object[]> tags = new ArrayList<>();
            for (int i : indices) {
                script.append("INSERT INTO tour_version_destinations (tour_version_id,destination_id,stop_order,is_primary) "
                        + "VALUES (").append(2_000_000L + i).append(",'QA_PERF',1,TRUE);\n");
                for (long interestId : switch (i % 4) {
                    case 0 -> new long[]{1, 2};
                    case 1 -> new long[]{1};
                    case 2 -> new long[]{2};
                    default -> new long[]{3};
                }) {
                    tags.add(new Object[]{2_000_000L + i, interestId});
                    script.append("INSERT INTO tour_version_interest_tags (tour_version_id,interest_id) VALUES (")
                            .append(2_000_000L + i).append(',').append(interestId).append(");\n");
                }
            }
            jdbc.batchUpdate("INSERT INTO tour_version_interest_tags (tour_version_id,interest_id) VALUES (?,?)", tags);
            executeSeed(script, "UPDATE tour_versions SET content_status='PUBLISHED'");
            executeSeed(script, "UPDATE tours SET current_published_version_id=tour_id+1000000");
            String departureSql = "INSERT INTO tour_departures (departure_id,departure_code,tour_id,tour_version_id,"
                    + "starts_at,ends_at,booking_opens_at,booking_closes_at,sales_status,total_seats,"
                    + "sellable_capacity,available_seats,hold_seats,confirmed_seats) VALUES (?,?,?,?,'2027-12-01T00:00:00Z',"
                    + "'2027-12-03T00:00:00Z','2026-01-01T00:00:00Z','2030-01-01T00:00:00Z','OPEN',40,40,40,0,0)";
            jdbc.batchUpdate(departureSql, indices, 500, (ps, i) -> {
                ps.setLong(1, 3_000_000L + i);
                ps.setString(2, "QA-DEP-" + i);
                ps.setLong(3, 1_000_000L + i);
                ps.setLong(4, 2_000_000L + i);
            });
            for (int i : indices) {
                script.append("INSERT INTO tour_departures (departure_id,departure_code,tour_id,tour_version_id,"
                                + "starts_at,ends_at,booking_opens_at,booking_closes_at,sales_status,total_seats,"
                                + "sellable_capacity,available_seats,hold_seats,confirmed_seats) VALUES (")
                        .append(3_000_000L + i).append(",'QA-DEP-").append(i).append("',")
                        .append(1_000_000L + i).append(',').append(2_000_000L + i)
                        .append(",'2027-12-01T00:00:00Z','2027-12-03T00:00:00Z','2026-01-01T00:00:00Z',"
                                + "'2030-01-01T00:00:00Z','OPEN',40,40,40,0,0);\n");
            }
            jdbc.batchUpdate("INSERT INTO departure_offer_versions (offer_id,departure_id,version_no,approval_status,adult_price,currency) "
                    + "VALUES (?,?,1,'APPROVED',?,'VND')", indices, 500, (ps, i) -> {
                ps.setLong(1, 4_000_000L + i);
                ps.setLong(2, 3_000_000L + i);
                ps.setLong(3, 2_000_000L + (i % 25) * 10_000L);
            });
            for (int i : indices) {
                script.append("INSERT INTO departure_offer_versions (offer_id,departure_id,version_no,approval_status,adult_price,currency) VALUES (")
                        .append(4_000_000L + i).append(',').append(3_000_000L + i)
                        .append(",1,'APPROVED',").append(2_000_000L + (i % 25) * 10_000L).append(",'VND');\n");
            }
            executeSeed(script, "UPDATE tour_departures SET active_offer_id=departure_id+1000000");
        });
        script.append("COMMIT;\nANALYZE;\n");
        Files.writeString(datasetDirectory.resolve("seed.sql"), script, StandardCharsets.UTF_8);
    }

    private void executeSeed(StringBuilder script, String sql) {
        jdbc.execute(sql);
        script.append(sql).append(";\n");
    }

    private Map<String, Object> counts() {
        Map<String, Object> counts = new LinkedHashMap<>();
        TABLES.forEach(table -> counts.put(table, jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class)));
        counts.put("availableSeats", jdbc.queryForObject("SELECT COALESCE(SUM(available_seats),0) FROM tour_departures", Long.class));
        counts.put("holdSeats", jdbc.queryForObject("SELECT COALESCE(SUM(hold_seats),0) FROM tour_departures", Long.class));
        counts.put("confirmedSeats", jdbc.queryForObject("SELECT COALESCE(SUM(confirmed_seats),0) FROM tour_departures", Long.class));
        return counts;
    }

    private Map<String, Object> environment() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordedAt", Instant.now().toString());
        result.put("javaVersion", System.getProperty("java.version"));
        result.put("javaVendor", System.getProperty("java.vendor"));
        result.put("osName", System.getProperty("os.name"));
        result.put("osVersion", System.getProperty("os.version"));
        result.put("osArch", System.getProperty("os.arch"));
        result.put("processor", System.getenv().getOrDefault("PROCESSOR_IDENTIFIER", "unreported"));
        result.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        result.put("jvmMaxMemoryBytes", Runtime.getRuntime().maxMemory());
        result.put("serverAddress", "127.0.0.1");
        result.put("serverPort", port);
        result.put("sourceVersionType", "SHA-256 file manifest; repository had no commit at baseline");
        Map<String, String> hashes = new LinkedHashMap<>();
        try (var paths = Files.walk(Path.of("src/main"))) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                hashes.put(path.toString().replace('\\', '/'), sha256(Files.readAllBytes(path)));
            }
        }
        for (String file : List.of("pom.xml", "src/test/java/vn/ptit/tour/qa/ModuleAPerformanceIT.java")) {
            hashes.put(file, sha256(Files.readAllBytes(Path.of(file))));
        }
        result.put("sourceSha256", hashes);
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            result.put("databaseProduct", meta.getDatabaseProductName());
            result.put("databaseVersion", meta.getDatabaseProductVersion());
            result.put("driver", meta.getDriverName());
            result.put("driverVersion", meta.getDriverVersion());
            result.put("jdbcUrl", meta.getURL());
            result.put("databaseUser", meta.getUserName());
        }
        return result;
    }

    private void writeLatencies(Path directory, List<Map<String, Object>> warmups,
                                List<Map<String, Object>> samples) throws IOException {
        StringBuilder csv = new StringBuilder("phase,index,elapsedMs,status,totalItems,error\n");
        for (List<Map<String, Object>> phase : List.of(warmups, samples)) {
            for (Map<String, Object> sample : phase) {
                csv.append(sample.get("phase")).append(',').append(sample.get("index")).append(',')
                        .append(sample.get("elapsedMs")).append(',').append(sample.get("status")).append(',')
                        .append(sample.getOrDefault("totalItems", "")).append(',').append('"')
                        .append(String.valueOf(sample.get("error")).replace("\"", "\"\""))
                        .append('"').append('\n');
            }
        }
        Files.writeString(directory.resolve("latencies.csv"), csv, StandardCharsets.UTF_8);
    }

    private static double percentile(List<Double> sorted, double percentile) {
        return sorted.get(Math.max(0, (int) Math.ceil(percentile * sorted.size()) - 1));
    }

    private void writeJson(Path path, Object value) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    record QueryCapture(String sql, Map<String, Object> parameters, int candidateCount) {
    }

    /** Observer only: executes the production SQL unchanged against the actual DataSource. */
    static class CapturingJdbcTemplate extends NamedParameterJdbcTemplate {
        final AtomicReference<QueryCapture> last = new AtomicReference<>();

        CapturingJdbcTemplate(DataSource dataSource) {
            super(dataSource);
        }

        @Override
        public <T> List<T> query(String sql, SqlParameterSource parameters, RowMapper<T> rowMapper) {
            List<T> result = super.query(sql, parameters, rowMapper);
            if (!sql.startsWith("EXPLAIN") && sql.contains("PARTITION BY t.tour_id")) {
                Map<String, Object> values = new LinkedHashMap<>();
                for (String name : parameters.getParameterNames()) {
                    values.put(name, parameters.getValue(name));
                }
                last.set(new QueryCapture(sql, values, result.size()));
            }
            return result;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class QueryCaptureConfiguration {
        @Bean
        @Primary
        CapturingJdbcTemplate capturedProductionQueries(DataSource dataSource) {
            return new CapturingJdbcTemplate(dataSource);
        }
    }
}
