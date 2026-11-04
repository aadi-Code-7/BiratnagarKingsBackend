package com.biratkings.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Serves biratkings.com images from OUR origin (and caches them). This removes cross-origin / referrer / hotlink
 * problems on mobile browsers and makes repeat loads instant. Strict allow-list, so it cannot be used as an open proxy.
 */
@RestController
public class ImageProxyController {
    private static final Set<String> TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif", "image/avif");
    private static final int MAX_BYTES = 8 * 1024 * 1024;

    private record Img(byte[] bytes, String type) {}

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(); // never follows redirects
    private final Map<String, Img> cache = Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Img> e) { return size() > 200; }
    });

    public static boolean allowed(URI u) {
        String path = u.getPath();
        return "https".equals(u.getScheme()) && "biratkings.com".equalsIgnoreCase(u.getHost()) && u.getUserInfo() == null && u.getPort() == -1
            && path != null && !path.contains("..") && (path.startsWith("/assets/") || path.startsWith("/__l5e/"));
    }

    @GetMapping("/api/img")
    public ResponseEntity<byte[]> img(@RequestParam("u") String u) {
        URI uri;
        try { uri = URI.create(u); } catch (IllegalArgumentException e) { return ResponseEntity.badRequest().build(); }
        if (!allowed(uri)) return ResponseEntity.badRequest().build();
        String key = uri.toString();
        Img img = cache.get(key);
        if (img == null) {
            try {
                HttpResponse<byte[]> r = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
                String type = r.headers().firstValue("content-type").orElse("").split(";")[0].trim().toLowerCase();
                if (r.statusCode() != 200 || !TYPES.contains(type) || r.body().length > MAX_BYTES) return ResponseEntity.status(502).build();
                img = new Img(r.body(), type);
                cache.put(key, img);
            } catch (Exception e) { return ResponseEntity.status(502).build(); }
        }
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic().immutable())
            .header("X-Content-Type-Options", "nosniff")
            .header("Content-Security-Policy", "default-src 'none'; sandbox")
            .contentType(MediaType.parseMediaType(img.type()))
            .body(img.bytes());
    }
}
