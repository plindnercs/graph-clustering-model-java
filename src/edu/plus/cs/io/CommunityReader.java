package edu.plus.cs.io;

import edu.plus.cs.model.Community;
import edu.plus.cs.util.LogLevel;
import edu.plus.cs.util.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CommunityReader {

    public static HashMap<Integer, Community> readCommunitiesFromFile(String communitiesFile, Logger logger) {
        HashMap<Integer, Community> communities = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(communitiesFile))) {
            String line;
            int communityId = 1; // communities start at 1
            while ((line = br.readLine()) != null) {
                // splitting each line by spaces to get member IDs as strings, then parsing them to integers.
                HashSet<Integer> memberSet = new HashSet<>();
                for (String memberIdStr : line.split(" ")) {
                    try {
                        int memberId = Integer.parseInt(memberIdStr);
                        memberSet.add(memberId);
                    } catch (NumberFormatException e) {
                        logger.log("Invalid member ID: " + memberIdStr, LogLevel.ERROR, communitiesFile);
                    }
                }

                // Creating a new Community object and adding it to the communities map.
                Community community = new Community(communityId, memberSet);
                communities.put(communityId, community);
                communityId++;
            }

            logger.log("Read communities from file", LogLevel.DEBUG, communitiesFile);

            return communities;
        } catch (IOException e) {
            logger.log("Error reading from file: " + communitiesFile + "; " + e.getMessage(), LogLevel.ERROR, e);
            return null;
        }
    }

    public static Set<Integer> readUniqueMembersFromFile(String communitiesFile, Logger logger) {
        Set<Integer> uniqueMemberIds = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(communitiesFile))) {
            String line;

            while ((line = br.readLine()) != null) {
                for (String memberId : line.split(" ")) {
                    uniqueMemberIds.add(Integer.parseInt(memberId));
                }
            }

            logger.log("Read unique members from community file", LogLevel.DEBUG, communitiesFile);
        } catch (IOException e) {
            logger.log("Error reading from file: " + communitiesFile + "; " + e.getMessage(), LogLevel.ERROR, e);
            return null;
        }

        return uniqueMemberIds;
    }
}