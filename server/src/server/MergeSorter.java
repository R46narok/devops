package server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MergeSorter {

    private static final int HARD_MAX_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() * 2);

    public int[] mergeSort(int[] array, int numThreads) {
        if (array == null) throw new IllegalArgumentException("array must not be null");
        int n = array.length;
        if (n <= 1) return array.clone();

        int maxUsableThreads = Math.min(HARD_MAX_THREADS, Math.max(1, n / 2));
        int safeThreads = Math.min(Math.max(1, numThreads), maxUsableThreads);

        int chunks = safeThreads;
        chunks = Math.min(chunks, n);

        int baseSize = n / chunks;
        int remainder = n % chunks;

        ExecutorService pool = Executors.newFixedThreadPool(safeThreads, r -> {
            Thread t = new Thread(r, "merge-sort-worker");
            t.setDaemon(true);
            return t;
        });

        List<Future<int[]>> futures = new ArrayList<>(chunks);
        int start = 0;
        for (int i = 0; i < chunks; i++) {
            int size = baseSize + (i < remainder ? 1 : 0);
            int end = start + size;
            futures.add(pool.submit(new MergeSortTask(array, start, end)));
            start = end;
        }

        List<int[]> sortedChunks = new ArrayList<>(chunks);
        try {
            for (Future<int[]> f : futures) {
                sortedChunks.add(f.get());
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
            throw new RuntimeException("Sorting interrupted", ie);
        } catch (ExecutionException ee) {
            pool.shutdownNow();
            throw new RuntimeException("Sorting failed", ee.getCause());
        } finally {
            pool.shutdown();
        }

        while (sortedChunks.size() > 1) {
            List<int[]> next = new ArrayList<>((sortedChunks.size() + 1) / 2);
            for (int i = 0; i < sortedChunks.size(); i += 2) {
                if (i + 1 < sortedChunks.size()) {
                    next.add(merge(sortedChunks.get(i), sortedChunks.get(i + 1)));
                } else {
                    next.add(sortedChunks.get(i));
                }
            }
            sortedChunks = next;
        }

        return sortedChunks.get(0);
    }

    private int[] merge(int[] left, int[] right) {
        int[] out = new int[left.length + right.length];
        int i = 0, j = 0, k = 0;
        while (i < left.length && j < right.length) {
            if (left[i] <= right[j]) {
                out[k++] = left[i++];
            } else {
                out[k++] = right[j++];
            }
        }
        while (i < left.length) out[k++] = left[i++];
        while (j < right.length) out[k++] = right[j++];
        return out;
    }
}