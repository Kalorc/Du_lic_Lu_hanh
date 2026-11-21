package vn.ptit.tour.qa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.ptit.tour.service.TourDetailService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:demo_media;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class DemoMediaIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private TourDetailService details;

    @Test
    void everySeedVersionHasOneLocalCoverAndEachTourHasADistinctPhoto() {
        assertEquals(14, jdbc.queryForObject("SELECT COUNT(*) FROM tours", Integer.class));
        assertEquals(15, jdbc.queryForObject("SELECT COUNT(*) FROM tour_versions", Integer.class));
        assertEquals(0, jdbc.queryForObject("""
                SELECT COUNT(*) FROM tour_versions v WHERE
                  (SELECT COUNT(*) FROM tour_media m
                   WHERE m.tour_version_id=v.tour_version_id AND m.is_cover=TRUE) <> 1
                """, Integer.class));
        assertEquals(14, jdbc.queryForObject("""
                SELECT COUNT(DISTINCT m.media_url) FROM tour_media m WHERE m.is_cover=TRUE
                """, Integer.class));
        jdbc.query("SELECT media_url, alt_text FROM tour_media", rs -> {
            assertTrue(rs.getString(1).startsWith("/images/tours/tour-"), rs.getString(1));
            assertFalse(rs.getString(2).isBlank());
        });
    }

    @Test
    void publicDetailReturnsThePublishedVersionsLocalMedia() {
        var detail = details.getPublicDetail(101L);
        assertEquals("201", detail.tourVersionId());
        assertEquals("/images/tours/tour-101.webp", detail.coverImageUrl());
        assertFalse(detail.media().isEmpty());
        assertTrue(detail.media().stream().anyMatch(m -> m.cover() && m.mediaUrl().equals(detail.coverImageUrl())));
    }
}
