package com.biratkings.service;

import com.biratkings.model.Models.Standing;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LiveStandingsServiceTest {
    // Shaped like MediaWiki's parse output: refs, nbsp entities, flag spans, th/td mix, unicode minus.
    static final String HTML = """
      <table class="wikitable" style="text-align:center"><tbody>
      <tr><th>Pos</th><th>Team</th><th>Pld</th><th>W</th><th>L</th><th>NR</th><th>Pts</th><th>NRR</th><th>Qualification</th></tr>
      <tr><td>1</td><td style="text-align:left"><span class="flagicon"></span>&#160;<a href="/wiki/Karnali_Yaks">Karnali Yaks</a><sup class="reference">[1]</sup></td><td>2</td><td>2</td><td>0</td><td>0</td><td>4</td><td>0.533</td><td>Advance</td></tr>
      <tr><td>5</td><td><b>Biratnagar Kings</b> (Q)</td><td>3</td><td>1</td><td>2</td><td>0</td><td>2</td><td>0.544</td><td></td></tr>
      <tr><td>6</td><td>Sudurpaschim Royals</td><td>3</td><td>1</td><td>2</td><td>0</td><td>2</td><td>&#8722;2.412</td><td></td></tr>
      </tbody></table>
      <table class="wikitable"><tbody>
      <tr><th>Visitor team &#8594;<br/>Home team &#8595;</th><th>BK</th><th>CR</th><th>JB</th><th>KY</th><th>KG</th><th>LL</th><th>PA</th><th>SR</th></tr>
      <tr><th>Biratnagar Kings</th><td></td><td>Chitwan 8 runs</td><td>Match 12</td><td></td><td></td><td>Match 26</td><td>Match 22</td><td></td></tr>
      <tr><th>Chitwan Rhinos</th><td></td><td></td><td></td><td>Match 27</td><td>Match 20</td><td></td><td>Match 17</td><td></td></tr>
      <tr><th>Karnali Yaks</th><td>Biratnagar 5 wickets</td><td></td><td>Match 3</td><td></td><td></td><td>Match 23</td><td></td><td></td></tr>
      <tr><th>Kathmandu Gorkhas</th><td>Match 15</td><td></td><td></td><td>Match 10</td><td></td><td>Match 18</td><td></td><td>Match 24</td></tr>
      <tr><th>Sudurpaschim Royals</th><td>Sudurpaschim Super Over</td><td>Match 11</td><td>Match 21</td><td>Match 16</td><td></td><td></td><td></td><td></td></tr>
      </tbody></table>""";

    @Test void parsesStandings() {
        List<Standing> t = LiveStandingsService.parseStandings(HTML);
        assertEquals(3, t.size());
        assertEquals("Karnali Yaks", t.get(0).team());
        assertEquals("+0.533", t.get(0).nrr());
        assertEquals("Biratnagar Kings", t.get(1).team());      // "(Q)" suffix stripped
        assertEquals(1, t.get(1).won());
        assertEquals("-2.412", t.get(2).nrr());                  // unicode minus normalised
    }

    @Test void parsesKingsResults() {
        Map<String, String> r = LiveStandingsService.parseKingsResults(HTML);
        assertEquals("Lost by 8 runs", r.get("Chitwan Rhinos"));        // Kings home, opponent won
        assertEquals("Won by 5 wickets", r.get("Karnali Yaks"));        // Kings visitor, Kings won
        assertEquals("Lost in Super Over", r.get("Sudurpaschim Royals"));
        assertNull(r.get("Janakpur Bolts"));                             // not played yet
        assertNull(r.get("Kathmandu Gorkhas"));
    }

    @Test void unwrapsMediaWikiJson() {
        String json = "{\"parse\":{\"title\":\"x\",\"text\":\"<p>A \\\"q\\\" \\u2212 b\\n</p>\"}}";
        assertEquals("<p>A \"q\" \u2212 b\n</p>", LiveStandingsService.extractHtml(json));
    }

    @Test void imageProxyAllowList() {
        var ok = java.net.URI.create("https://biratkings.com/assets/x.png");
        assertTrue(com.biratkings.controller.ImageProxyController.allowed(ok));
        for (String bad : new String[]{"http://biratkings.com/assets/x.png", "https://evil.com/assets/x.png",
            "https://biratkings.com.evil.com/assets/x.png", "https://biratkings.com/api/x.png",
            "https://biratkings.com/assets/../etc", "https://u@biratkings.com/assets/x.png", "https://biratkings.com:8443/assets/x.png"})
            assertFalse(com.biratkings.controller.ImageProxyController.allowed(java.net.URI.create(bad)), bad);
    }
}
