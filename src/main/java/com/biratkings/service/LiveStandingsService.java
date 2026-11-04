package com.biratkings.service;

import com.biratkings.model.Models.Standing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Keeps the Season 3 (NPL 2026) table and the Kings' match results fresh from Wikipedia's free MediaWiki API
 * (no API key). Falls back to the last known snapshot if a request fails or returns an older table.
 */
@Service
public class LiveStandingsService {
    private static final Logger log = LoggerFactory.getLogger(LiveStandingsService.class);

    /** @param kingsResults opponent full team name -> "Won by 5 wickets" / "Lost by 8 runs" … */
    public record Snapshot(List<Standing> table, Map<String, String> kingsResults, Instant at, String source) {}

    static final Map<String, String> TEAMS = new LinkedHashMap<>();   // abbreviation -> full name
    static final Map<String, String> CITIES = new LinkedHashMap<>();  // abbreviation -> city word used in result cells
    static {
        String[][] t = {{"BK", "Biratnagar Kings", "Biratnagar"}, {"CR", "Chitwan Rhinos", "Chitwan"}, {"JB", "Janakpur Bolts", "Janakpur"},
            {"KY", "Karnali Yaks", "Karnali"}, {"KG", "Kathmandu Gorkhas", "Kathmandu"}, {"LL", "Lumbini Lions", "Lumbini"},
            {"PA", "Pokhara Avengers", "Pokhara"}, {"SR", "Sudurpaschim Royals", "Sudurpaschim"}};
        for (String[] x : t) { TEAMS.put(x[0], x[1]); CITIES.put(x[0], x[2]); }
    }

    // Snapshot taken 2 Nov 2026 (Wikipedia / ESPNcricinfo). Replaced as soon as a fresher table is fetched.
    private static final List<Standing> SEED = List.of(
        new Standing(1, "Karnali Yaks", 2, 2, 0, 0, "+0.533", 4),
        new Standing(2, "Janakpur Bolts", 3, 2, 1, 0, "+0.141", 4),
        new Standing(3, "Lumbini Lions", 2, 1, 0, 1, "+4.599", 3),
        new Standing(4, "Chitwan Rhinos", 2, 1, 0, 1, "+0.400", 3),
        new Standing(5, "Biratnagar Kings", 3, 1, 2, 0, "+0.544", 2),
        new Standing(6, "Sudurpaschim Royals", 3, 1, 2, 0, "-2.412", 2),
        new Standing(7, "Kathmandu Gorkhas", 1, 0, 1, 0, "-0.942", 0),
        new Standing(8, "Pokhara Avengers", 2, 0, 2, 0, "-0.934", 0));

    private final AtomicReference<Snapshot> current = new AtomicReference<>(new Snapshot(SEED,
        Map.of("Chitwan Rhinos", "Lost by 8 runs"), Instant.parse("2026-11-02T00:00:00Z"), "Snapshot · Wikipedia / ESPNcricinfo"));
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String page;

    public LiveStandingsService(@Value("${app.live.page}") String page) { this.page = page; }

    public Snapshot snapshot() { return current.get(); }

    /** Matches the Kings have played this season, from the live table. */
    public int kingsPlayed() {
        return current.get().table().stream().filter(s -> s.team().contains("Biratnagar")).mapToInt(Standing::played).findFirst().orElse(0);
    }

    @Scheduled(initialDelay = 3000, fixedDelayString = "${app.live.refresh-ms}")
    public void refresh() {
        try {
            String url = "https://en.wikipedia.org/w/api.php?action=parse&format=json&formatversion=2&prop=text&redirects=1&page="
                + URLEncoder.encode(page, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
                .header("User-Agent", "BiratKingsSite/1.0 (admin: lucifer1000a@gmail.com)").GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) { log.warn("Wikipedia API returned {}", res.statusCode()); return; }
            String html = extractHtml(res.body());
            List<Standing> t = parseStandings(html);
            if (t.size() != 8) { log.warn("Unexpected standings size {}", t.size()); return; }
            // Only accept data at least as advanced as what we hold (guards against stale caches).
            if (played(t) >= played(current.get().table())) {
                Map<String, String> r = parseKingsResults(html);
                if (r.isEmpty()) r = current.get().kingsResults();
                current.set(new Snapshot(t, r, Instant.now(), "Live · Wikipedia (ESPNcricinfo)"));
                log.info("Live standings updated ({} team-matches played)", played(t));
            }
        } catch (Exception e) {
            log.warn("Live standings refresh failed: {}", e.getMessage());
        }
    }

    private static int played(List<Standing> t) { return t.stream().mapToInt(Standing::played).sum(); }

    /** The parse API returns {"parse":{"text":"<html…>"}}; unescape the JSON string without a JSON library. */
    static String extractHtml(String json) {
        int k = json.indexOf("\"text\":\"");
        if (k < 0) return "";
        int i = k + 8; StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i++);
            if (c == '"') break;
            if (c == '\\' && i < json.length()) {
                char n = json.charAt(i++);
                switch (n) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> { }
                    case 'u' -> { sb.append((char) Integer.parseInt(json.substring(i, i + 4), 16)); i += 4; }
                    default -> sb.append(n);
                }
            } else sb.append(c);
        }
        return sb.toString();
    }

    static List<Standing> parseStandings(String html) {
        for (List<List<String>> rows : WikiTables.tables(html)) {
            String head = rows.get(0).toString();
            if (!(head.contains("Pld") && head.contains("NRR") && head.contains("Pts"))) continue;
            List<Standing> out = new ArrayList<>();
            for (List<String> c : rows) {
                if (c.size() < 8) continue;
                try {
                    String team = c.get(1).replaceAll("\\s*\\([^)]*\\)\\s*$", "").trim();
                    String nrr = c.get(7).trim();
                    if (nrr.matches("^\\d.*")) nrr = "+" + nrr;
                    out.add(new Standing(Integer.parseInt(c.get(0)), team, Integer.parseInt(c.get(2)), Integer.parseInt(c.get(3)),
                        Integer.parseInt(c.get(4)), Integer.parseInt(c.get(5)), nrr.isEmpty() ? "—" : nrr, Integer.parseInt(c.get(6))));
                } catch (NumberFormatException ignored) { /* header or legend row */ }
            }
            if (!out.isEmpty()) return out;
        }
        return List.of();
    }

    /** Reads the home/visitor results matrix and returns the Kings' results keyed by opponent. */
    static Map<String, String> parseKingsResults(String html) {
        Map<String, String> out = new LinkedHashMap<>();
        for (List<List<String>> rows : WikiTables.tables(html)) {
            if (rows.isEmpty() || !rows.get(0).toString().contains("Visitor team")) continue;
            List<String> cols = new ArrayList<>();
            for (String h : rows.get(0)) if (TEAMS.containsKey(h.trim())) cols.add(h.trim());
            if (cols.size() != 8) continue;
            for (List<String> row : rows.subList(1, rows.size())) {
                if (row.size() < 9) continue;
                String home = abbrOf(row.get(0));
                if (home == null) continue;
                for (int i = 0; i < 8; i++) {
                    String visitor = cols.get(i), cell = row.get(i + 1);
                    if (!(home.equals("BK") || visitor.equals("BK")) || home.equals(visitor)) continue;
                    String res = kingsResult(cell, home.equals("BK") ? visitor : home);
                    if (res != null) out.put(TEAMS.get(home.equals("BK") ? visitor : home), res);
                }
            }
        }
        return out;
    }

    private static String abbrOf(String teamCell) {
        for (var e : TEAMS.entrySet()) if (teamCell.startsWith(e.getValue()) || teamCell.startsWith(CITIES.get(e.getKey()))) return e.getKey();
        return null;
    }

    /** "Chitwan 8 runs" -> "Lost by 8 runs"; "Biratnagar Super Over" -> "Won in Super Over"; unplayed -> null. */
    static String kingsResult(String cell, String opponentAbbr) {
        String c = cell.trim();
        if (c.isEmpty() || c.startsWith("Match") || c.matches("^[-–—]$")) return null;
        if (c.toLowerCase().contains("no result") || c.toLowerCase().contains("abandon")) return "No result";
        boolean kingsWon = c.startsWith(CITIES.get("BK"));
        boolean oppWon = c.startsWith(CITIES.get(opponentAbbr));
        if (!kingsWon && !oppWon) return null;
        String margin = c.substring((kingsWon ? CITIES.get("BK") : CITIES.get(opponentAbbr)).length()).trim();
        if (margin.isEmpty()) return kingsWon ? "Won" : "Lost";
        String how = margin.toLowerCase().startsWith("super over") ? "in " : "by ";
        return (kingsWon ? "Won " : "Lost ") + how + margin;
    }
}
