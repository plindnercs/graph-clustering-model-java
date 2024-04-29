package edu.plus.cs.io;

import edu.plus.cs.Main;
import edu.plus.cs.model.Community;
import edu.plus.cs.model.Member;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class CommunityWriter {
    public static void writeCommunitiesToFile(HashMap<Integer, Community> communitiesStubs, HashMap<Integer,
            Member> membersStubs, boolean onMach2) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = now.format(formatter);

        String outputPath = "random_matching_communities_output_" + timestamp + ".txt";
        if (onMach2) {
            outputPath = Main.MACH2_DIR_PREFIX + outputPath;
        }

        List<Integer> sortedCommunityStubs = communitiesStubs.keySet().stream().sorted().collect(Collectors.toList());
        System.out.println("First community stub id: " + sortedCommunityStubs.get(0));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            for (int communityId : sortedCommunityStubs) {
                List<Integer> members = new ArrayList<>(communitiesStubs.get(communityId).getMembers());
                for (int memberId : members) {
                    writer.write(Integer.toString(memberId));

                    // only place whitespace if current member is not the last one
                    if (memberId != members.get(members.size() - 1)) {
                        writer.write(" ");
                    }
                }
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to the output file: " + e.getMessage());
        }
    }
}
