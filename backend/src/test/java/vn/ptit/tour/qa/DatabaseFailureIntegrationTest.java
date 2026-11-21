package vn.ptit.tour.qa;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:modulea_dbfailure;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DatabaseFailureIntegrationTest extends ModuleAEvidenceSupport {

    @Autowired private DataSource dataSource;

    @Test void tc37DatabaseConnectionUnavailable() throws Exception {
        call(get("/api/v1/tours"), 200);
        HikariDataSource pool = dataSource.unwrap(HikariDataSource.class);
        pool.close();
        observe("Actual isolated connection pool closed before request; no mock DAO/service", pool.isClosed());
        var r = call(get("/api/v1/tours"), 500);
        assertEquals("INTERNAL_ERROR", r.at("/error/code").asText());
        assertFalse(r.at("/error/requestId").asText().isBlank());
        assertEquals("Loi khong mong doi", r.at("/error/message").asText());
        assertTrue(r.at("/error/details").isNull() || r.at("/error/details").isMissingNode());
        String body = r.toString().toLowerCase();
        for (String forbidden : new String[]{"jdbc:", "hikari", "select ", "exception", "stacktrace", "password"}) {
            assertFalse(body.contains(forbidden), forbidden + " leaked in response");
        }
    }
}
