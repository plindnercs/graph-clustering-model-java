package edu.plus.cs.io;

import edu.plus.cs.model.Community;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class OverlapFunctionReader {
    private HashMap<Integer, Community> communities;
    private int[][] h;

    public OverlapFunctionReader(HashMap<Integer, Community> communities) {
        this.communities = communities;
    }

    public int[][] readOverlapFunctionFromFile(String overlapsFile) {
        int maxCommunitySize = findMaxCommunitySize();
        int maxOverlap = findMaxOverlap(overlapsFile);

        // Initialize the 2D array with sizes + 1 (to account for the index starting at 0)
        h = new int[maxCommunitySize + 1][maxOverlap + 1];

        // Read the file again to fill in the 2D array
        try (BufferedReader br = new BufferedReader(new FileReader(overlapsFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split(",");
                int overlapSize = Integer.parseInt(parts[0].trim());
                int communitySize = Integer.parseInt(parts[1].trim());
                int count = Integer.parseInt(parts[2].trim());

                h[communitySize][overlapSize] = count;
            }

            return h;
        } catch (IOException e) {
            System.err.println("Error reading from the file: " + e.getMessage());
            return null;
        }
    }

    private int findMaxCommunitySize() {
        return communities.values().stream()
                .mapToInt(Community::getNumberOfMembers) // Assuming Community has a method getNumberOfMembers
                .max()
                .orElse(0);
    }

    private int findMaxOverlap(String overlapsFile) {
        int maxOverlap = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(overlapsFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                int overlap = Integer.parseInt(line.trim().split(",")[0]);
                if (overlap > maxOverlap) {
                    maxOverlap = overlap;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading from the file: " + e.getMessage());
        }
        return maxOverlap;
    }
}
