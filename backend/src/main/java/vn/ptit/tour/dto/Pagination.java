package vn.ptit.tour.dto;

public record Pagination(int page, int size, long totalItems, int totalPages) {

    public static Pagination of(int page, int size, long totalItems) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalItems / size);
        return new Pagination(page, size, totalItems, totalPages);
    }
}
