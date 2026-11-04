package com.biratkings.service;

import com.biratkings.model.Models.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** In-memory seed data mirroring biratkings.com. Swap for JPA repositories when you add a database. */
@Service
public class DataService {
    private static final String SITE = "https://biratkings.com";
    private static final String A = SITE + "/assets/";
    private static final String L = SITE + "/__l5e/assets-v1/";
    private final AtomicInteger orderSeq = new AtomicInteger(1000);
    private final Map<String, OrderResponse> orders = new ConcurrentHashMap<>();
    private final LiveStandingsService live;
    private final MailService mail;
    private final OverrideStore store;

    /** Matches the Kings had played before the 2026 season (7 + 7 league games + 2 playoff games). */
    private static final int MATCHES_BEFORE_2026 = 16;

    public DataService(LiveStandingsService live, MailService mail, OverrideStore store) {
        this.live = live; this.mail = mail; this.store = store;
    }

    /** Counters: admin override wins; otherwise the matches counter grows automatically with the live table. */
    public Franchise franchise() {
        var c = store.get().counters();
        int matches = c != null ? c.matches() : MATCHES_BEFORE_2026 + live.kingsPlayed();
        return new Franchise("Biratnagar Kings", 2024, "Biratnagar · Nepal", matches, c != null ? c.runs() : 2225, c != null ? c.wickets() : 96,
                List.of("Biratnagar Kings", "कीर्ति", "Roar", "Reign", "Repeat", "#letsgokings"));
    }

    // ---------- Players (NPL 2026 squad, 19 players) ----------
    private static Player p(String id, Integer no, String first, String last, String full, String role, String group,
                            boolean cap, boolean overseas, String img, String born, String place, String bat, String bowl,
                            List<String> teams, Player.Stats st, String bio, List<String> hl) {
        return new Player(id, no, first, last, full, role, group, cap, overseas, img, born, place, bat, bowl, teams, st, bio, hl);
    }

    @Cacheable("players")
    public List<Player> players() {
        final String K = "Biratnagar Kings";
        // Groups follow ESPNcricinfo roles: Batter · Wicketkeeper · All-rounder · Bowler.
        return List.of(
            p("sandeep-lamichhane", 25, "Sandeep", "Lamichhane", "Sandeep Lamichhane", "Leg-spinner", "Bowler", true, false,
              A + "sandeep-lamichhane-nobg-DL9DhKuu.png", "2000-08-02", null, "Right-hand bat", "Right-arm leg-break",
              List.of("Nepal", K, "Delhi Capitals", "Melbourne Renegades"), new Player.Stats(16, 53, 23, 132.50),
              "Nepal's spin wizard and one of the most feared leg-spinners on the global T20 circuit. Across two seasons Sandeep has taken 23 wickets for the Kings at an economy under 6, headlining the middle-overs squeeze.",
              List.of("Captain of the Biratnagar Kings and Nepal's ODI captain", "First Nepali cricketer to play in the IPL (Delhi Capitals, 2018)", "Joint-leading wicket-taker of NPL 2025 with 17 wickets", "Retained by the Kings for NPL 2026")),
            p("lokesh-bam", 7, "Lokesh", "Bam", "Lokesh Bam", "Wicketkeeper batter", "Wicketkeeper", false, false,
              A + "lokesh-bam-nobg-CGCWVUyI.png", null, null, "Right-hand bat", null,
              List.of("Nepal", K), null,
              "Middle-order batter and wicketkeeper who has been part of the Kings since the inaugural Nepal Premier League in 2024.",
              List.of("Biratnagar Kings squad member since NPL 2024", "Retained by the Kings for NPL 2026")),
            p("basir-ahmad", 11, "Basir", "Ahmad", "Basir Ahamad", "Batting all-rounder", "All-rounder", false, false,
              A + "basir-ahmad-nobg-DyRY3aw0.png", null, null, "Left-hand bat", "Slow left-arm orthodox",
              List.of("Nepal", K), null,
              "Left-handed all-rounder who bowls slow left-arm orthodox, adding balance and control through the middle overs.",
              List.of("Biratnagar Kings squad member since NPL 2024", "Retained by the Kings for NPL 2026")),
            p("pratish-gc", 44, "Pratish", "GC", "Pratish GC", "Medium-pace bowler", "Bowler", false, false,
              A + "pratish-gc-nobg-DzvlCQOh.png", null, null, "Right-hand bat", "Left-arm medium",
              List.of("Nepal", K), null,
              "Left-arm seamer who gives the Kings the domestic pace option, and a useful lower-order hitter.",
              List.of("Took 3/27 in the NPL 2025 Qualifier 2 against Lumbini Lions", "Retained by the Kings for NPL 2026")),
            p("subash-bhandari", 33, "Subash", "Bhandari", "Subash Bhandari", "Off-spinner", "Bowler", false, false,
              A + "subash-bhandari-nobg-C51pnMvS.png", "2004-12-05", "Kathekhola, Baglung, Nepal", "Right-hand bat", "Right-arm off-break",
              List.of("Nepal", K, "Gandaki Province", "Nepal Under-19s"), null,
              "Young off-spinner from Baglung who came through Gandaki Province and Nepal's Under-19 side, and has been a regular in the Kings attack.",
              List.of("Took 3/14 in the NPL 2025 Qualifier 2 against Lumbini Lions", "Played for Nepal at the 2024 Under-19 World Cup", "Retained by the Kings for NPL 2026")),
            p("narayan-joshi", 17, "Narayan", "Joshi", "Narayan Joshi", "All-rounder", "All-rounder", false, false,
              A + "narayan-joshi-nobg-DaRa962R.png", null, null, "Right-hand bat", "Right-arm medium",
              List.of("Nepal", K + " (2025–)", "Pokhara Avengers (NPL 2024)", "Biratnagar Super Kings (2022–23)"), null,
              "Right-handed all-rounder who bowls medium pace. Moved to the Kings ahead of NPL 2025 after a season with Pokhara Avengers.",
              List.of("Selected for Nepal's squad in November 2024", "Retained by the Kings for NPL 2026")),
            p("dilsad-ali", null, "Dilsad", "Ali", "Dilsad Ali", "Wicketkeeper batter", "Wicketkeeper", false, false, null,
              null, null, "Right-hand bat", null, List.of(K), null,
              "Right-handed batter in the Kings' 2026 squad.", List.of()),
            p("sujan-thapaliya", null, "Sujan", "Thapaliya", "Sujan Thapaliya", "Batting All-rounder", "All-rounder", false, false, null,
              null, null, "Right-hand bat", "Right-arm medium pace", List.of(K), null,
              "Right-handed batter in the Kings' 2026 squad.", List.of()),
            p("pratik-shrestha", null, "Pratik", "Shrestha", "Pratik Shrestha", "Batter", "Batter", false, false, null,
              null, null, "Right-hand bat", "Right-arm off spin", List.of(K, "Kathmandu Gorkhas (NPL 2024)"), null,
              "Right-handed batter who can bowl a few overs of medium pace. Played for Kathmandu Gorkhas in NPL 2024.", List.of()),
            p("naren-bhatta", null, "Naren", "Bhatta", "Naren Bhatta", "Batter", "Batter", false, false, null,
              null, null, "Left-hand bat", null, List.of(K), null,
              "Left-handed batter who was part of the Kings' inaugural NPL squad.",
              List.of("Biratnagar Kings squad member since NPL 2024")),
            p("aprajit-poudel", null, "Aprajit", "Poudel", "Aprajit Poudel", "Bowler", "Bowler", false, false, null,
              null, null, "Right-hand bat", "Slow left-arm orthodox", List.of(K), null,
              "Right-handed batter in the Kings' 2026 squad.", List.of()),
            p("charith-asalanka", null, "Charith", "Asalanka", "Charith Asalanka", "Batting all-rounder", "All-rounder", false, true, null,
              "1997-06-29", "Galle, Sri Lanka", "Left-hand bat", "Right-arm off-break", List.of("Sri Lanka", K), null,
              "Sri Lankan left-hand batter and off-spinner, one of the Kings' overseas signings for NPL 2026. Has captained Sri Lanka in white-ball cricket.",
              List.of("Sri Lanka international", "Overseas player")),
            p("angelo-mathews", null, "Angelo", "Mathews", "Angelo Mathews", "Batting all-rounder", "All-rounder", false, true, null,
              "1987-06-02", "Colombo, Sri Lanka", "Right-hand bat", "Right-arm medium", List.of("Sri Lanka", K), null,
              "Experienced Sri Lankan all-rounder and former national captain, bringing big-match nerve to the Kings' middle order.",
              List.of("Former Sri Lanka captain", "Overseas player")),
            p("andre-russell", null, "Andre", "Russell", "Andre Dwayne Russell", "Pace-bowling all-rounder", "All-rounder", false, true, null,
              "1988-04-29", "Jamaica", "Right-hand bat", "Right-arm fast", List.of("West Indies", K, "Kolkata Knight Riders", "Jamaica Tallawahs"), null,
              "One of T20 cricket's great power-hitters and a genuine fast-bowling all-rounder, signed as a marquee overseas name for the Kings.",
              List.of("Part of West Indies' T20 World Cup-winning squads (2012, 2016)", "Overseas player")),
            p("shubham-ranjane", null, "Shubham", "Ranjane", "Shubham Ranjane", "All-rounder", "All-rounder", false, false, null,
              null, null, "Right-hand bat", "Right-arm medium", List.of(K), null,
              "Right-handed batting all-rounder who bowls medium pace.", List.of()),
            p("imran-sheikh", null, "Imran", "Sheikh", "Imran Sheikh", "Batter", "Batter", false, false, null,
              null, null, "Right-hand bat", "Right-arm off-break", List.of(K), null,
              "Right-handed batter who bowls off-spin.", List.of()),
            p("ayush-neupane", null, "Ayush", "Neupane", "Ayush Neupane", "All-rounder", "All-rounder", false, false, null,
              null, null, "Right-hand bat", "Right-arm off-break", List.of(K), null,
              "Young right-handed batter who bowls off-spin; part of the Kings squad since NPL 2025.", List.of()),
            p("rupesh-singh", null, "Rupesh", "Singh", "Rupesh Kumar Singh", "Medium-pace bowler", "Bowler", false, false, null,
              null, null, "Right-hand bat", "Right-arm medium", List.of(K, "Janakpur Bolts (NPL 2024)"), null,
              "Right-arm medium-pace bowler who played for Janakpur Bolts in NPL 2024 before joining the Kings.", List.of()),
            p("shehan-jayasuriya", null, "Shehan", "Jayasuriya", "Shehan Jayasuriya", "All-rounder", "Batter", false, false, null,
              null, null, "Left-hand bat", "Right-arm off-break", List.of(K, "Sri Lanka", "United States", "Jaffna Stallions", "Silicon Valley Strikers"), null,
              "Experienced international all-rounder who played for Sri Lanka and the USA, signed by Biratnagar Kings as an overseas player.",
               List.of("Scored over 6,000 First-Class runs", "Took over 50 T20 wickets with a strong economy rate")),
        p("naren-saud", null, "Naren", "Saud", "Naren Saud", "Left-arm spinner", "Bowler", false, false, null,
              null, null, "Right-hand bat", "Slow left-arm orthodox", List.of(K, "Sudurpaschim Royals (NPL 2024)"), null,
              "Slow left-arm orthodox spinner who played for Sudurpaschim Royals in NPL 2024.",
              List.of("Took 5/19 for Sudurpaschim Royals in NPL 2024")));
    }

    private static final List<Fixture> SEED_FIXTURES = List.of(
        new Fixture(1, "Oct 27", "12:15 PM IST", "Chitwan Rhinos", "TU Stadium", true, null),
        new Fixture(2, "Oct 29", "4:45 PM IST", "Karnali Yaks", "TU Stadium", true, null),
        new Fixture(3, "Oct 31", "4:45 PM IST", "Sudurpaschim Royals", "TU Stadium", true, null),
        new Fixture(4, "Nov 03", "12:15 PM IST", "Janakpur Bolts", "TU Stadium", true, null),
        new Fixture(5, "Nov 05", "4:45 PM IST", "Kathmandu Gorkhas", "TU Stadium", true, null),
        new Fixture(6, "Nov 12", "12:15 PM IST", "Pokhara Avengers", "TU Stadium", true, null),
        new Fixture(7, "Nov 14", "4:45 PM IST", "Lumbini Lions", "TU Stadium", true, null));

    /**
     * Fixtures: the admin-edited list if present, otherwise the seed. Results the admin left blank are filled
     * automatically from the live Wikipedia results matrix, so scores appear without anyone editing anything.
     */
    public List<Fixture> fixtures() {
        var edited = store.get().fixtures();
        var results = live.snapshot().kingsResults();
        return (edited != null ? edited : SEED_FIXTURES).stream()
            .map(f -> (f.result() == null || f.result().isBlank()) && results.containsKey(f.opponent())
                ? new Fixture(f.id(), f.date(), f.time(), f.opponent(), f.venue(), f.home(), results.get(f.opponent())) : f)
            .toList();
    }

    public KingsStats stats() {
        var s = store.get().stats();
        return s != null ? s : new KingsStats(2225, 682, 96, 3, 69.74, 30.26, 9, 708, 139, 105);
    }

    // ---------- Standings ----------
    /** Season 3 is live (refreshed in the background); Seasons 1–2 are final. */
    public List<Season> seasons() {
        var snap = live.snapshot();
        return List.of(
            new Season(3, 2026, "NPL Season 3 · Group Stage Standings", true, snap.at().toString(), snap.source(), snap.table()),
            new Season(2, 2025, "NPL Season 2 · Group Stage Standings", false, null, "Final · Group stage", List.of(
                new Standing(1, "Sudurpaschim Royals", 7, 6, 1, 0, "+2.47", 12),
                new Standing(2, "Biratnagar Kings", 7, 5, 2, 0, "+0.680", 10),
                new Standing(3, "Kathmandu Gorkhas", 7, 5, 2, 0, "—", 10),
                new Standing(4, "Lumbini Lions", 7, 4, 3, 0, "—", 8),
                new Standing(5, "Pokhara Avengers", 7, 3, 4, 0, "—", 6),
                new Standing(6, "Karnali Yaks", 7, 2, 5, 0, "—", 4),
                new Standing(7, "Chitwan Rhinos", 7, 2, 5, 0, "—", 4),
                new Standing(8, "Janakpur Bolts", 7, 1, 6, 0, "—", 2))),
            new Season(1, 2024, "NPL Season 1 · Group Stage Standings", false, null, "Final · Group stage", List.of(
                new Standing(1, "Sudurpaschim Royals", 7, 6, 1, 0, "+2.087", 12),
                new Standing(2, "Janakpur Bolts", 7, 5, 2, 0, "+0.164", 10),
                new Standing(3, "Chitwan Rhinos", 7, 4, 3, 0, "+0.634", 8),
                new Standing(4, "Karnali Yaks", 7, 4, 3, 0, "-0.184", 8),
                new Standing(5, "Kathmandu Gorkhas", 7, 4, 3, 0, "-0.321", 8),
                new Standing(6, "Biratnagar Kings", 7, 2, 5, 0, "-0.703", 4),
                new Standing(7, "Pokhara Avengers", 7, 2, 5, 0, "-1.332", 4),
                new Standing(8, "Lumbini Lions", 7, 1, 6, 0, "-0.498", 2))));
    }

    @Cacheable("gallery")
    public List<GalleryItem> gallery() {
        return List.of(
            new GalleryItem(1, "Kings on Stage", L + "9d7cc65b-c0d9-4ab7-bf47-1e40805cf697/gallery-5.png"),
            new GalleryItem(2, "Running Between the Wickets", L + "55f043d6-dd5c-4918-93f7-71292c678c03/kingdom-action-1.png"),
            new GalleryItem(3, "The Roar · #25", L + "aa4ad840-9b7f-4dc5-b212-7cde42dc0d65/gallery-9.png"),
            new GalleryItem(4, "Skipper's Anthem", L + "f4c99a66-dedd-462b-90ae-bcb6408c3910/gallery-6.png"),
            new GalleryItem(5, "Smooth Play of the Match", L + "6a27b179-41d0-44f4-a729-069eed52f3d9/kingdom-action-2.png"),
            new GalleryItem(6, "Sky High", L + "39bfef11-7ba6-40c3-97ed-65865d4d170d/gallery-7.png"),
            new GalleryItem(7, "At the Crease", L + "0614d8fe-ae57-455b-aa7a-ff1630f2601b/gallery-8.png"));
    }

    @Cacheable("sponsors")
    public List<Sponsor> sponsors() {
        return List.of(
            new Sponsor("Title Sponsor", "Xtreme Energy Drink", L + "0aa6fe1a-d867-4b80-b82d-ccff5a0ee30b/xtreme-energy-drink.png"),
            new Sponsor("Powered By", "Everest Bank", L + "9e7bd2e3-9989-49d1-b5c3-d92b93d316d2/everest-bank.jpg"),
            new Sponsor("Official Partners", "Freemans", L + "410beae4-6726-44e0-8397-980d303b44aa/freemans.png"),
            new Sponsor("Official Partners", "PLO", L + "2eb9a64b-0585-4234-9289-1619389f63c8/plo-lubricants.jpeg"),
            new Sponsor("Official Partners", "Hyatt Centric", L + "54f2d0d6-0d35-4701-b24d-7ff7a47b770e/hyatt-centric.png"),
            new Sponsor("Official Partners", "Bajeko Sekuwa", L + "1b827819-8289-46c5-a0f5-9ab1014e2f47/bajeko-sekuwa.jpg"));
    }

    @Cacheable("products")
    public List<Product> products() {
        return List.of(new Product("home-jersey-2026", "Home Jersey 2026", "OFFICIAL PLAYER REPLICA",
            "The 2026 home kit — royal navy with gold trim, mountain-etch sublimation and the crown crest embroidered over the heart. Lightweight, breathable, match-grade.",
            List.of("Match-grade polyester", "Sublimated crest", "Slim athletic fit", "Machine washable"),
            List.of("S", "M", "L", "XL", "XXL"),
            List.of(A + "sandeep-lamichhane-nobg-DL9DhKuu.png", A + "basir-ahmad-nobg-DyRY3aw0.png", A + "lokesh-bam-nobg-CGCWVUyI.png"),
            "Dolpo Gears · Thamel & Sorhakhutte", "Dolpo", true));
    }

    public Optional<Product> product(String id) {
        return products().stream().filter(p -> p.id().equals(id)).findFirst();
    }

    public OrderResponse placeOrder(OrderRequest r) {
        String id = "BK-" + orderSeq.incrementAndGet();
        boolean notified = mail.notifyAdmin(id, r);
        String msg = notified
            ? "Order " + id + " received. The Kings store team has been notified and will contact you on " + r.phone() + "."
            : "Order " + id + " received, but the store team could not be emailed automatically. Please keep this order number.";
        var res = new OrderResponse(id, "RECEIVED", notified, msg);
        orders.put(id, res);
        return res;
    }
}
