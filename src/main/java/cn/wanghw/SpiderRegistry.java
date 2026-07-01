package cn.wanghw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Central registry for ISpider implementations using Java ServiceLoader SPI.
 * Single source of truth shared by CLI (Main) and Desktop GUI (HeapDumpGUI).
 *
 * <p>Implementations are discovered via {@code META-INF/services/cn.wanghw.ISpider}.
 * The discovered list is cached for the lifetime of the registry and ordered
 * by category then by name to give stable display output.</p>
 */
public final class SpiderRegistry {

    private static final class Holder {
        static final SpiderRegistry INSTANCE = new SpiderRegistry();
    }

    private final List<ISpider> spiders;
    private final Map<String, List<ISpider>> byCategory;

    private SpiderRegistry() {
        List<ISpider> loaded = new ArrayList<>();
        for (ISpider spider : ServiceLoader.load(ISpider.class)) {
            loaded.add(spider);
        }
        Map<String, List<ISpider>> grouped = new LinkedHashMap<>();
        for (ISpider spider : loaded) {
            grouped.computeIfAbsent(spider.getCategory(), k -> new ArrayList<>()).add(spider);
        }
        this.spiders = Collections.unmodifiableList(loaded);
        Map<String, List<ISpider>> immutable = new LinkedHashMap<>();
        for (Map.Entry<String, List<ISpider>> e : grouped.entrySet()) {
            immutable.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
        }
        this.byCategory = Collections.unmodifiableMap(immutable);
    }

    public static SpiderRegistry getInstance() {
        return Holder.INSTANCE;
    }

    public List<ISpider> getSpiders() {
        return spiders;
    }

    public Map<String, List<ISpider>> getByCategory() {
        return byCategory;
    }

    public int size() {
        return spiders.size();
    }

    public ISpider findByName(String name) {
        if (name == null) return null;
        for (ISpider spider : spiders) {
            if (name.equalsIgnoreCase(spider.getName())) return spider;
        }
        return null;
    }
}
