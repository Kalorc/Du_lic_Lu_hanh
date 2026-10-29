package vn.ptit.tour.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/** 4.7. Hinh anh Tour. Toi da mot anh is_cover=true moi phien ban. */
@Entity
@Table(name = "tour_media")
@Getter @Setter @NoArgsConstructor
public class TourMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id")
    private Long mediaId;

    @Column(name = "tour_version_id", nullable = false)
    private Long tourVersionId;

    @Column(name = "media_type", nullable = false, length = 12)
    private String mediaType = "IMAGE";

    @Column(name = "media_url", nullable = false, columnDefinition = "TEXT")
    private String mediaUrl;

    @Column(name = "alt_text", length = 200)
    private String altText;

    @Column(length = 200)
    private String caption;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder = 0;

    @Column(name = "is_cover", nullable = false)
    private boolean cover = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
