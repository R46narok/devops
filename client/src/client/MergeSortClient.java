package client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public class MergeSortClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        System.out.println("MergeSort Client started. Enter array and thread count. Type 'quit' to exit.");
        System.out.println("Format: <comma-separated integers> <number of threads>");
        System.out.println("Commands: benchmark <size> <maxThreads> <runs>");

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                    break;
                }

                if (line.toLowerCase(Locale.ROOT).startsWith("benchmark")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length < 4) {
                        System.out.println("Usage: benchmark <size> <maxThreads> <runs>");
                        continue;
                    }
                    try {
                        int size = Integer.parseInt(parts[1]);
                        int maxThreads = Integer.parseInt(parts[2]);
                        int runs = Integer.parseInt(parts[3]);
                        if (size <= 0 || maxThreads <= 0 || runs <= 0) {
                            System.out.println("All parameters must be positive integers.");
                            continue;
                        }
                        runBenchmark(size, maxThreads, runs);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid parameters. Expected integers for size, maxThreads, and runs.");
                    }
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length < 2) {
                    System.out.println("Invalid input. Expected: <comma-separated integers> <number of threads>");
                    continue;
                }
                String threadsToken = parts[parts.length - 1];
                StringBuilder arrayBuilder = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    if (i > 0) arrayBuilder.append(' ');
                    arrayBuilder.append(parts[i]);
                }
                String arrayInput = arrayBuilder.toString();

                int[] array;
                int numberOfThreads;
                try {
                    array = ArrayUtils.parseArray(arrayInput);
                } catch (Exception e) {
                    System.out.println("Failed to parse array: " + e.getMessage());
                    continue;
                }
                try {
                    numberOfThreads = Integer.parseInt(threadsToken);
                    if (numberOfThreads <= 0) {
                        System.out.println("Number of threads must be positive.");
                        continue;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number of threads: " + threadsToken);
                    continue;
                }

                try {
                    out.writeObject(array);
                    out.writeInt(numberOfThreads);
                    out.flush();

                    int[] sortedArray = (int[]) in.readObject();
                    System.out.println("Sorted Array: " + Arrays.toString(sortedArray));
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("Request failed: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to connect to server: " + e.getMessage());
        } finally {
            scanner.close();
            System.out.println("Client terminated.");
        }
    }

    private static void runBenchmark(int size, int maxThreads, int runs) {
        System.out.println("Running benchmark: size=" + size + ", maxThreads=" + maxThreads + ", runs=" + runs);
        int[] base = ArrayUtils.randomArray(size, 42L);
        for (int threads = 1; threads <= maxThreads; threads++) {
            long totalNanos = 0L;
            for (int r = 0; r < runs; r++) {
                int[] arr = Arrays.copyOf(base, base.length);
                long start = System.nanoTime();
                try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                    out.writeObject(arr);
                    out.writeInt(threads);
                    out.flush();

                    int[] sorted = (int[]) in.readObject();
                    if (!isSorted(sorted)) {
                        System.out.println("Validation failed for threads=" + threads + " run=" + r);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("Benchmark request failed (threads=" + threads + "): " + e.getMessage());
                    return;
                }
                long end = System.nanoTime();
                totalNanos += (end - start);
            }
            double avgMs = totalNanos / (runs * 1_000_000.0);
            System.out.printf("threads=%d avg=%.3f ms%n", threads, avgMs);
        }
        System.out.println("Benchmark complete.");
    }

    private static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i] < a[i - 1]) return false;
        }
        return true;
    }
}