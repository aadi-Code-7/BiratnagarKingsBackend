package com.biratkings.service;

import com.biratkings.model.Models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/** Admin-editable data (fixtures, stats, counters) persisted to ./data/overrides.json so edits survive restarts. */
@Service
public class OverrideStore {
    private static final Logger log = LoggerFactory.getLogger(OverrideStore.class);

    public record Data(List<Fixture> fixtures, KingsStats stats, Counters counters) {}

    private final ObjectMapper om = new ObjectMapper();
    private final Path file;
    private volatile Data data = new Data(null, null, null);

    public OverrideStore(@Value("${app.data.dir:./data}") String dir) {
        this.file = Path.of(dir, "overrides.json");
        try { if (Files.exists(file)) data = om.readValue(file.toFile(), Data.class); }
        catch (Exception e) { log.warn("Could not read {}: {}", file, e.getMessage()); }
    }

    public Data get() { return data; }

    public synchronized void setFixtures(List<Fixture> f) { data = new Data(List.copyOf(f), data.stats(), data.counters()); save(); }
    public synchronized void setStats(KingsStats s) { data = new Data(data.fixtures(), s, data.counters()); save(); }
    public synchronized void setCounters(Counters c) { data = new Data(data.fixtures(), data.stats(), c); save(); }
    public synchronized void reset() { data = new Data(null, null, null); save(); }

    private void save() {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path tmp = file.resolveSibling("overrides.json.tmp");
            om.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), data);
            try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { throw new IllegalStateException("Could not save data: " + e.getMessage(), e); }
    }
}
