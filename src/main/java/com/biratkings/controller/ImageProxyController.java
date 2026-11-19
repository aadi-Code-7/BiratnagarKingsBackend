
package com.biratkings.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@RestController
public class ImageProxyController {

    private static final Logger log =
            LoggerFactory.getLogger(ImageProxyController.class);

    private static final Set<String> TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp",
            "image/gif", "image/avif"
    );

    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final int MAX_REDIRECTS = 5;

    private record Img(byte[] bytes, String type) {}

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private final Map<String, Img> cache =
            Collections.synchronizedMap(
                    new LinkedHashMap<>(64, 0.75f, true) {
                        @Override
                        protected boolean removeEldestEntry(
                                Map.Entry<String, Img> e) {
                            return size() > 200;
                        }
                    }
            );

    public static boolean allowed(URI u) {
        String path = u.getPath();

        return "https".equalsIgnoreCase(u.getScheme())
                && "biratkings.com".equalsIgnoreCase(u.getHost())
                && u.getUserInfo() == null
                && u.getPort() == -1
                && path != null
                && !path.contains("..")
                && (path.startsWith("/assets/")
                || path.startsWith("/__l5e/"));
    }

    @GetMapping("/api/img")
    public ResponseEntity<byte[]> img(@RequestParam("u") String u) {

        URI uri;

        try {
            uri = URI.create(u);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        if (!allowed(uri)) {
            return ResponseEntity.badRequest().build();
        }

        String key = uri.toString();

        Img cached = cache.get(key);

        if (cached != null) {
            return imageResponse(cached);
        }

        try {
            URI current = uri;

            for (int i = 0; i <= MAX_REDIRECTS; i++) {

                HttpRequest request = HttpRequest.newBuilder(current)
                        .timeout(Duration.ofSeconds(15))
                        .header("User-Agent",
                                "Mozilla/5.0 (compatible; BiratKingsImageProxy/1.0)")
                        .header("Accept", "image/avif,image/webp,image/png,image/*")
                        .GET()
                        .build();

                HttpResponse<byte[]> response = http.send(
                        request,
                        HttpResponse.BodyHandlers.ofByteArray()
                );

                int status = response.statusCode();

                // Handle redirects manually and validate every destination.
                if (status == 301 || status == 302 ||
                        status == 303 || status == 307 || status == 308) {

                    Optional<String> location =
                            response.headers().firstValue("location");

                    if (location.isEmpty() || i == MAX_REDIRECTS) {
                        log.warn("Image redirect failed. Status: {}, host: {}",
                                status, current.getHost());
                        return ResponseEntity.status(502).build();
                    }

                    URI next = current.resolve(location.get());

                    if (!allowed(next)) {
                        log.warn("Image redirect rejected. Destination host: {}",
                                next.getHost());
                        return ResponseEntity.status(502).build();
                    }

                    current = next;
                    continue;
                }

                String type = response.headers()
                        .firstValue("content-type")
                        .orElse("")
                        .split(";")[0]
                        .trim()
                        .toLowerCase();

                byte[] body = response.body();

                if (status != 200) {
                    log.warn("Image server returned HTTP {} for {}",
                            status, current.getPath());
                    return ResponseEntity.status(502).build();
                }

                if (!TYPES.contains(type)) {
                    log.warn("Unsupported image content type: {} for {}",
                            type, current.getPath());
                    return ResponseEntity.status(502).build();
                }

                if (body.length > MAX_BYTES) {
                    log.warn("Image exceeds size limit: {} bytes for {}",
                            body.length, current.getPath());
                    return ResponseEntity.status(502).build();
                }

                Img image = new Img(body, type);
                cache.put(key, image);

                return imageResponse(image);
            }

        } catch (Exception e) {
            log.error("Image proxy request failed: {}",
                    e.toString(), e);
        }

        return ResponseEntity.status(502).build();
    }

    private ResponseEntity<byte[]> imageResponse(Img img) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7))
                        .cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy",
                        "default-src 'none'; sandbox")
                .contentType(MediaType.parseMediaType(img.type()))
                .body(img.bytes());
    }
}
