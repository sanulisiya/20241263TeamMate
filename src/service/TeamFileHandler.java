package service;

import model.Participant;
import model.PersonalityType;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class TeamFileHandler {

    public static void saveTeamsToCSV(List<List<Participant>> teams, String outputPath) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            StringBuilder sb = new StringBuilder();


            sb.append("Team Number,ID,Name,Email,Preferred Game,Skill Level,Preferred Role,Personality Score,Personality Type\n");

            //  Loop
            for (int i = 0; i < teams.size(); i++) {
                int teamNumber = i + 1;
                List<Participant> team = teams.get(i);

                int totalSkill = 0;
                int leaderCount = 0;
                int thinkerCount = 0;
                int balancedCount = 0;

                for (Participant p : team) {
                    sb.append(teamNumber).append(",")
                            .append(p.getId()).append(",")
                            .append(p.getName()).append(",")
                            .append(p.getEmail()).append(",")
                            .append(p.getPreferredGame()).append(",")
                            .append(p.getSkillLevel()).append(",")
                            .append(p.getPreferredRole()).append(",")
                            .append(p.getPersonalityScore()).append(",")
                            .append(p.getPersonalityType()).append("\n");

                    totalSkill += p.getSkillLevel();

                    // Count personality types
                    PersonalityType type = p.getPersonalityType();
                    if (type.equals(PersonalityType.LEADER)) leaderCount++;
                    else if (type.equals(PersonalityType.THINKER)) thinkerCount++;
                    else balancedCount++;
                }

                double avgSkill = (team.isEmpty()) ? 0 : (double) totalSkill / team.size();


                sb.append("Summary for Team ").append(teamNumber).append(",,")
                        .append("Average Skill: ").append(String.format("%.2f", avgSkill))
                        .append(", Leaders: ").append(leaderCount)
                        .append(", Thinkers: ").append(thinkerCount)
                        .append(", Balanced: ").append(balancedCount)
                        .append(",Motivator: ").append(balancedCount)
                        .append("\n");

                sb.append("----------------------------------------------------------------\n");
            }

            writer.write(sb.toString());

            System.out.println("\n Teams successfully saved to: " + outputPath);

        } catch (IOException e) {
            System.err.println(" Error writing to CSV file: " + e.getMessage());
        }
    }
}