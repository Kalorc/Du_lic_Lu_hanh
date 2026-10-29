package vn.ptit.tour.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 4.3. Danh muc diem den - tim kiem/loc bang ma chuan thay vi so khop chuoi ten. */
@Entity
@Table(name = "destinations")
@Getter @Setter @NoArgsConstructor
public class Destination {

    @Id
    @Column(name = "destination_id", length = 20)
    private String destinationId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(name = "destination_type", nullable = false, length = 16)
    private String destinationType;

    @Column(name = "parent_destination_id", length = 20)
    private String parentDestinationId;

    @Column(name = "province_name", length = 120)
    private String provinceName;

    @Column(length = 60)
    private String region;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
