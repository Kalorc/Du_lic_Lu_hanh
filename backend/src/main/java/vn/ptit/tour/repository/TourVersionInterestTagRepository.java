package vn.ptit.tour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import vn.ptit.tour.entity.TourVersionInterestTag;
import vn.ptit.tour.entity.TourVersionInterestTagId;

import java.util.Collection;
import java.util.List;

public interface TourVersionInterestTagRepository
        extends JpaRepository<TourVersionInterestTag, TourVersionInterestTagId> {

    /**
     * Cac tag CUA phien ban, chi tinh so thich dang active (theo dac ta: "chi tinh active").
     * Tra ve (tourVersionId, code, name) de Service gom nhom.
     */
    @Query("""
           SELECT t.id.tourVersionId, i.code, i.name
           FROM TourVersionInterestTag t
             JOIN TourInterest i ON i.interestId = t.id.interestId
           WHERE t.id.tourVersionId IN :versionIds AND i.active = true
           ORDER BY i.displayOrder, i.code
           """)
    List<Object[]> findActiveTagsByVersionIds(Collection<Long> versionIds);

    /** Toan bo tag cua mot phien ban, ke ca so thich da tat - dung cho API noi bo. */
    @Query("""
           SELECT t.id.interestId, i.code, i.name, i.active, t.assignedByUserId, t.createdAt
           FROM TourVersionInterestTag t
             JOIN TourInterest i ON i.interestId = t.id.interestId
           WHERE t.id.tourVersionId = :versionId
           ORDER BY i.displayOrder, i.code
           """)
    List<Object[]> findAllTagsByVersionId(Long versionId);

    @Modifying
    @Query("DELETE FROM TourVersionInterestTag t WHERE t.id.tourVersionId = :versionId")
    int deleteByVersionId(Long versionId);
}
