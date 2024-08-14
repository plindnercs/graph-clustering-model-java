package edu.plus.cs.impl;

import edu.plus.cs.io.CommunityReader;
import edu.plus.cs.io.GraphWriter;
import edu.plus.cs.model.Community;
import edu.plus.cs.util.Constants;
import edu.plus.cs.util.LogLevel;
import edu.plus.cs.util.Logger;
import edu.plus.cs.util.Mode;

import java.util.*;

public class DrawEdgesImpl {
    public static void drawEdges(String matchedCommunitiesFile, int targetNumberOfEdges, double deviationFactor,
                                  boolean onMach2, Logger logger) {
        logger.log("Starting drawing edges ...", LogLevel.INFO);

        if (onMach2) {
            matchedCommunitiesFile = Constants.MACH2_DIR_PREFIX + matchedCommunitiesFile;
        }

        HashMap<Integer, Community> matchedCommunities = CommunityReader.readCommunitiesFromFile(matchedCommunitiesFile,
                logger);

        if (matchedCommunities == null) {
            logger.log("Could not read matched communities!", LogLevel.ERROR, matchedCommunitiesFile,
                    targetNumberOfEdges, deviationFactor);
            return;
        }

        // first we create adjacencyLists for all member/vertices in our graph
        HashMap<Integer, Set<Integer>> adjacencyLists = getEmptyAdjacencyLists(matchedCommunities);

        // we start with an initial edge probability of 0.5 and do a binary search inspired approach to hit the
        // target of drawn edges inside our graph
        double p = 0.125;
        double l = 0.0;
        double r = 1.0;
        Random random = new Random();
        boolean thresholdReached = false;
        int currentNumberOfEdges = 0;
        List<Integer> matchedCommunityIds = new ArrayList<>(matchedCommunities.keySet());
        while (!thresholdReached) {
            for (int matchedCommunityId : matchedCommunityIds) {
                Community currentCommunity = matchedCommunities.get(matchedCommunityId);
                for (int memberId : currentCommunity.getMembers()) {
                    for (int otherMemberId : currentCommunity.getMembers()) {
                        if (otherMemberId == memberId) {
                            continue;
                        }

                        if (random.nextDouble() <= p) {
                            // draw edge between vertices
                            adjacencyLists.get(memberId).add(otherMemberId);
                            adjacencyLists.get(otherMemberId).add(memberId);
                        }
                    }
                }
            }

            currentNumberOfEdges = getNumberOfEdges(adjacencyLists);

            if (currentNumberOfEdges < (targetNumberOfEdges - (targetNumberOfEdges * deviationFactor))) {
                l = p;
                p = p + (0.5 * (r - l));

                adjacencyLists = null;
                System.gc();
                adjacencyLists = getEmptyAdjacencyLists(matchedCommunities);
            } else if (currentNumberOfEdges > (targetNumberOfEdges + (targetNumberOfEdges * deviationFactor))) {
                r = p;
                p = p - (0.5 * (r - l));

                adjacencyLists = null;
                System.gc();
                adjacencyLists = getEmptyAdjacencyLists(matchedCommunities);
            } else {
                thresholdReached = true;
            }
        }

        GraphWriter.writeGraphToFile(adjacencyLists, currentNumberOfEdges, Mode.DRAW_EDGES, onMach2, logger, true);

        logger.log("Reached target number of edges " + targetNumberOfEdges + " with deviation factor of "
                + deviationFactor + " by drawing " + currentNumberOfEdges + " edges", LogLevel.INFO, matchedCommunitiesFile);
    }

    private static HashMap<Integer, Set<Integer>> getEmptyAdjacencyLists(HashMap<Integer, Community> matchedCommunities) {
        List<Integer> matchedCommunityIds = new ArrayList<>(matchedCommunities.keySet());

        HashMap<Integer, Set<Integer>> adjacencyLists = new HashMap<>();
        for (int matchedCommunityId : matchedCommunityIds) {
            Community currentCommunity = matchedCommunities.get(matchedCommunityId);
            for (int memberId : currentCommunity.getMembers()) {
                adjacencyLists.put(memberId, new HashSet<>());
            }
        }

        return adjacencyLists;
    }

    private static int getNumberOfEdges(HashMap<Integer, Set<Integer>> adjacencyLists) {
        int numberOfEdgeEntries = 0;

        List<Integer> verticesIds = new ArrayList<>(adjacencyLists.keySet());

        for (int vertexId : verticesIds) {
            numberOfEdgeEntries += adjacencyLists.get(vertexId).size();
        }

        return numberOfEdgeEntries / 2;
    }
}
