package com.biratkings.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tiny dependency-free extractor for MediaWiki "wikitable" HTML: table -> rows -> cell text. */
public final class WikiTables {
    private WikiTables() {}

    private static final Pattern TABLE = Pattern.compile("<table[^>]*class=\"[^\"]*wikitable[^\"]*\"[^>]*>(.*?)</table>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern ROW = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern CELL = Pattern.compile("<t([hd])[^>]*>(.*?)</t\\1>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern NUM_ENT = Pattern.compile("&#(\\d+);");
    private static final Pattern HEX_ENT = Pattern.compile("&#x([0-9a-fA-F]+);");

    /** @return every wikitable as a list of rows, each row a list of cell texts. */
    public static List<List<List<String>>> tables(String html) {
        List<List<List<String>>> out = new ArrayList<>();
        Matcher t = TABLE.matcher(html == null ? "" : html);
        while (t.find()) {
            List<List<String>> rows = new ArrayList<>();
            Matcher r = ROW.matcher(t.group(1));
            while (r.find()) {
                List<String> cells = new ArrayList<>();
                Matcher c = CELL.matcher(r.group(1));
                while (c.find()) cells.add(text(c.group(2)));
                if (!cells.isEmpty()) rows.add(cells);
            }
            out.add(rows);
        }
        return out;
    }

    public static String text(String html) {
        String s = html.replaceAll("(?is)<sup[^>]*>.*?</sup>", "")
                       .replaceAll("(?is)<style[^>]*>.*?</style>", "")
                       .replaceAll("(?i)<br\\s*/?>", " ")
                       .replaceAll("<[^>]+>", "");
        s = s.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
             .replace("&minus;", "-").replace("&ndash;", "–").replace("&mdash;", "—");
        s = decode(NUM_ENT, s, 10);
        s = decode(HEX_ENT, s, 16);
        return s.replace('\u00a0', ' ').replace('\u2212', '-').replaceAll("\\s+", " ").trim();
    }

    private static String decode(Pattern p, String s, int radix) {
        Matcher m = p.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, Matcher.quoteReplacement(new String(Character.toChars(Integer.parseInt(m.group(1), radix)))));
        m.appendTail(sb);
        return sb.toString();
    }
}
