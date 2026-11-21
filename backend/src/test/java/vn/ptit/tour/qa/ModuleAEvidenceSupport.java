package vn.ptit.tour.qa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Records real requests and DB observations without storing authorization headers. */
public abstract class ModuleAEvidenceSupport {

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected JdbcTemplate jdbc;
    protected List<Map<String, Object>> observations;
    private String testName;

    @BeforeEach
    void prepareEvidence(TestInfo info) {
        testName = info.getTestMethod().orElseThrow().getName();
        observations = new ArrayList<>();
    }

    @RegisterExtension
    final TestWatcher evidenceWriter = new TestWatcher() {
        @Override public void testSuccessful(ExtensionContext context) { save("PASS", null); }
        @Override public void testFailed(ExtensionContext context, Throwable cause) {
            save("FAIL", cause.getClass().getSimpleName() + ": " + cause.getMessage());
        }
    };

    private void save(String status, String failure) {
        try {
            String caseId = testName.matches("tc\\d{2}.*")
                    ? testName.substring(0, 4).toUpperCase() : "REGRESSION";
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("testId", caseId);
            record.put("method", getClass().getSimpleName() + "." + testName);
            record.put("status", status);
            record.put("executedAt", OffsetDateTime.now().toString());
            record.put("runId", System.getProperty("qa.runId", "local"));
            record.put("sourceId", System.getProperty("qa.sourceId", "working-tree"));
            record.put("environment", jdbc.getDataSource().toString());
            record.put("observations", observations);
            record.put("failure", failure);
            Path dir = Path.of(System.getProperty("qa.evidenceDir", "target/module-a-evidence"), caseId);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(testName + ".json"),
                    json.writerWithDefaultPrettyPrinter().writeValueAsString(record));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot write Module A test evidence", ex);
        }
    }

    protected void observe(String description, Object value) {
        observations.add(Map.of("description", description, "actual", value));
    }

    protected JsonNode call(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(request).andReturn();
        var req = result.getRequest();
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("method", req.getMethod());
        event.put("path", req.getRequestURI());
        event.put("parameters", req.getParameterMap());
        event.put("body", req.getContentAsString());
        event.put("status", result.getResponse().getStatus());
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        event.put("response", body);
        observations.add(event);
        org.junit.jupiter.api.Assertions.assertEquals(expectedStatus, result.getResponse().getStatus(),
                req.getMethod() + " " + req.getRequestURI());
        return body;
    }

    protected void sql(String statement, Object... values) {
        observe("SQL fixture", Map.of("statement", statement, "parameters", values));
        jdbc.update(statement, values);
    }
}
