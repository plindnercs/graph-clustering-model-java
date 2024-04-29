package edu.plus.cs.io;

import edu.plus.cs.model.Community;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class CommunityReader {

    public static HashMap<Integer, Community> readCommunitiesFromFile(String communitiesFile) {
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
                        System.err.println("Invalid member ID: " + memberIdStr);
                    }
                }

                // Creating a new Community object and adding it to the communities map.
                Community community = new Community(communityId, memberSet);
                communities.put(communityId, community);
                communityId++;
            }

            return communities;
        } catch (IOException e) {
            System.err.println("Error reading from the file: " + e.getMessage());
            return null;
        }
    }
}