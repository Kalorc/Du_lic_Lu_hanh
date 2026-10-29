package vn.ptit.tour.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Ham cham diem so thich (muc 5.2 dac ta).
 *
 * Cho S la tap ma so thich hop le, khong trung nhau, do khach chon;
 * T la tap tag thuoc published version va dang ACTIVE cua mot tour.
 *
 *   matched      = S giao T
 *   matchedCount = |matched|
 *   |S| > 0  ->  matchScore = round(100 * matchedCount / |S|, 2),  nam trong 0-100
 *   |S| = 0  ->  matchScore = null
 *
 * matchScore la TY LE TRUNG SO THICH. Day khong phai xac suat, cung khong phai
 * ty le hai long du bao.
 */
@Component
public class InterestScorer {

    public record Score(List<String> matched, int matchedCount, Double matchScore) {
    }

    public Score score(List<String> selected, Set<String> tourTags) {
        Set<String> s = new LinkedHashSet<>(selected == null ? List.of() : selected);

        if (s.isEmpty()) {
            // Khong co so thich nao duoc chon -> khong cham diem, khong tuyen bo ca nhan hoa.
            return new Score(List.of(), 0, null);
        }

        List<String> matched = new ArrayList<>();
        for (String code : s) {
            if (tourTags != null && tourTags.contains(code)) {
                matched.add(code);
            }
        }

        double raw = 100.0 * matched.size() / s.size();
        double rounded = BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP).doubleValue();
        return new Score(matched, matched.size(), rounded);
    }
}
