package client;

import java.util.Arrays;
import java.util.Random;

public class ArrayUtils {
    
    public static int[] parseArray(String input) {
        String[] stringArray = input.split(",");
        int[] intArray = new int[stringArray.length];
        for (int i = 0; i < stringArray.length; i++) {
            intArray[i] = Integer.parseInt(stringArray[i].trim());
        }
        return intArray;
    }

    public static String formatArray(int[] array) {
        return Arrays.toString(array).replaceAll("[\\[\\]]", "");
    }

    public static int[] randomArray(int size, long seed) {
        int[] a = new int[size];
        Random rnd = new Random(seed);
        for (int i = 0; i < size; i++) {
            a[i] = rnd.nextInt();
        }
        return a;
    }
}