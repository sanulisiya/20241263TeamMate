package service;

import model.Participant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ParticipantLoaderThreadManager {

    private static final int CHUNK_SIZE = 1024 * 1024;
    private static final int THREAD_POOL_SIZE = 4;

    // ------------ PUBLIC METHOD TO LOAD WITH THREADS ------------

    public static List<Participant> loadParticipantsMultiThread(String filePath) {
        File file = new File(filePath);
        long fileLength = file.length();

        List<Callable<List<Participant>>> tasks = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        long currentPosition = 0;

        while (currentPosition < fileLength) {
            long length = Math.min(CHUNK_SIZE, fileLength - currentPosition);
            tasks.add(new participantLoaderTask(filePath, currentPosition, length));
            currentPosition += length;
        }

        List<Participant> allParticipants = new ArrayList<>();

        try {
            List<Future<List<Participant>>> results = executor.invokeAll(tasks);

            for (Future<List<Participant>> f : results) {
                allParticipants.addAll(f.get());
            }

        } catch (Exception e) {
            System.err.println("Thread loading error: " + e.getMessage());
        } finally {
            executor.shutdown();
        }

        return allParticipants;
    }
}
