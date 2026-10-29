package vn.ptit.tour.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.ptit.tour.dto.CandidateRow;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Buoc 1-2 cua thuat toan: chon ung vien va ap HARD FILTERS, sau do lay
 * "cheapest eligible departure" cua tung Tour.
 *
 * Dieu kien bat buoc cua he thong (khong phu thuoc input cua khach):
 *   - Tour.lifecycle_status = ACTIVE            (ARCHIVED khong xuat hien cong khai)
 *   - Phien ban dung la tours.current_published_version_id va co content_status = PUBLISHED
 *   - Chuyen khoi hanh sales_status = OPEN
 *   - asOf nam trong booking window [booking_opens_at, booking_closes_at]
 *   - Ban gia dang active cua chuyen co approval_status = APPROVED
 *   - Neu availableOnly = true thi available_seats > 0
 *
 * Hard filters theo input: keyword, destinationId, durationDays, maxBudget.
 *
 * Luu y thu tu: maxBudget duoc ap TRUOC khi chon chuyen re nhat, va cac chuyen
 * het cho bi loai TRUOC khi tim gia thap nhat - dung dac ta muc 6.3
 * ("khong duoc quang cao gia 4,2 trieu neu chuyen do da het cho").
 */
@Repository
public class TourSearchDao {

    private final NamedParameterJdbcTemplate jdbc;

    public TourSearchDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CandidateRow> findCandidates(String keyword,
                                             String destinationId,
                                             Integer durationDays,
                                             Long maxBudget,
                                             boolean availableOnly,
                                             OffsetDateTime asOf) {

        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("asOf", asOf);

        List<String> where = new ArrayList<>();
        where.add("t.lifecycle_status = 'ACTIVE'");
        where.add("v.content_status = 'PUBLISHED'");
        where.add("d.tour_version_id = t.current_published_version_id");
        where.add("d.sales_status = 'OPEN'");
        where.add("d.booking_opens_at <= :asOf");
        where.add("d.booking_closes_at >= :asOf");
        where.add("o.approval_status = 'APPROVED'");

        if (availableOnly) {
            where.add("d.available_seats > 0");
        }
        if (durationDays != null) {
            where.add("v.duration_days = :durationDays");
            p.addValue("durationDays", durationDays);
        }
        if (maxBudget != null) {
            where.add("o.adult_price <= :maxBudget");
            p.addValue("maxBudget", maxBudget);
        }
        if (destinationId != null && !destinationId.isBlank()) {
            where.add("""
                      EXISTS (SELECT 1 FROM tour_version_destinations td
                               WHERE td.tour_version_id = v.tour_version_id
                                 AND td.destination_id = :destinationId)
                      """);
            p.addValue("destinationId", destinationId);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.add("""
                      (LOWER(v.title) LIKE :kw
                       OR LOWER(COALESCE(v.summary, '')) LIKE :kw
                       OR LOWER(COALESCE(v.description, '')) LIKE :kw
                       OR LOWER(t.tour_code) LIKE :kw
                       OR EXISTS (SELECT 1 FROM tour_version_destinations td2
                                    JOIN destinations dd ON dd.destination_id = td2.destination_id
                                   WHERE td2.tour_version_id = v.tour_version_id
                                     AND LOWER(dd.name) LIKE :kw))
                      """);
            p.addValue("kw", "%" + keyword.toLowerCase() + "%");
        }

        String sql = """
                SELECT c.tour_id, c.tour_version_id, c.tour_code, c.slug, c.title, c.summary,
                       c.duration_days, c.nights, c.departure_id, c.starts_at,
                       c.available_seats, c.adult_price, c.currency
                FROM (
                    SELECT t.tour_id, v.tour_version_id, t.tour_code, t.slug, v.title, v.summary,
                           v.duration_days, v.nights, d.departure_id, d.starts_at,
                           d.available_seats, o.adult_price, o.currency,
                           ROW_NUMBER() OVER (
                               PARTITION BY t.tour_id
                               ORDER BY o.adult_price ASC, d.starts_at ASC, d.departure_id ASC
                           ) AS rn
                    FROM tour_departures d
                    JOIN tours t  ON t.tour_id = d.tour_id
                    JOIN tour_versions v ON v.tour_version_id = t.current_published_version_id
                    JOIN departure_offer_versions o ON o.offer_id = d.active_offer_id
                    WHERE %s
                ) c
                WHERE c.rn = 1
                """.formatted(String.join("\n  AND ", where));

        return jdbc.query(sql, p, (rs, rowNum) -> new CandidateRow(
                rs.getLong("tour_id"),
                rs.getLong("tour_version_id"),
                rs.getString("tour_code"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getInt("duration_days"),
                rs.getInt("nights"),
                rs.getLong("departure_id"),
                rs.getObject("starts_at", OffsetDateTime.class),
                rs.getInt("available_seats"),
                rs.getBigDecimal("adult_price"),
                rs.getString("currency")
        ));
    }

    /** Gia "tu" thap nhat du dieu kien cua mot tour cu the (dung cho API chi tiet). */
    public List<CandidateRow> findCandidateForTour(Long tourId, boolean availableOnly, OffsetDateTime asOf) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("asOf", asOf);
        p.addValue("tourId", tourId);

        String availClause = availableOnly ? " AND d.available_seats > 0" : "";

        String sql = """
                SELECT c.tour_id, c.tour_version_id, c.tour_code, c.slug, c.title, c.summary,
                       c.duration_days, c.nights, c.departure_id, c.starts_at,
                       c.available_seats, c.adult_price, c.currency
                FROM (
                    SELECT t.tour_id, v.tour_version_id, t.tour_code, t.slug, v.title, v.summary,
                           v.duration_days, v.nights, d.departure_id, d.starts_at,
                           d.available_seats, o.adult_price, o.currency,
                           ROW_NUMBER() OVER (
                               PARTITION BY t.tour_id
                               ORDER BY o.adult_price ASC, d.starts_at ASC, d.departure_id ASC
                           ) AS rn
                    FROM tour_departures d
                    JOIN tours t ON t.tour_id = d.tour_id
                    JOIN tour_versions v ON v.tour_version_id = t.current_published_version_id
                    JOIN departure_offer_versions o ON o.offer_id = d.active_offer_id
                    WHERE t.tour_id = :tourId
                      AND t.lifecycle_status = 'ACTIVE'
                      AND v.content_status = 'PUBLISHED'
                      AND d.tour_version_id = t.current_published_version_id
                      AND d.sales_status = 'OPEN'
                      AND d.booking_opens_at <= :asOf
                      AND d.booking_closes_at >= :asOf
                      AND o.approval_status = 'APPROVED'%s
                ) c
                WHERE c.rn = 1
                """.formatted(availClause);

        return jdbc.query(sql, p, (rs, rowNum) -> new CandidateRow(
                rs.getLong("tour_id"),
                rs.getLong("tour_version_id"),
                rs.getString("tour_code"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getInt("duration_days"),
                rs.getInt("nights"),
                rs.getLong("departure_id"),
                rs.getObject("starts_at", OffsetDateTime.class),
                rs.getInt("available_seats"),
                rs.getBigDecimal("adult_price"),
                rs.getString("currency")
        ));
    }
}
