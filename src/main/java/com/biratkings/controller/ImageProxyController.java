
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
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/gif",
            "image/avif"
    );

    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final int MAX_REDIRECTS = 4;

    private record Img(byte[] bytes, String type) {}

    // Redirects are handled manually so every destination can be validated.
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
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

    public static boolean allowed(URI uri) {
        String path = uri.getPath();

        return "https".equalsIgnoreCase(uri.getScheme())
                && "biratkings.com".equalsIgnoreCase(uri.getHost())
                && uri.getUserInfo() == null
                && uri.getPort() == -1
                && path != null
                && !path.contains("..")
                && (path.startsWith("/assets/")
                || path.startsWith("/__l5e/"));
    }

    @GetMapping("/api/img")
    public ResponseEntity<byte[]> img(@RequestParam("u") String u) {

        URI original;

        try {
            original = URI.create(u);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        if (!allowed(original)) {
            return ResponseEntity.badRequest().build();
        }

        String key = original.toString();

        Img cached = cache.get(key);

        if (cached != null) {
            return imageResponse(cached);
        }

        try {
            URI current = original;
            HttpResponse<byte[]> response = null;

            for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {

                HttpRequest request = HttpRequest.newBuilder(current)
                        .timeout(Duration.ofSeconds(8))
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Accept", "image/avif,image/webp,image/*,*/*;q=0.8")
                        .GET()
                        .build();

                response = http.send(
                        request,
                        HttpResponse.BodyHandlers.ofByteArray()
                );

                int status = response.statusCode();

                // Handle redirects manually.
                if (status == 301 || status == 302 || status == 303
                        || status == 307 || status == 308) {

                    Optional<String> location =
                            response.headers().firstValue("location");

                    if (location.isEmpty()) {
                        log.warn("Image redirect has no Location header: {}",
                                current.getPath());
                        return ResponseEntity.status(502).build();
                    }

                    URI next = current.resolve(location.get());

                    // Never follow a redirect outside the approved host/path.
                    if (!allowed(next)) {
                        log.warn("Image redirect rejected: {} -> {}",
                                current.getHost(), next.getHost());
                        return ResponseEntity.status(502).build();
                    }

                    if (redirects == MAX_REDIRECTS) {
                        log.warn("Too many image redirects: {}", current.getPath());
                        return ResponseEntity.status(502).build();
                    }

                    current = next;
                    continue;
                }

                if (status != 200) {
                    log.warn("Image origin returned HTTP {} for {}",
                            status, current.getPath());
                    return ResponseEntity.status(502).build();
                }

                String type = response.headers()
                        .firstValue("content-type")
                        .orElse("")
                        .split(";")[0]
                        .trim()
                        .toLowerCase(Locale.ROOT);

                byte[] bytes = response.body();

                if (!TYPES.contains(type)) {
                    log.warn("Unsupported image content type '{}' for {}",
                            type, current.getPath());
                    return ResponseEntity.status(502).build();
                }

                if (bytes.length > MAX_BYTES) {
                    log.warn("Image exceeds size limit: {}", current.getPath());
                    return ResponseEntity.status(502).build();
                }

                Img img = new Img(bytes, type);
                cache.put(key, img);

                return imageResponse(img);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Image proxy request interrupted", e);
        } catch (Exception e) {
            log.error("Image proxy request failed: {}", e.toString());
        }

        return ResponseEntity.status(502).build();
    }

    private ResponseEntity<byte[]> imageResponse(Img img) {
        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl.maxAge(Duration.ofDays(7))
                                .cachePublic()
                                .immutable()
                )
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .contentType(MediaType.parseMediaType(img.type()))
                .body(img.bytes());
    }
}
