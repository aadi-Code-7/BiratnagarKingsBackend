package com.biratkings.controller;

import com.biratkings.model.Models.*;
import com.biratkings.service.DataService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final DataService data;
    public ApiController(DataService data) { this.data = data; }

    private <T> ResponseEntity<T> cached(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()).body(body);
    }
    /** Live / admin-editable data: short cache so updates show up quickly. */
    private <T> ResponseEntity<T> fresh(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(20)).cachePublic()).body(body);
    }

    @GetMapping("/franchise")  public ResponseEntity<Franchise> franchise() { return fresh(data.franchise()); }
    @GetMapping("/players")    public ResponseEntity<List<Player>> players() { return cached(data.players()); }
    @GetMapping("/players/{id}") public ResponseEntity<Player> player(@PathVariable String id) {
        return data.players().stream().filter(p -> p.id().equals(id)).findFirst()
            .map(this::cached).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/fixtures")   public ResponseEntity<List<Fixture>> fixtures() { return fresh(data.fixtures()); }
    @GetMapping("/stats")      public ResponseEntity<KingsStats> stats() { return fresh(data.stats()); }
    @GetMapping("/gallery")    public ResponseEntity<List<GalleryItem>> gallery() { return cached(data.gallery()); }
    @GetMapping("/sponsors")   public ResponseEntity<List<Sponsor>> sponsors() { return cached(data.sponsors()); }
    @GetMapping("/store/products") public ResponseEntity<List<Product>> products() { return cached(data.products()); }
    @GetMapping("/store/products/{id}") public ResponseEntity<Product> product(@PathVariable String id) {
        return data.product(id).map(this::cached).orElse(ResponseEntity.notFound().build());
    }

    /** All three seasons; Season 3 reflects the latest live snapshot. Never cached by the browser. */
    @GetMapping("/standings")
    public ResponseEntity<List<Season>> standings() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(data.seasons());
    }

    @PostMapping("/store/orders")
    public ResponseEntity<?> order(@Valid @RequestBody OrderRequest r) {
        for (OrderItem i : r.items()) {
            Product p = data.product(i.productId()).orElse(null);
            if (p == null || !p.sizes().contains(i.size()))
                return ResponseEntity.badRequest().body(Map.of("message", "Unknown product or size."));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(data.placeOrder(r));
    }
}
