package com.biratkings.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public class Models {
    /** Cricinfo-style profile. Every optional field may be null and the UI simply hides it. */
    public record Player(String id, Integer jersey, String firstName, String lastName, String fullName,
                         String role, String group, boolean captain, boolean overseas, String image,
                         String born, String birthPlace, String battingStyle, String bowlingStyle,
                         List<String> teams, Stats kingsStats, String bio, List<String> highlights) {
        public record Stats(int matches, int runs, int wickets, double strikeRate) {}
    }
    public record Fixture(int id, @NotBlank String date, @NotBlank String time, @NotBlank String opponent,
                          @NotBlank String venue, boolean home, String result) {}
    public record Standing(int position, String team, int played, int won, int lost, int noResult,
                           String nrr, int points) {}
    public record Season(int season, int year, String title, boolean live, String updatedAt,
                         String source, List<Standing> table) {}
    public record Franchise(String name, int established, String city, int matches, int runs, int wickets,
                            List<String> tickerWords) {}
    public record KingsStats(@Min(0) int totalRuns, @Min(0) int powerPlayRuns, @Min(0) int totalWickets, @Min(0) int maidens,
                             @DecimalMin("0") @DecimalMax("100") double pacePct, @DecimalMin("0") @DecimalMax("100") double spinPct,
                             @Min(0) int freeHits, @Min(0) int dotBalls, @Min(0) int fours, @Min(0) int sixes) {}
    public record Counters(@Min(0) int matches, @Min(0) int runs, @Min(0) int wickets) {}
    public record FixturesPayload(@NotEmpty List<@Valid Fixture> fixtures) {}
    public record GalleryItem(int no, String title, String image) {}
    public record Sponsor(String tier, String name, String logo) {}
    public record Product(String id, String name, String tag, String description, List<String> features,
                          List<String> sizes, List<String> images, String retailers, String partner,
                          boolean personalizable) {}

    public record OrderItem(@NotBlank String productId, @NotBlank String size,
                            @Min(1) @Max(10) int quantity,
                            @Pattern(regexp = "^[A-Za-z ]{0,12}$", message = "Jersey name: letters only, max 12") String customName,
                            @Min(0) @Max(99) Integer customNumber) {}

    public record OrderRequest(
        @NotBlank(message = "Name is required")
        @Pattern(regexp = "^[\\p{L}][\\p{L} .'-]{1,59}$", message = "Enter a valid name (letters only)") String name,
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\d{7,15}$", message = "Phone must contain digits only (7–15 digits)") String phone,
        @NotBlank(message = "Address is required")
        @Size(min = 5, max = 250, message = "Address must be 5–250 characters") String address,
        @NotEmpty(message = "Your cart is empty") List<@Valid OrderItem> items) {}

    public record OrderResponse(String orderId, String status, boolean adminNotified, String message) {}
}
