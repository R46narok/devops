package server;

import java.util.Arrays;
import java.util.concurrent.Callable;

public class MergeSortTask implements Callable<int[]> {
    private final int[] source;
    private final int start;
    private final int end;

    public MergeSortTask(int[] source, int start, int end) {
        this.source = source;
        this.start = start;
        this.end = end;
    }

    @Override
    public int[] call() {
        int[] slice = Arrays.copyOfRange(source, start, end);
        Arrays.sort(slice); 
        return slice;
    }
}