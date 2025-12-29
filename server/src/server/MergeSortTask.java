package server;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Arrays;
import java.util.concurrent.Callable;

public class MergeSortTask implements Callable<int[]> {
    @SuppressFBWarnings("EI_EXPOSE_REP2")
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