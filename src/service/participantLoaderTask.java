package service;

import model.Participant;
import model.RoleType;
import model.PersonalityType;

import java.io.RandomAccessFile;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class participantLoaderTask implements Callable<List<Participant>> {

    private final String filePath;
    private final long start;
    private final long length;
    private final List<Participant> participants = new ArrayList<>();

    public participantLoaderTask(String filePath, long start, long length) {
        this.filePath = filePath;
        this.start = start;
        this.length = length;
    }

    @Override
    public List<Participant> call() throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(new File(filePath), "r")) {
            raf.seek(start);

            if (start == 0) {
                raf.readLine(); // skip header
            } else {
                raf.readLine(); // skip partial line
            }

            long end = start + length;
            String line;

            while (raf.getFilePointer() < end && (line = raf.readLine()) != null) {
                Participant p = parseParticipant(line);
                if (p != null) participants.add(p);
            }
        }
        return participants;
    }

    private Participant parseParticipant(String line) {
        String[] data = line.split(",");
        if (data.length < 8) return null;

        try {
            RoleType role = RoleType.valueOf(data[5].trim().toUpperCase());
            PersonalityType type = PersonalityType.valueOf(data[7].trim().toUpperCase());

            return new Participant(
                    data[0].trim(),
                    data[1].trim(),
                    data[2].trim(),
                    data[3].trim(),
                    Integer.parseInt(data[4].trim()),
                    role,
                    Integer.parseInt(data[6].trim()),
                    type
            );

        } catch (Exception e) {
            return null;
        }
    }
}
