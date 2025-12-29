package server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MergeSortTaskTest {

    @Test
    void testSortEntireArray() throws Exception {
        int[] source = {5, 2, 8, 1, 9};
        MergeSortTask task = new MergeSortTask(source, 0, source.length);

        int[] result = task.call();

        assertArrayEquals(new int[]{1, 2, 5, 8, 9}, result);
    }

    @Test
    void testSortSliceOfArray() throws Exception {
        int[] source = {5, 2, 8, 1, 9};
        MergeSortTask task = new MergeSortTask(source, 1, 4);

        int[] result = task.call();

        assertArrayEquals(new int[]{1, 2, 8}, result);
    }

    @Test
    void testSortSingleElement() throws Exception {
        int[] source = {42};
        MergeSortTask task = new MergeSortTask(source, 0, 1);

        int[] result = task.call();

        assertArrayEquals(new int[]{42}, result);
    }

    @Test
    void testSortAlreadySortedArray() throws Exception {
        int[] source = {1, 2, 3, 4, 5};
        MergeSortTask task = new MergeSortTask(source, 0, source.length);

        int[] result = task.call();

        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void testOriginalArrayUnmodified() throws Exception {
        int[] source = {5, 2, 8};
        MergeSortTask task = new MergeSortTask(source, 0, source.length);

        task.call();

        assertArrayEquals(new int[]{5, 2, 8}, source);
    }
}
