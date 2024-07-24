package edu.plus.cs.io;

import edu.plus.cs.model.Community;
import edu.plus.cs.util.Constants;
import edu.plus.cs.util.LogLevel;
import edu.plus.cs.util.Logger;

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
    public static void writeCommunitiesToFile(HashMap<Integer, Community> communitiesStubs, String prefix,
                                              boolean onMach2, Logger logger) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS");
        String timestamp = now.format(formatter);

        String outputPath = "random_matching_communities_output_" + prefix + timestamp + ".txt";
        if (onMach2) {
            outputPath = Constants.MACH2_DIR_PREFIX + outputPath;
        }

        List<Integer> sortedCommunityStubs = communitiesStubs.keySet().stream().sorted().collect(Collectors.toList());

        logger.log("First community stub id: " + sortedCommunityStubs.get(0), LogLevel.DEBUG, outputPath);
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

            logger.log("Finished writing into file", LogLevel.DEBUG, outputPath);
        } catch (IOException e) {
            logger.log("Error writing to the output file: " + e.getMessage(), LogLevel.ERROR, outputPath);
        }
    }
}
