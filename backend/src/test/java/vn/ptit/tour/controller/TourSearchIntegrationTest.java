package vn.ptit.tour.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiem tra lai vi du nghiep vu trong dac ta (muc 6):
 * khach muon di Da Nang, 3 ngay, ngan sach toi da 5 trieu, thich BEACH + CULTURE.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TourSearchIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("API 1: danh muc so thich chi tra ve cac muc dang active")
    void interestsOnlyActive() throws Exception {
        mvc.perform(get("/api/v1/interests"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.success").value(true))
           .andExpect(jsonPath("$.data[*].code", hasItems("BEACH", "CULTURE", "FOOD")))
           .andExpect(jsonPath("$.data[*].code", not(hasItem("FESTIVAL"))));
    }

    @Test
    @DisplayName("API 2: danh muc diem den mo cho khach chua dang nhap")
    void destinationsPublic() throws Exception {
        mvc.perform(get("/api/v1/destinations"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data[*].destinationId", hasItems("DN", "HOIAN", "DLT")));
    }

    @Test
    @DisplayName("Hard filters: DN + 3 ngay + <= 5 trieu loai het C, D, E, F va tour ARCHIVED")
    void hardFiltersExample() throws Exception {
        mvc.perform(get("/api/v1/tours")
                        .param("destinationId", "DN")
                        .param("durationDays", "3")
                        .param("maxBudget", "5000000"))
           .andExpect(status().isOk())
           // Tour 102 (6 trieu), 103 (Da Lat), 104 (4 ngay), 105 (chua cong bo), 114 (ARCHIVED) bi loai
           .andExpect(jsonPath("$.data.items[*].tourId", not(hasItem("102"))))
           .andExpect(jsonPath("$.data.items[*].tourId", not(hasItem("103"))))
           .andExpect(jsonPath("$.data.items[*].tourId", not(hasItem("104"))))
           .andExpect(jsonPath("$.data.items[*].tourId", not(hasItem("105"))))
           .andExpect(jsonPath("$.data.items[*].tourId", not(hasItem("114"))))
           .andExpect(jsonPath("$.data.items[*].tourId", hasItems("101", "113")));
    }

    @Test
    @DisplayName("Chi dung ban gia DA DUYET: tour 101 co gia tu 4.200.000, khong phai 3.000.000")
    void onlyApprovedOfferPrice() throws Exception {
        mvc.perform(get("/api/v1/tours").param("destinationId", "DN").param("durationDays", "3"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.items[?(@.tourId == '101')].priceFrom")
                   .value(hasItem("4200000")));
    }

    @Test
    @DisplayName("Goi y: BEACH + CULTURE -> cac tour dat 100.0 len dau, cung diem thi gia thap truoc")
    void recommendationRanking() throws Exception {
        // Tour 112 (4.900.000) va tour 113 (5.000.000) deu trung 2/2 -> 100.0.
        // Theo quy tac sort "matchScore DESC, priceFrom ASC, tourId ASC",
        // tour 112 xep truoc tour 113, va tour 101 (chi trung BEACH -> 50.0) xep sau.
        mvc.perform(get("/api/v1/tour-recommendations")
                        .param("destinationId", "DN")
                        .param("durationDays", "3")
                        .param("maxBudget", "5000000")
                        .param("interestCodes", "BEACH")
                        .param("interestCodes", "CULTURE"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.items[0].tourId").value("112"))
           .andExpect(jsonPath("$.data.items[0].matchScore").value(100.0))
           .andExpect(jsonPath("$.data.items[0].matchedInterests", hasItems("BEACH", "CULTURE")))
           .andExpect(jsonPath("$.data.items[1].tourId").value("113"))
           .andExpect(jsonPath("$.data.items[1].matchScore").value(100.0))
           .andExpect(jsonPath("$.data.items[2].tourId").value("101"))
           .andExpect(jsonPath("$.data.items[2].matchScore").value(50.0))
           .andExpect(jsonPath("$.meta.sort").value("RELEVANCE"));
    }

    @Test
    @DisplayName("Tour khong trung so thich nao van duoc giu, nhung xep cuoi")
    void zeroMatchStillKept() throws Exception {
        mvc.perform(get("/api/v1/tour-recommendations")
                        .param("destinationId", "DN")
                        .param("durationDays", "3")
                        .param("interestCodes", "MOUNTAIN"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.items[*].matchScore", everyItem(is(0.0))))
           .andExpect(jsonPath("$.data.items", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("So thich khong active (FESTIVAL) khong duoc tinh diem")
    void inactiveInterestRejected() throws Exception {
        mvc.perform(get("/api/v1/tour-recommendations").param("interestCodes", "FESTIVAL"))
           .andExpect(status().isUnprocessableEntity())
           .andExpect(jsonPath("$.error.code").value("INVALID_INTEREST_CODE"));
    }

    @Test
    @DisplayName("Khong chon so thich -> matchScore null va sort theo gia tang")
    void noInterestSelected() throws Exception {
        mvc.perform(get("/api/v1/tours").param("size", "10"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.items[0].matchScore").value(nullValue()))
           .andExpect(jsonPath("$.meta.sort").value("PRICE_ASC"))
           .andExpect(jsonPath("$.meta.personalized").value(false));
    }

    @Test
    @DisplayName("availableOnly: chuyen het cho bi loai truoc khi tim gia thap nhat")
    void availableOnlyAffectsPriceFrom() throws Exception {
        // Tour 110 co chuyen 3.900.000 nhung HET CHO, va chuyen 4.400.000 con cho.
        mvc.perform(get("/api/v1/tours").param("destinationId", "NT").param("availableOnly", "true"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.items[?(@.tourId == '110')].priceFrom")
                   .value(hasItem("4400000")));

        mvc.perform(get("/api/v1/tours").param("destinationId", "NT").param("availableOnly", "false"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.items[?(@.tourId == '110')].priceFrom")
                   .value(hasItem("3900000")));
    }

    @Test
    @DisplayName("Loc diem den tim thay tour lien ket nhieu diem den")
    void multiDestinationFilter() throws Exception {
        mvc.perform(get("/api/v1/tours").param("destinationId", "HOIAN"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.items[*].tourId", hasItems("101", "106", "112")));
    }

    @Test
    @DisplayName("Validate: maxBudget <= 0 -> 400 INVALID_BUDGET")
    void invalidBudget() throws Exception {
        mvc.perform(get("/api/v1/tours").param("maxBudget", "0"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error.code").value("INVALID_BUDGET"));
    }

    @Test
    @DisplayName("Validate: size ngoai khoang 1-10 -> 400 INVALID_PAGINATION")
    void invalidPagination() throws Exception {
        mvc.perform(get("/api/v1/tours").param("size", "50"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error.code").value("INVALID_PAGINATION"));
    }

    @Test
    @DisplayName("API 5: tour ARCHIVED khong xem duoc cong khai -> 404 TOUR_NOT_FOUND")
    void archivedTourNotFound() throws Exception {
        mvc.perform(get("/api/v1/tours/114"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error.code").value("TOUR_NOT_FOUND"));
    }

    @Test
    @DisplayName("API 5: chi tiet tour cong khai tra ve lich trinh va chuyen khoi hanh")
    void publicDetail() throws Exception {
        mvc.perform(get("/api/v1/tours/101"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.tourCode").value("T101"))
           .andExpect(jsonPath("$.data.priceFrom").value("4200000"))
           .andExpect(jsonPath("$.data.interests[*].code", hasItems("BEACH", "FOOD", "RELAXATION")))
           .andExpect(jsonPath("$.data.departures", hasSize(greaterThan(1))));
    }

    @Test
    @DisplayName("API noi bo khong co token -> 401 UNAUTHENTICATED")
    void internalRequiresToken() throws Exception {
        mvc.perform(get("/api/v1/internal/tour-versions/201/interests"))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }
}
