package server;
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

            while (!clientSocket.isClosed()) {
                try {
                    Object obj = in.readObject();
                    if (!(obj instanceof int[])) {
                        break;
                    }
                    int[] array = (int[]) obj;
                    int numThreads = in.readInt();

                    MergeSorter sorter = new MergeSorter();
                    int[] sortedArray = sorter.mergeSort(array, numThreads);

                    out.writeObject(sortedArray);
                    out.flush();
                } catch (EOFException e) {
                    break;
                } catch (SocketException e) {
                    break;
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}