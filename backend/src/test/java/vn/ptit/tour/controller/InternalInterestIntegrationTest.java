package vn.ptit.tour.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Kiem tra ma tran quyen va cac API noi bo 6-9. */
@SpringBootTest
@AutoConfigureMockMvc
class InternalInterestIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;

    private String login(String username) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = om.readTree(body);
        return node.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("PRODUCT xem duoc so thich cua phien ban Tour")
    void productCanRead() throws Exception {
        mvc.perform(get("/api/v1/internal/tour-versions/201/interests")
                        .header("Authorization", "Bearer " + login("product01")))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.interests[*].code", hasItems("BEACH", "FOOD", "RELAXATION")));
    }

    @Test
    @DisplayName("MANAGER xem duoc nhung KHONG duoc gan so thich -> 403 FORBIDDEN")
    void managerReadOnly() throws Exception {
        String token = login("manager01");

        mvc.perform(get("/api/v1/internal/tour-versions/201/interests")
                        .header("Authorization", "Bearer " + token))
           .andExpect(status().isOk());

        mvc.perform(put("/api/v1/internal/tour-versions/215/interests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interestCodes\":[\"BEACH\"]}"))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("CUSTOMER khong duoc dung API noi bo -> 403 FORBIDDEN")
    void customerForbidden() throws Exception {
        mvc.perform(get("/api/v1/internal/tour-versions/201/interests")
                        .header("Authorization", "Bearer " + login("khach01")))
           .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PRODUCT gan lai so thich cho phien ban DRAFT (215) thanh cong")
    void productReplacesTagsOnDraft() throws Exception {
        mvc.perform(put("/api/v1/internal/tour-versions/215/interests")
                        .header("Authorization", "Bearer " + login("product01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interestCodes\":[\"BEACH\",\"CULTURE\",\"HISTORY\"]}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.interests", hasSize(3)))
           .andExpect(jsonPath("$.data.interests[*].code", hasItems("BEACH", "CULTURE", "HISTORY")));
    }

    @Test
    @DisplayName("Sua phien ban da PUBLISHED -> 409 VERSION_LOCKED")
    void publishedVersionLocked() throws Exception {
        mvc.perform(put("/api/v1/internal/tour-versions/201/interests")
                        .header("Authorization", "Bearer " + login("product01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interestCodes\":[\"BEACH\"]}"))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.error.code").value("VERSION_LOCKED"));
    }

    @Test
    @DisplayName("Body co ma trung -> 422 DUPLICATE_INTEREST_CODE")
    void duplicateInBody() throws Exception {
        mvc.perform(put("/api/v1/internal/tour-versions/215/interests")
                        .header("Authorization", "Bearer " + login("product01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interestCodes\":[\"BEACH\",\"beach\"]}"))
           .andExpect(status().isUnprocessableEntity())
           .andExpect(jsonPath("$.error.code").value("DUPLICATE_INTEREST_CODE"));
    }

    @Test
    @DisplayName("Ma so thich khong ton tai -> 422 INVALID_INTEREST_CODE")
    void unknownCode() throws Exception {
        mvc.perform(put("/api/v1/internal/tour-versions/215/interests")
                        .header("Authorization", "Bearer " + login("product01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interestCodes\":[\"KHONG_CO_THAT\"]}"))
           .andExpect(status().isUnprocessableEntity())
           .andExpect(jsonPath("$.error.code").value("INVALID_INTEREST_CODE"));
    }

    @Test
    @DisplayName("Phien ban khong ton tai -> 404 VERSION_NOT_FOUND")
    void versionNotFound() throws Exception {
        mvc.perform(get("/api/v1/internal/tour-versions/999999/interests")
                        .header("Authorization", "Bearer " + login("product01")))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error.code").value("VERSION_NOT_FOUND"));
    }

    @Test
    @DisplayName("PRODUCT tao va cap nhat danh muc so thich (API 8, 9)")
    void createAndUpdateInterest() throws Exception {
        String token = login("product01");

        String created = mvc.perform(post("/api/v1/internal/interests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ECO_TOURISM\",\"name\":\"Du lịch sinh thái\","
                               + "\"description\":\"Trải nghiệm sinh thái bền vững\",\"displayOrder\":12}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("ECO_TOURISM"))
                .andReturn().getResponse().getContentAsString();

        long id = om.readTree(created).path("data").path("interestId").asLong();

        mvc.perform(patch("/api/v1/internal/interests/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Du lịch sinh thái (mới)\",\"active\":false}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.name").value("Du lịch sinh thái (mới)"))
           .andExpect(jsonPath("$.data.active").value(false));

        // Da tat -> khong con xuat hien trong danh muc cong khai
        mvc.perform(get("/api/v1/interests"))
           .andExpect(jsonPath("$.data[*].code", not(hasItem("ECO_TOURISM"))));
    }

    @Test
    @DisplayName("Tao trung ma -> 422 DUPLICATE_INTEREST_CODE")
    void duplicateCreate() throws Exception {
        mvc.perform(post("/api/v1/internal/interests")
                        .header("Authorization", "Bearer " + login("product01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"BEACH\",\"name\":\"Biển trùng\"}"))
           .andExpect(status().isUnprocessableEntity())
           .andExpect(jsonPath("$.error.code").value("DUPLICATE_INTEREST_CODE"));
    }

    @Test
    @DisplayName("Cap nhat so thich khong ton tai -> 404 INTEREST_NOT_FOUND")
    void updateMissingInterest() throws Exception {
        mvc.perform(patch("/api/v1/internal/interests/999999")
                        .header("Authorization", "Bearer " + login("product01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error.code").value("INTEREST_NOT_FOUND"));
    }

    @Test
    @DisplayName("Token sai -> 401 UNAUTHENTICATED")
    void badToken() throws Exception {
        mvc.perform(get("/api/v1/internal/tour-versions/201/interests")
                        .header("Authorization", "Bearer khong-phai-token"))
           .andExpect(status().isUnauthorized());
    }
}
