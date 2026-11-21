package vn.ptit.tour.qa;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import vn.ptit.tour.repository.AppUserRepository;
import vn.ptit.tour.security.JwtService;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:modulea_cases;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.session.events.log=false"})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
@Import(ModuleARegressionTest.FixedTime.class)
class ModuleARegressionTest extends ModuleAEvidenceSupport {

    @Autowired private AppUserRepository users;
    @Autowired private JwtService tokens;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private Clock clock;

    @TestConfiguration
    static class FixedTime {
        @Bean @Primary Clock qaClock() { return Clock.fixed(Instant.now(), ZoneOffset.UTC); }
    }

    private String token(String username) {
        return "Bearer " + tokens.generate(users.findByUsername(username).orElseThrow());
    }

    private List<JsonNode> items(JsonNode response) {
        return StreamSupport.stream(response.path("data").path("items").spliterator(), false).toList();
    }

    private List<String> ids(JsonNode response) {
        return items(response).stream().map(i -> i.path("tourId").asText()).toList();
    }

    private JsonNode find(JsonNode response, String id) {
        return items(response).stream().filter(i -> id.equals(i.path("tourId").asText())).findFirst().orElseThrow();
    }

    /** Creates synthetic data with IDs distinct from demo seed, rolled back after each test. */
    private void tour(long id, int days, long price, int... interests) {
        sql("INSERT INTO tours(tour_id,tour_code,slug) VALUES (?,?,?)", id, "QA" + id, "qa-" + id);
        sql("INSERT INTO tour_versions(tour_version_id,tour_id,version_no,content_status,title,duration_days,nights) VALUES (?,?,1,'PUBLISHED',?, ?,0)", id, id, "QA Tour " + id, days);
        sql("UPDATE tours SET current_published_version_id=? WHERE tour_id=?", id, id);
        sql("INSERT INTO tour_departures(departure_id,departure_code,tour_id,tour_version_id,starts_at,ends_at,booking_opens_at,booking_closes_at,sales_status,total_seats,sellable_capacity,available_seats) VALUES (?,?,?,?,CURRENT_TIMESTAMP + INTERVAL '20' DAY,CURRENT_TIMESTAMP + INTERVAL '23' DAY,CURRENT_TIMESTAMP - INTERVAL '10' DAY,CURRENT_TIMESTAMP + INTERVAL '10' DAY,'OPEN',20,20,20)", id, "QA-D" + id, id, id);
        sql("INSERT INTO departure_offer_versions(offer_id,departure_id,version_no,approval_status,adult_price) VALUES (?,?,1,'APPROVED',?)", id, id, price);
        sql("UPDATE tour_departures SET active_offer_id=? WHERE departure_id=?", id, id);
        for (int interest : interests) {
            sql("INSERT INTO tour_version_interest_tags(tour_version_id,interest_id) VALUES (?,?)", id, interest);
        }
    }

    @Test void tc01DraftVisibility() throws Exception {
        sql("UPDATE tour_versions SET content_status='DRAFT' WHERE tour_version_id=205");
        sql("UPDATE tours SET current_published_version_id=205 WHERE tour_id=105");
        assertFalse(ids(call(get("/api/v1/tours"), 200)).contains("105"));
        call(get("/api/v1/tours/105"), 404);
        assertEquals("201", call(get("/api/v1/tours/101"), 200).at("/data/tourVersionId").asText());
    }

    @Test void tc02ClosedDeparture() throws Exception {
        sql("UPDATE tour_departures SET sales_status='CLOSED' WHERE tour_id=101");
        assertFalse(ids(call(get("/api/v1/tours").param("destinationId", "DN"), 200)).contains("101"));
        assertFalse(ids(call(get("/api/v1/tours").param("destinationId", "DN").param("availableOnly", "false"), 200)).contains("101"));
    }

    @Test void tc03CheapestEligiblePrice() throws Exception {
        JsonNode r = call(get("/api/v1/tours").param("destinationId", "DN"), 200);
        assertEquals("4200000", find(r, "101").path("priceFrom").asText());
        assertEquals("301", find(r, "101").path("representativeDepartureId").asText());
        assertEquals("4400000", find(call(get("/api/v1/tours").param("destinationId", "NT"), 200), "110").path("priceFrom").asText());
        assertEquals("3900000", find(call(get("/api/v1/tours").param("destinationId", "NT").param("availableOnly", "false"), 200), "110").path("priceFrom").asText());
    }

    @Test void tc04PendingOfferIgnored() throws Exception {
        observe("Approved/PENDING offers for departure301", jdbc.queryForList("SELECT offer_id,approval_status,adult_price FROM departure_offer_versions WHERE departure_id=301 ORDER BY offer_id"));
        assertEquals("4200000", find(call(get("/api/v1/tours").param("destinationId", "DN"), 200), "101").path("priceFrom").asText());
        sql("UPDATE tour_departures SET active_offer_id=602 WHERE departure_id=301");
        assertEquals("4600000", find(call(get("/api/v1/tours").param("destinationId", "DN"), 200), "101").path("priceFrom").asText());
    }

    @Test void tc05BudgetBoundary() throws Exception {
        assertTrue(ids(call(get("/api/v1/tours").param("destinationId", "DN").param("maxBudget", "5000000"), 200)).contains("113"));
        assertFalse(ids(call(get("/api/v1/tours").param("destinationId", "DN").param("maxBudget", "4999999"), 200)).contains("113"));
    }

    @Test void tc06UnsupportedMinBudget() throws Exception {
        assertEquals("INVALID_FILTER", call(get("/api/v1/tours").param("minBudget", "6000000").param("maxBudget", "5000000"), 400).at("/error/code").asText());
    }

    @Test void tc07Duration() throws Exception {
        var r = call(get("/api/v1/tours").param("durationDays", "3"), 200);
        assertTrue(items(r).stream().allMatch(i -> i.path("durationDays").asInt() == 3));
        assertFalse(ids(r).contains("111"));
        assertTrue(ids(call(get("/api/v1/tours").param("durationDays", "2"), 200)).contains("111"));
    }

    @Test void tc08MultipleDestinations() throws Exception {
        sql("INSERT INTO tour_version_destinations(tour_version_id,destination_id,stop_order) VALUES (201,'HUE',3)");
        var result = ids(call(get("/api/v1/tours").param("destinationId", "DN"), 200));
        assertEquals(1, result.stream().filter("101"::equals).count());
        assertTrue(ids(call(get("/api/v1/tours").param("destinationId", "HUE"), 200)).contains("101"));
    }

    @Test void tc09HalfScore() throws Exception {
        sql("DELETE FROM tour_version_interest_tags WHERE tour_version_id=201 AND interest_id<>1");
        var tour = find(call(get("/api/v1/tour-recommendations").param("destinationId", "DN").param("interestCodes", "BEACH", "FOOD"), 200), "101");
        assertEquals(50.0, tour.path("matchScore").asDouble());
        assertEquals(1, tour.path("matchedInterests").size());
    }

    @Test void tc10ZeroScoreRetained() throws Exception {
        var tour = find(call(get("/api/v1/tour-recommendations").param("interestCodes", "BEACH").param("destinationId", "SAPA"), 200), "109");
        assertEquals(0.0, tour.path("matchScore").asDouble());
    }

    @Test void tc11NoInterests() throws Exception {
        var r = call(get("/api/v1/tour-recommendations"), 200);
        assertFalse(r.at("/meta/personalized").asBoolean());
        assertTrue(items(r).stream().allMatch(i -> i.path("matchScore").isNull()));
        var prices = items(r).stream().map(i -> new BigDecimal(i.path("priceFrom").asText())).toList();
        assertEquals(prices.stream().sorted().toList(), prices);
        assertEquals("PRICE_ASC", r.at("/meta/sort").asText());
    }

    @Test void tc12DeduplicateQuery() throws Exception {
        var a = call(get("/api/v1/tour-recommendations").param("interestCodes", "BEACH", " beach "), 200);
        var b = call(get("/api/v1/tour-recommendations").param("interestCodes", "BEACH"), 200);
        assertEquals(a.at("/data"), b.at("/data"));
        assertEquals(1, a.at("/meta/appliedInterestCodes").size());
    }

    @Test void tc13DescendingScore() throws Exception {
        var r = call(get("/api/v1/tour-recommendations").param("interestCodes", "BEACH", "CULTURE"), 200);
        var scores = items(r).stream().map(i -> i.path("matchScore").asDouble()).toList();
        assertTrue(scores.containsAll(List.of(100.0, 50.0, 0.0)));
        assertEquals(scores.stream().sorted(Comparator.reverseOrder()).toList(), scores);
    }

    @Test void tc14TieBreakNumericId() throws Exception {
        tour(9007199254740993L, 3, 3000000, 1);
        tour(9007199254740992L, 3, 3000000, 1);
        tour(9100, 3, 4000000, 1);
        var r = call(get("/api/v1/tour-recommendations").param("keyword", "QA Tour").param("interestCodes", "BEACH"), 200);
        assertEquals(List.of("9007199254740992", "9007199254740993", "9100"), ids(r));
    }

    @Test void tc15SortInjectionRejected() throws Exception {
        var before = jdbc.queryForObject("SELECT COUNT(*) FROM tours", Long.class);
        call(get("/api/v1/tours").param("sort", "drop table tours;--"), 400);
        call(get("/api/v1/tour-recommendations").param("sort", "price desc;drop table tours"), 400);
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM tours", Long.class));
        observe("Tours count unchanged after injection", before);
    }

    @Test void tc16SizeBounds() throws Exception {
        for (String size : List.of("0", "51", "11", "-1")) {
            assertEquals("INVALID_PAGINATION", call(get("/api/v1/tours").param("size", size), 400).at("/error/code").asText());
        }
        call(get("/api/v1/tours").param("size", "1"), 200);
        call(get("/api/v1/tours").param("size", "10"), 200);
    }

    @Test void tc17GlobalRankBeforePagination() throws Exception {
        for (int i = 0; i < 13; i++) tour(5000 + i, 3, 1000000 + i, i == 12 ? new int[]{1, 7} : new int[]{1});
        var first = call(get("/api/v1/tour-recommendations").param("keyword", "QA Tour").param("interestCodes", "BEACH", "FOOD").param("size", "10"), 200);
        var second = call(get("/api/v1/tour-recommendations").param("keyword", "QA Tour").param("interestCodes", "BEACH", "FOOD").param("size", "10").param("page", "2"), 200);
        assertEquals("5012", ids(first).getFirst());
        var combined = new ArrayList<>(ids(first)); combined.addAll(ids(second));
        assertEquals(13, combined.size()); assertEquals(13, combined.stream().distinct().count());
        assertEquals(13, first.at("/data/pagination/totalItems").asInt());
    }

    @Test void tc18NoDuplicateJoins() throws Exception {
        tour(5100, 3, 4200000, 1, 2, 3, 4, 5);
        for (String destination : List.of("DN", "HUE", "HOIAN")) sql("INSERT INTO tour_version_destinations(tour_version_id,destination_id) VALUES (5100,?)", destination);
        var r = call(get("/api/v1/tour-recommendations").param("destinationId", "DN").param("interestCodes", "BEACH", "FOOD"), 200);
        assertEquals(1, ids(r).stream().filter("5100"::equals).count());
    }

    @Test void tc19NoValidPrice() throws Exception {
        sql("UPDATE departure_offer_versions SET approval_status='PENDING' WHERE departure_id IN (SELECT departure_id FROM tour_departures WHERE tour_id=101)");
        assertFalse(ids(call(get("/api/v1/tours").param("destinationId", "DN"), 200)).contains("101"));
        assertTrue(call(get("/api/v1/tours/101"), 200).at("/data/priceFrom").isNull());
    }

    @Test void tc20DetailReadsNewPrice() throws Exception {
        assertEquals("4200000", find(call(get("/api/v1/tours").param("destinationId", "DN"), 200), "101").path("priceFrom").asText());
        sql("INSERT INTO departure_offer_versions(offer_id,departure_id,version_no,approval_status,adult_price) VALUES (7000,301,3,'APPROVED',4500000)");
        sql("UPDATE tour_departures SET active_offer_id=7000 WHERE departure_id=301");
        assertEquals("4500000", call(get("/api/v1/tours/101"), 200).at("/data/priceFrom").asText());
    }

    @Test void tc21RolesCannotMutateTags() throws Exception {
        var before = jdbc.queryForList("SELECT interest_id FROM tour_version_interest_tags WHERE tour_version_id=215 ORDER BY interest_id");
        for (String user : List.of("khach01", "manager01", "admin01")) {
            call(put("/api/v1/internal/tour-versions/215/interests").header("Authorization", token(user)).contentType(MediaType.APPLICATION_JSON).content("{\"interestCodes\":[\"BEACH\"]}"), 403);
        }
        var after = jdbc.queryForList("SELECT interest_id FROM tour_version_interest_tags WHERE tour_version_id=215 ORDER BY interest_id");
        observe("Mappings before", before); observe("Mappings after", after); assertEquals(before, after);
        call(put("/api/v1/internal/tour-versions/201/interests").header("Authorization", token("product01")).contentType(MediaType.APPLICATION_JSON).content("{\"interestCodes\":[\"BEACH\"]}"), 409);
    }

    @Test void tc22InvalidTagAtomicity() throws Exception {
        var before = jdbc.queryForList("SELECT interest_id FROM tour_version_interest_tags WHERE tour_version_id=215 ORDER BY interest_id");
        assertEquals("INVALID_INTEREST_CODE", call(put("/api/v1/internal/tour-versions/215/interests").header("Authorization", token("product01")).contentType(MediaType.APPLICATION_JSON).content("{\"interestCodes\":[\"BEACH\",\"UNKNOWN\"]}"), 422).at("/error/code").asText());
        var after = jdbc.queryForList("SELECT interest_id FROM tour_version_interest_tags WHERE tour_version_id=215 ORDER BY interest_id");
        observe("Mappings before", before); observe("Mappings after", after); assertEquals(before, after);
    }

    @Test void tc23AnonymousPublicSearch() throws Exception {
        assertFalse(items(call(get("/api/v1/tours"), 200)).isEmpty());
        call(get("/api/v1/tour-recommendations"), 200); call(get("/api/v1/tours/101"), 200);
        call(get("/api/v1/internal/tour-versions/215/interests"), 401);
    }

    @Test void tc24WhitespaceKeyword() throws Exception {
        assertEquals(call(get("/api/v1/tours"), 200).at("/data"), call(get("/api/v1/tours").param("keyword", "    "), 200).at("/data"));
    }

    @Test void tc25UnicodeNfcCaseAndAccents() throws Exception {
        var r = call(get("/api/v1/tours").param("keyword", "ĐÀ NẴNG"), 200);
        assertTrue(ids(r).contains("101"));
        assertEquals(r.at("/data"), call(get("/api/v1/tours").param("keyword", Normalizer.normalize("Đà Nẵng", Normalizer.Form.NFD)), 200).at("/data"));
        assertTrue(items(call(get("/api/v1/tours").param("keyword", "Da Nang"), 200)).isEmpty());
    }

    @Test void tc26KeywordInjectionAndWildcards() throws Exception {
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM tours", Long.class);
        assertTrue(items(call(get("/api/v1/tours").param("keyword", "' OR 1=1; DROP TABLE tours;--"), 200)).isEmpty());
        assertTrue(items(call(get("/api/v1/tours").param("keyword", "%"), 200)).isEmpty());
        assertTrue(items(call(get("/api/v1/tours").param("keyword", "_"), 200)).isEmpty());
        assertEquals(count, jdbc.queryForObject("SELECT COUNT(*) FROM tours", Long.class));
    }

    @Test void tc27UnknownDestination() throws Exception {
        assertEquals("INVALID_DESTINATION", call(get("/api/v1/tours").param("destinationId", "NO_SUCH_DEST"), 422).at("/error/code").asText());
    }

    @Test void tc28MoneyString() throws Exception {
        sql("UPDATE departure_offer_versions SET adult_price=4290000.00 WHERE offer_id=601");
        JsonNode value = find(call(get("/api/v1/tours").param("destinationId", "DN"), 200), "101").path("priceFrom");
        assertTrue(value.isTextual()); assertEquals(0, new BigDecimal("4290000.00").compareTo(new BigDecimal(value.asText())));
        assertTrue(call(get("/api/v1/tours/101"), 200).at("/data/priceFrom").isTextual());
    }

    @Test void tc29BigintString() throws Exception {
        tour(9007199254740993L, 3, 4290000, 1);
        var r = call(get("/api/v1/tours").param("keyword", "QA Tour"), 200);
        assertEquals("9007199254740993", items(r).getFirst().path("tourId").textValue());
        assertEquals("9007199254740993", call(get("/api/v1/tours/9007199254740993"), 200).at("/data/tourId").textValue());
        sql("INSERT INTO tour_interests(interest_id,code,name) VALUES (9007199254740993,'BIG_ID','QA Interest')");
        var catalog = call(get("/api/v1/interests"), 200).path("data");
        for (JsonNode interest : catalog) if (interest.path("code").asText().equals("BIG_ID")) {
            assertEquals("9007199254740993", interest.path("interestId").textValue());
        }
    }

    @Test void tc30AsOfOffset() throws Exception {
        var before = OffsetDateTime.now(clock);
        var r = call(get("/api/v1/tour-recommendations"), 200);
        var asOf = OffsetDateTime.parse(r.at("/meta/asOf").asText());
        assertEquals(before, asOf);
    }

    @Test void tc39ListRecommendationDetailConsistency() throws Exception {
        var list = call(get("/api/v1/tours").param("destinationId", "DN"), 200);
        var recommendations = call(get("/api/v1/tour-recommendations").param("destinationId", "DN").param("interestCodes", "BEACH"), 200);
        assertEquals(list.at("/meta/asOf"), recommendations.at("/meta/asOf"));
        var a = find(list, "101");
        var b = find(recommendations, "101");
        var c = call(get("/api/v1/tours/101"), 200).path("data");
        for (String field : List.of("priceFrom", "durationDays", "tourVersionId")) {
            assertEquals(a.path(field), b.path(field)); assertEquals(a.path(field), c.path(field));
        }
        observe("Identical fixed Clock/asOf for list, recommendations and detail", list.at("/meta/asOf").asText());
        var dests = StreamSupport.stream(c.path("destinations").spliterator(), false).map(d -> d.path("destinationId").asText()).toList();
        assertEquals(StreamSupport.stream(a.path("destinationIds").spliterator(), false).map(JsonNode::asText).toList(), dests);
    }

    @Test void tc40ReadOnlyNoBookingMutation() throws Exception {
        List<Map<String, Object>> before = jdbc.queryForList("SELECT * FROM tour_departures ORDER BY departure_id");
        var tables = jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name");
        observe("Module tables (Booking not implemented in this module)", tables);
        call(get("/api/v1/tour-recommendations").param("interestCodes", "BEACH", "FOOD"), 200);
        List<Map<String, Object>> after = jdbc.queryForList("SELECT * FROM tour_departures ORDER BY departure_id");
        observe("Departure/counter snapshot before", before); observe("Departure/counter snapshot after", after);
        assertEquals(before, after);
    }

    @Test void tc41DeterministicScoreOrder() throws Exception {
        var initial = call(get("/api/v1/tour-recommendations").param("interestCodes", "BEACH", "FOOD", "CULTURE"), 200);
        for (int i = 0; i < 5; i++) assertEquals(initial.at("/data"), call(get("/api/v1/tour-recommendations").param("interestCodes", "BEACH", "FOOD", "CULTURE"), 200).at("/data"));
    }

    @Test void tc42UnknownInterestPolicy() throws Exception {
        for (String code : List.of("UNKNOWN", "FESTIVAL")) assertEquals("INVALID_INTEREST_CODE", call(get("/api/v1/tour-recommendations").param("interestCodes", code), 422).at("/error/code").asText());
    }

    @Test void tc43InactiveMappingPolicy() throws Exception {
        var before = jdbc.queryForList("SELECT * FROM tour_version_interest_tags WHERE interest_id=1 ORDER BY tour_version_id");
        call(patch("/api/v1/internal/interests/1").header("Authorization", token("product01")).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"), 200);
        assertFalse(call(get("/api/v1/interests"), 200).path("data").toString().contains("BEACH"));
        var r = call(get("/api/v1/tour-recommendations").param("destinationId", "DN").param("interestCodes", "CULTURE"), 200);
        assertFalse(find(r, "101").path("interestCodes").toString().contains("BEACH"));
        call(put("/api/v1/internal/tour-versions/215/interests").header("Authorization", token("product01")).contentType(MediaType.APPLICATION_JSON).content("{\"interestCodes\":[\"BEACH\"]}"), 422);
        var after = jdbc.queryForList("SELECT * FROM tour_version_interest_tags WHERE interest_id=1 ORDER BY tour_version_id");
        observe("Mapping before deactivate", before); observe("Mapping after rejected new assignment", after); assertEquals(before, after);
        call(patch("/api/v1/internal/interests/1").header("Authorization", token("product01")).contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"), 200);
        assertTrue(call(get("/api/v1/interests"), 200).path("data").toString().contains("BEACH"));
        var restored = find(call(get("/api/v1/tour-recommendations").param("destinationId", "DN").param("interestCodes", "BEACH"), 200), "101");
        assertEquals(100.0, restored.path("matchScore").asDouble());
        assertEquals(before, jdbc.queryForList("SELECT * FROM tour_version_interest_tags WHERE interest_id=1 ORDER BY tour_version_id"));
        observe("Reactivation restores original public mapping and score", restored);
    }

    @Test void tc44InputLimits() throws Exception {
        call(get("/api/v1/tours").param("keyword", "a".repeat(100)), 200);
        call(get("/api/v1/tours").param("keyword", "a".repeat(101)), 400);
        call(get("/api/v1/tours").param("interestCodes", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K"), 400);
        call(get("/api/v1/tours").param("interestCodes", java.util.Collections.nCopies(11, "BEACH").toArray(String[]::new)), 200);
        for (String value : List.of("0", "-1", "1.5", "abc")) call(get("/api/v1/tours").param("maxBudget", value), 400);
    }

    @Test void regressionLargePageDoesNotOverflow() throws Exception {
        assertTrue(items(call(get("/api/v1/tours").param("page", "2147483647").param("size", "10"), 200)).isEmpty());
    }

    @Test void regressionOfferBelongsToDeparture() throws Exception {
        sql("UPDATE tour_departures SET active_offer_id=711 WHERE departure_id=301");
        assertEquals("4600000", find(call(get("/api/v1/tours").param("destinationId", "DN"), 200), "101").path("priceFrom").asText());
        var detail = call(get("/api/v1/tours/101"), 200).path("data");
        for (JsonNode dep : detail.path("departures")) if (dep.path("departureId").asText().equals("301")) {
            assertTrue(dep.path("adultPrice").isNull()); assertFalse(dep.path("bookable").asBoolean());
        }
    }

    @Test void regressionCurrentVersionBelongsToTour() throws Exception {
        sql("UPDATE tours SET current_published_version_id=202 WHERE tour_id=101");
        call(get("/api/v1/tours/101"), 404);
    }

    @Test void regressionDestinationQueriesDoNotGrowPerCandidate() throws Exception {
        for (int i = 0; i < 15; i++) tour(6000 + i, 3, 2000000 + i, 1);
        var stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        call(get("/api/v1/tours").param("keyword", "QA Tour 6000"), 200);
        long oneCandidate = stats.getPrepareStatementCount();
        stats.clear();
        call(get("/api/v1/tours").param("keyword", "QA Tour"), 200);
        long fifteenCandidates = stats.getPrepareStatementCount();
        observe("Query count at 1/15 candidates", Map.of("one", oneCandidate, "fifteen", fifteenCandidates));
        assertTrue(fifteenCandidates <= oneCandidate + 1, "Destination lookup query count must not grow per tour");
    }

    @Test void regressionInternalCatalogRequiresProduct() throws Exception {
        call(get("/api/v1/internal/interests").header("Authorization", token("manager01")), 403);
        call(get("/api/v1/internal/interests").header("Authorization", token("product01")), 200);
        call(get("/api/v1/internal/tour-versions/201/interests").header("Authorization", token("manager01")), 200);
    }

    @Test void regressionMalformedJsonIsBadRequest() throws Exception {
        assertEquals("INVALID_FILTER", call(put("/api/v1/internal/tour-versions/215/interests")
                .header("Authorization", token("product01")).contentType(MediaType.APPLICATION_JSON)
                .content("{"), 400).at("/error/code").asText());
    }

    @Test void regressionTagReplacementLocksVersion() throws Exception {
        call(put("/api/v1/internal/tour-versions/215/interests")
                .header("Authorization", token("product01")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"interestCodes\":[\"BEACH\"]}"), 200);
        // A second physical connection must not acquire the version lock before the
        // replacement transaction commits; otherwise concurrent PUTs may merge sets.
        try (var other = jdbc.getDataSource().getConnection(); var statement = other.createStatement()) {
            other.setAutoCommit(false);
            statement.execute("SET LOCK_TIMEOUT 200");
            try {
                var timeout = assertThrows(java.sql.SQLException.class,
                        () -> statement.executeQuery("SELECT tour_version_id FROM tour_versions WHERE tour_version_id=215 FOR UPDATE"));
                observe("Second independent connection blocked by replacement row lock", timeout.getSQLState());
                assertEquals("HYT00", timeout.getSQLState());
            } finally {
                // Hikari evicts the secondary H2 connection after its lock timeout.
                if (!other.isClosed()) other.rollback();
            }
        }
    }
}
