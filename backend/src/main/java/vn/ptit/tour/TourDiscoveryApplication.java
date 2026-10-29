package vn.ptit.tour;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Module Tra cuu Tour va Goi y Tour theo So thich.
 * Kien truc: React -> Auth(JWT) -> Controller -> Service -> Repository -> DB.
 */
@SpringBootApplication
public class TourDiscoveryApplication {
    public static void main(String[] args) {
        SpringApplication.run(TourDiscoveryApplication.class, args);
    }
}
