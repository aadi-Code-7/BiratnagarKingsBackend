package com.biratkings.controller;

import com.biratkings.model.Models.*;
import com.biratkings.service.DataService;
import com.biratkings.service.OverrideStore;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/** Update fixtures / stats without redeploying. Disabled unless ADMIN_TOKEN is set; send it as header X-Admin-Token. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final OverrideStore store;
    private final DataService data;
    private final byte[] token;

    public AdminController(OverrideStore store, DataService data, @Value("${app.admin.token:}") String token) {
        this.store = store; this.data = data; this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    private void auth(String header) {
        if (token.length == 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin API is disabled. Set the ADMIN_TOKEN environment variable.");
        if (header == null || !MessageDigest.isEqual(token, header.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin token.");
    }

    @GetMapping("/ping")
    public Map<String, Object> ping(@RequestHeader(value = "X-Admin-Token", required = false) String t) { auth(t); return Map.of("ok", true); }

    @GetMapping("/fixtures")
    public List<Fixture> fixtures(@RequestHeader(value = "X-Admin-Token", required = false) String t) { auth(t); return data.fixtures(); }

    @PutMapping("/fixtures")
    public List<Fixture> putFixtures(@RequestHeader(value = "X-Admin-Token", required = false) String t, @Valid @RequestBody FixturesPayload body) {
        auth(t); store.setFixtures(body.fixtures()); return data.fixtures();
    }

    @GetMapping("/stats")
    public KingsStats stats(@RequestHeader(value = "X-Admin-Token", required = false) String t) { auth(t); return data.stats(); }

    @PutMapping("/stats")
    public KingsStats putStats(@RequestHeader(value = "X-Admin-Token", required = false) String t, @Valid @RequestBody KingsStats body) {
        auth(t); store.setStats(body); return data.stats();
    }

    @GetMapping("/counters")
    public Counters counters(@RequestHeader(value = "X-Admin-Token", required = false) String t) {
        auth(t); var f = data.franchise(); return new Counters(f.matches(), f.runs(), f.wickets());
    }

    @PutMapping("/counters")
    public Counters putCounters(@RequestHeader(value = "X-Admin-Token", required = false) String t, @Valid @RequestBody Counters body) {
        auth(t); store.setCounters(body); return body;
    }

    @DeleteMapping("/overrides")
    public Map<String, Object> reset(@RequestHeader(value = "X-Admin-Token", required = false) String t) { auth(t); store.reset(); return Map.of("ok", true); }
}
