package cn.wanghw;

import java.util.List;
import java.util.concurrent.*;

/**
 * Parallel scanning framework for concurrent Spider execution.
 */
public class ParallelScanner {

    private final IHeapHolder heapHolder;
    private final int threadCount;

    public ParallelScanner(IHeapHolder heapHolder) {
        this(heapHolder, Math.min(Runtime.getRuntime().availableProcessors(), 8));
    }

    public ParallelScanner(IHeapHolder heapHolder, int threadCount) {
        this.heapHolder = heapHolder;
        this.threadCount = Math.max(1, threadCount);
    }

    /**
     * Scan spiders in parallel using a fixed thread pool.
     */
    public ConcurrentHashMap<String, String> scan(List<ISpider> spiders, Severity minSeverity) {
        ConcurrentHashMap<String, String> results = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(threadCount, spiders.size()));
        List<Future<?>> futures = new java.util.ArrayList<>();

        for (ISpider spider : spiders) {
            if (spider.getSeverity().ordinal() > minSeverity.ordinal()) continue;
            futures.add(executor.submit(() -> {
                try {
                    String result = spider.sniff(heapHolder);
                    if (result != null && !result.isEmpty()) {
                        results.put(spider.getName(), result);
                    }
                } catch (Exception e) {
                    results.put(spider.getName(), "ERROR: " + e.getMessage());
                }
            }));
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                System.err.println("[ParallelScanner] task failed: " + e.getMessage());
            }
        }
        executor.shutdown();
        return results;
    }
}
