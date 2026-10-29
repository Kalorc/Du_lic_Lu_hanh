package vn.ptit.tour.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Kiem tra ham cham diem so thich theo muc 5.2 dac ta. */
class InterestScorerTest {

    private final InterestScorer scorer = new InterestScorer();

    private Set<String> tags(String... codes) {
        return new LinkedHashSet<>(List.of(codes));
    }

    @Test
    @DisplayName("Trung 2/2 so thich -> 100.0")
    void fullMatch() {
        var s = scorer.score(List.of("BEACH", "CULTURE"), tags("BEACH", "CULTURE"));
        assertEquals(2, s.matchedCount());
        assertEquals(100.0, s.matchScore().doubleValue());
    }

    @Test
    @DisplayName("Trung 1/2 so thich -> 50.0")
    void halfMatch() {
        var s = scorer.score(List.of("BEACH", "CULTURE"), tags("BEACH", "FOOD", "RELAXATION"));
        assertEquals(List.of("BEACH"), s.matched());
        assertEquals(50.0, s.matchScore().doubleValue());
    }

    @Test
    @DisplayName("Khong trung so thich nao -> 0.0 nhung tour van duoc giu")
    void noMatch() {
        var s = scorer.score(List.of("BEACH", "CULTURE"), tags("MOUNTAIN", "NATURE"));
        assertEquals(0, s.matchedCount());
        assertEquals(0.0, s.matchScore().doubleValue());
    }

    @Test
    @DisplayName("Khach khong chon so thich -> matchScore = null")
    void noSelection() {
        var s = scorer.score(List.of(), tags("BEACH", "CULTURE"));
        assertNull(s.matchScore());
        assertTrue(s.matched().isEmpty());
    }

    @Test
    @DisplayName("Lam tron 2 chu so thap phan: 1/3 -> 33.33")
    void rounding() {
        var s = scorer.score(List.of("BEACH", "CULTURE", "FOOD"), tags("BEACH"));
        assertEquals(33.33, s.matchScore().doubleValue());
    }

    @Test
    @DisplayName("Diem luon nam trong khoang 0-100")
    void bounded() {
        for (int n = 1; n <= 10; n++) {
            var selected = new java.util.ArrayList<String>();
            for (int i = 0; i < n; i++) selected.add("C" + i);
            var s = scorer.score(selected, new LinkedHashSet<>(selected));
            assertTrue(s.matchScore().doubleValue() >= 0.0 && s.matchScore().doubleValue() <= 100.0);
        }
    }
}
