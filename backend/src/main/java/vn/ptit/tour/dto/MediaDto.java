package vn.ptit.tour.dto;

public record MediaDto(String mediaId, String mediaType, String mediaUrl,
                       String altText, String caption, Integer sortOrder, boolean cover) {
}
