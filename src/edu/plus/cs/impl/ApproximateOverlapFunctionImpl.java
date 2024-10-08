package edu.plus.cs.impl;

import edu.plus.cs.io.CommunityWriter;
import edu.plus.cs.io.FunctionOutputWriter;
import edu.plus.cs.model.ChangedPosition;
import edu.plus.cs.model.Community;
import edu.plus.cs.model.CommunityAdjacency;
import edu.plus.cs.model.Member;
import edu.plus.cs.util.CommunityHelper;
import edu.plus.cs.util.FunctionChange;
import edu.plus.cs.util.LogLevel;
import edu.plus.cs.util.Logger;

import java.util.*;

public class ApproximateOverlapFunctionImpl {

    /**
     * Tries to approximate the original overlap function by
     * swapping members between communities
     * @param communitiesStubs Current communities
     * @param membersStubs Current members
     * @param communityAdjacencies Overlaps between communities
     * @param h Overlap function
     * @param hGenerated Generated overlap function
     * @param onMach2 Changes output file directory prefix
     * @param logger Logging Object
     */
    public static void approximateOverlapFunction(HashMap<Integer, Community> communitiesStubs,
                                                  HashMap<Integer, Member> membersStubs, HashMap<Integer,
            HashMap<Integer, CommunityAdjacency>> communityAdjacencies, int[][] h, int[][] hGenerated, boolean onMach2,
                                                  Logger logger) {
        Random random = new Random();

        List<Integer> communityStubsIds = new ArrayList<>(communitiesStubs.keySet());

        List<Date> swapTimestamps = new LinkedList<>();
        Date lastPrintTime = new Date();
        int numberOfConsecutiveIncorrectSwaps = 0;
        boolean incorrectSwap = false;
        long numberOfSwaps = 0;

        // TODO: experimental setup
        Date startTime = new Date();
        int sixHoursInMs = 21600000;
        double improvementFactor = 1.5; // 2.0 before

        logger.log("Error before swaps: " + calculateQuadraticError(h, hGenerated), LogLevel.INFO);
        while (numberOfConsecutiveIncorrectSwaps < 1000 && numberOfSwaps < 1000000
            && (new Date().getTime() - startTime.getTime() < sixHoursInMs)) { // here we only want to compute for six hours
            // choose two random communities
            Community firstCommunity = communitiesStubs.get(random.ints(1, 1, communityStubsIds.size())
                    .findFirst().getAsInt());
            Community secondCommunity = communitiesStubs.get(random.ints(1, 1, communityStubsIds.size())
                    .findFirst().getAsInt());

            if (firstCommunity.getId() == secondCommunity.getId()) {
                continue;
            }

            // choose a member of each community
            int firstMember = new ArrayList<>(firstCommunity.getMembers()).get(random.ints(1, 0, firstCommunity.getMembers().size()).findFirst().getAsInt());
            int secondMember = new ArrayList<>(secondCommunity.getMembers()).get(random.ints(1, 0, secondCommunity.getMembers().size()).findFirst().getAsInt());

            if (firstMember == secondMember || secondCommunity.getMembers().contains(firstMember) || firstCommunity.getMembers().contains(secondMember)) {
                // TODO: choose another random member in this case?
                continue;
            }

            // Map<Integer, CommunityAdjacency> firstCommunityAdjacencies = communityAdjacencies.get(firstCommunity.getId());
            // Map<Integer, CommunityAdjacency> secondCommunityAdjacencies = communityAdjacencies.get(secondCommunity.getId());

            // identify affected communities
            Set<Integer> affectedCommunities = identifyAffectedCommunities(firstMember, secondMember, membersStubs);

            // backup the original state in case there was no improvement
            HashSet<Integer> originalFirstCommunityMembers = new HashSet<>(firstCommunity.getMembers());
            HashSet<Integer> originalSecondCommunityMembers = new HashSet<>(secondCommunity.getMembers());
            HashSet<Integer> originalFirstMemberCommunities = new HashSet<>(membersStubs.get(firstMember).getCommunities());
            HashSet<Integer> originalSecondMemberCommunities = new HashSet<>(membersStubs.get(secondMember).getCommunities());
            // TODO: only backup changed values as key-value-pairs?
            int[][] originalHGenerated = copyMatrix(hGenerated);
            // TODO: only backup communityAdjacencies that potentially change?
            // HashMap<Integer, HashMap<Integer, CommunityAdjacency>> originalCommunityAdjacencies =
            // copyCommunityAdjacencies(communityAdjacencies);
            HashMap<Integer, HashMap<Integer, CommunityAdjacency>> originalCommunityAdjacencies =
                    backupCommunityAdjacencies(communityAdjacencies, affectedCommunities);

            // set to track the changes
            Set<ChangedPosition> changedPositions = new HashSet<>();

            // swap members
            firstCommunity.getMembers().remove(firstMember);
            secondCommunity.getMembers().remove(secondMember);
            firstCommunity.getMembers().add(secondMember);
            secondCommunity.getMembers().add(firstMember);

            // set new communities into member communities
            membersStubs.get(firstMember).getCommunities().remove(firstCommunity.getId());
            membersStubs.get(secondMember).getCommunities().remove(secondCommunity.getId());
            membersStubs.get(firstMember).getCommunities().add(secondCommunity.getId());
            membersStubs.get(secondMember).getCommunities().add(firstCommunity.getId());

            // update the overlaps
            updateOverlaps(firstCommunity, secondCommunity, firstMember, secondMember, communitiesStubs, membersStubs,
                    communityAdjacencies, hGenerated, changedPositions, logger);

            // evaluate the new state
            if (isImprovement(originalHGenerated, hGenerated, h, changedPositions, improvementFactor)) {
                logger.log("Improvement found with swap!", LogLevel.DEBUG, firstCommunity.getId(), secondCommunity.getId(), firstMember, secondMember);
                incorrectSwap = false;
                numberOfConsecutiveIncorrectSwaps = 0;

                swapTimestamps.add(new Date());
                cleanOldTimestamps(swapTimestamps);
                numberOfSwaps++;
            } else {
                if (numberOfConsecutiveIncorrectSwaps % 10 == 0) {
                    logger.log("No improvement found with swap, rolling back changes!", LogLevel.DEBUG, firstCommunity.getId(), secondCommunity.getId(), firstMember, secondMember);
                }

                // restore the original state if no improvement
                firstCommunity.setMembers(originalFirstCommunityMembers);
                secondCommunity.setMembers(originalSecondCommunityMembers);
                membersStubs.get(firstMember).setCommunities(originalFirstMemberCommunities);
                membersStubs.get(secondMember).setCommunities(originalSecondMemberCommunities);
                hGenerated = originalHGenerated;
                // communityAdjacencies = originalCommunityAdjacencies;

                for (Integer communityId : affectedCommunities) {
                    if (originalCommunityAdjacencies.containsKey(communityId)) {
                        communityAdjacencies.put(communityId, originalCommunityAdjacencies.get(communityId));
                    } else {
                        communityAdjacencies.remove(communityId);
                    }
                }

                if (incorrectSwap) {
                    numberOfConsecutiveIncorrectSwaps++;
                }
                incorrectSwap = true;
            }

            // print the number of swaps in the last hour every 10 minutes
            Date now = new Date();
            if (now.getTime() - lastPrintTime.getTime() >= 3600000) { // 600000 milliseconds = 10 minutes
                logger.log("Number of swaps in the last hour: " + swapTimestamps.size(), LogLevel.DEBUG);
                lastPrintTime = now;

                if (swapTimestamps.size() < 100) {
                    logger.log("Less than 100 successful swaps of connections in the last hour, aborting program!",
                            LogLevel.INFO, swapTimestamps.size());

                    break;
                }

                logger.log("Writing intermediate result to file ...", LogLevel.DEBUG);
                logger.log("Intermediate error after swaps: " + calculateQuadraticError(h, hGenerated), LogLevel.DEBUG);

                CommunityWriter.writeCommunitiesToFile(communitiesStubs, "intermediate_approx_", onMach2, logger);
                FunctionOutputWriter.writeToOutputFile(hGenerated, "intermediate_approx_", onMach2, logger);
            }
        }

        if (numberOfConsecutiveIncorrectSwaps == 1000) {
            logger.log("Found no further improvements through swaps!", LogLevel.INFO);
        }

        logger.log("Error after swaps: " + calculateQuadraticError(h, hGenerated), LogLevel.INFO);
        logger.log("Total swaps executed: " + numberOfSwaps, LogLevel.INFO);

        logger.log("Finished approximating the target function", LogLevel.INFO);

        CommunityWriter.writeCommunitiesToFile(communitiesStubs, "after_approx_", onMach2, logger);

        FunctionOutputWriter.writeToOutputFile(hGenerated, "after_approx_", onMach2, logger);
    }

    /**
     * Updates communityAdjacencies and hGenerated to reflect swaps of
     * community members
     * @param firstCommunity Community Id
     * @param secondCommunity Community Id
     * @param firstMemberId Member Id
     * @param secondMemberId Member Id
     * @param communitiesStubs All current working communities
     * @param membersStubs All current working members
     * @param communityAdjacencies All community overlaps
     * @param hGenerated Generated overlap fct
     * @param changedPositions Which communities have swapped members
     * @param logger Logging Object
     */
    private static void updateOverlaps(Community firstCommunity, Community secondCommunity,
                                       int firstMemberId, int secondMemberId,
                                       HashMap<Integer, Community> communitiesStubs,
                                       HashMap<Integer, Member> membersStubs,
                                       HashMap<Integer, HashMap<Integer, CommunityAdjacency>> communityAdjacencies,
                                       int[][] hGenerated, Set<ChangedPosition> changedPositions, Logger logger) {

        Set<Integer> affectedCommunities = membersStubs.get(firstMemberId).getCommunities();
        affectedCommunities.addAll(membersStubs.get(secondMemberId).getCommunities());

        //  logger.log("Number of affected communities: " + affectedCommunities.size(), LogLevel.DEBUG);

        for (Integer otherCommunityId : affectedCommunities/*communitiesStubs.keySet()*/) {
            Community otherCommunity = communitiesStubs.get(otherCommunityId);

            if (otherCommunity.getId() == firstCommunity.getId() || otherCommunity.getId() == secondCommunity.getId()) {
                continue;
            }

            // I. otherCommunityId \in Adj(firstCommunity) \land \notin Adj(secondCommunity)
            if (communityAdjacencies.get(firstCommunity.getId()).containsKey(otherCommunityId)
                    && !communityAdjacencies.get(secondCommunity.getId()).containsKey(otherCommunityId)) {
                int currentFirstOverlapSize = CommunityHelper.getIntersectionSize(firstCommunity.getMembers(), otherCommunity.getMembers());
                int oldFirstOverlapSize = 0;
                if (communityAdjacencies.get(firstCommunity.getId()).containsKey(otherCommunity.getId())) {
                    oldFirstOverlapSize = communityAdjacencies.get(firstCommunity.getId()).get(otherCommunity.getId()).getOverlapSize();
                }

                // overlap for first community and other community changed => update it
                if (currentFirstOverlapSize != oldFirstOverlapSize) {
                    CommunityHelper.updateCommunityAdjacencies(communityAdjacencies, firstCommunity.getId(),
                            otherCommunityId, currentFirstOverlapSize);

                    // decrease h function for old overlap value
                    changeOverlapFunction(firstCommunity.getNumberOfMembers(), otherCommunity.getNumberOfMembers(),
                            oldFirstOverlapSize, hGenerated, changedPositions, FunctionChange.DECREASE, "I", logger);

                    if (currentFirstOverlapSize != 0) {
                        // increase h function for new overlap value
                        changeOverlapFunction(firstCommunity.getNumberOfMembers(), otherCommunity.getNumberOfMembers(),
                                currentFirstOverlapSize, hGenerated, changedPositions, FunctionChange.INCREASE, "I", logger);
                    }
                }

                int currentSecondOverlapSize = CommunityHelper.getIntersectionSize(secondCommunity.getMembers(),
                        otherCommunity.getMembers());
                // overlap of first community did not change and second community has no overlap with other community => skip
                if (currentFirstOverlapSize == oldFirstOverlapSize && currentSecondOverlapSize == 0) {
                    continue;
                }

                // overlap for second community and other community is not empty => update it
                CommunityHelper.updateCommunityAdjacencies(communityAdjacencies, secondCommunity.getId(),
                        otherCommunityId, currentSecondOverlapSize);

                // increase h function for new overlap value
                changeOverlapFunction(secondCommunity.getNumberOfMembers(), otherCommunity.getNumberOfMembers(),
                        currentSecondOverlapSize, hGenerated, changedPositions, FunctionChange.INCREASE, "I", logger);
            } else if (!communityAdjacencies.get(firstCommunity.getId()).containsKey(otherCommunityId)
                    && communityAdjacencies.get(secondCommunity.getId()).containsKey(otherCommunityId)) {
                // II. otherCommunityId \notin Adj(firstCommunity) \land \in Adj(secondCommunity)
                int currentSecondOverlapSize = CommunityHelper.getIntersectionSize(secondCommunity.getMembers(), otherCommunity.getMembers());
                int oldSecondOverlapSize = 0;
                if (communityAdjacencies.get(secondCommunity.getId()).containsKey(otherCommunity.getId())) {
                    oldSecondOverlapSize = communityAdjacencies.get(secondCommunity.getId()).get(otherCommunity.getId()).getOverlapSize();
                }

                // overlap for second community and other community changed => update it
                if (currentSecondOverlapSize != oldSecondOverlapSize) {
                    CommunityHelper.updateCommunityAdjacencies(communityAdjacencies, secondCommunity.getId(),
                            otherCommunityId, currentSecondOverlapSize);

                    // decrease h function for old overlap value
                    changeOverlapFunction(secondCommunity.getNumberOfMembers(), otherCommunity.getNumberOfMembers(),
                            oldSecondOverlapSize, hGenerated, changedPositions, FunctionChange.DECREASE, "II", logger);

                    if (currentSecondOverlapSize != 0) {
                        // increase h function for new overlap value
                        changeOverlapFunction(secondCommunity.getNumberOfMembers(), otherCommunity.getNumberOfMembers(),
                                currentSecondOverlapSize, hGenerated, changedPositions, FunctionChange.INCREASE, "II", logger);
                    }
                }

                int currentFirstOverlapSize = CommunityHelper.getIntersectionSize(firstCommunity.getMembers(),
                        otherCommunity.getMembers());
                // overlap of first community did not change and second community has no overlap with other community => skip
                if (currentSecondOverlapSize == oldSecondOverlapSize && currentFirstOverlapSize == 0) {
                    continue;
                }

                // overlap for second community and other community is not empty => update it
                CommunityHelper.updateCommunityAdjacencies(communityAdjacencies, firstCommunity.getId(),
                        otherCommunityId, currentFirstOverlapSize);

                // increase h function for new overlap value
                changeOverlapFunction(firstCommunity.getNumberOfMembers(), otherCommunity.getNumberOfMembers(),
                        currentFirstOverlapSize, hGenerated, changedPositions, FunctionChange.INCREASE, "II", logger);
            } else if (communityAdjacencies.get(firstCommunity.getId()).containsKey(otherCommunityId)
                    && communityAdjacencies.get(secondCommunity.getId()).containsKey(otherCommunityId)) {
                // III. otherCommunityId \in Adj(secondCommunity) \land \in Adj(firstCommunity)

                int currentFirstOverlapSize = CommunityHelper.getIntersectionSize(firstCommunity.getMembers(), otherCommunity.getMembers());
                int currentSecondOverlapSize = CommunityHelper.getIntersectionSize(secondCommunity.getMembers(), otherCommunity.getMembers());

                int oldFirstOverlapSize = 0;
                if (communityAdjacencies.get(firstCommunity.getId()).containsKey(otherCommunity.getId())) {
                    oldFirstOverlapSize = communityAdjacencies.get(firstCommunity.getId()).get(otherCommunity.getId()).getOverlapSize();
                }
                int oldSecondOverlapSize = 0;
                if (communityAdjacencies.get(secondCommunity.getId()).containsKey(otherCommunity.getId())) {
                    oldSecondOverlapSize = communityAdjacencies.get(secondCommunity.getId()).get(otherCommunity.getId()).getOverlapSize();
                }

                if (currentFirstOverlapSize == oldFirstOverlapSize && currentSecondOverlapSize == oldSecondOverlapSize) {
                    // nothing to do here, overlaps did not change for both communities
                    continue;
                }

                // overlap for first community and other community changed => update it
                if (currentFirstOverlapSize != oldFirstOverlapSize) {
                    CommunityHelper.updateCommunityAdjacencies(communityAdjacencies, firstCommunity.getId(),
                            otherCommunityId, currentFirstOverlapSize);

                    // decrease h function for old overlap value
                    changeOverlapFunction(firstCommunity.getNumberOfMembers(), otherCommunity.getNumberOfMembers(),
                            oldFirstOverlapSize, hGenerated, changedPositions, FunctionChange.DECREASE, "III", logger);

                    if (currentFirstOverlapSize != 0) {
                        // increase h function for new overlap value
                        changeOverlapFunction(firstCommunity.getNumberOfMembers(), otherCommunity.getNumberOfMembers(),
                                currentFirstOverlapSize, hGenerated, changedPositions, FunctionChange.INCREASE, "III", logger);
                    }
                }

                // overlap for second community and other community changed => update it
                if (currentSecondOverlapSize != oldSecondOverlapSize) {
                    CommunityHelper.updateCommunityAdjacencies(communityAdjacencies, secondCommunity.getId(),
                            otherCommunityId, currentSecondOverlapSize);

                    // decrease h function for old overlap value
                    changeOverlapFunction(secondCommunity.getNumberOfMembers(), otherCommunity.getNumberOfMembers(),
                            oldSecondOverlapSize, hGenerated, changedPositions, FunctionChange.DECREASE, "III", logger);

                    if (currentSecondOverlapSize != 0) {
                        // increase h function for new overlap value
                        changeOverlapFunction(secondCommunity.getNumberOfMembers(), otherCommunity.getNumberOfMembers(),
                                currentSecondOverlapSize, hGenerated, changedPositions, FunctionChange.INCREASE, "III", logger);
                    }
                }
            } else {
                // IV. otherCommunityId \notin Adj(secondCommunity) \land \notin Adj(firstCommunity)
                int currentFirstOverlapSize = CommunityHelper.getIntersectionSize(firstCommunity.getMembers(), otherCommunity.getMembers());
                int currentSecondOverlapSize = CommunityHelper.getIntersectionSize(secondCommunity.getMembers(), otherCommunity.getMembers());

                if (currentFirstOverlapSize != 0) {
                    CommunityHelper.updateCommunityAdjacencies(communityAdjacencies, firstCommunity.getId(),
                            otherCommunityId, currentFirstOverlapSize);

                    // increase h function for new overlap value
                    changeOverlapFunction(firstCommunity.getNumberOfMembers(), otherCommunity.getNumberOfMembers(),
                            currentFirstOverlapSize, hGenerated, changedPositions, FunctionChange.INCREASE, "IV", logger);
                }

                if (currentSecondOverlapSize != 0) {
                    CommunityHelper.updateCommunityAdjacencies(communityAdjacencies, secondCommunity.getId(),
                            otherCommunityId, currentSecondOverlapSize);

                    // increase h function for new overlap value
                    changeOverlapFunction(secondCommunity.getNumberOfMembers(), otherCommunity.getNumberOfMembers(),
                            currentSecondOverlapSize, hGenerated, changedPositions, FunctionChange.INCREASE, "IV", logger);
                }
            }
        }
    }

    /**
     * Checks if newHGenerated is more accurate to targetH than originalHGenerated
     * @param originalHGenerated Function before current swaps
     * @param newHGenerated Function after current swaps
     * @param targetH The function we want to approximate
     * @param changedPositions All communities that are affected by swaps
     * @param improvementFactor By how much more there should be improvements
     * @return True if there are more improvements than deteriorations in absolute
     * errors when comparing overlap functions
     */
    private static boolean isImprovement(int[][] originalHGenerated, int[][] newHGenerated,
                                         int[][] targetH, Set<ChangedPosition> changedPositions,
                                         double improvementFactor) {
        int improved = 0;
        int worsened = 0;

        for (ChangedPosition position : changedPositions) {
            int i = position.getCommunitySize();
            int j = position.getOverlapSize();
            int originalDifference = Math.abs(originalHGenerated[i][j] - ((targetH.length > i && targetH[0].length > j) ? targetH[i][j] : 0));
            int newDifference = Math.abs(newHGenerated[i][j] - ((targetH.length > i && targetH[0].length > j) ? targetH[i][j] : 0));

            if (newDifference < originalDifference) {
                improved++;
            } else if (newDifference > originalDifference) {
                worsened++;
            }
        }

        // if (worsened > improved) {
        //     System.out.println("Worsened larger: " + worsened + " > " + improved);
        // }

        return improved > (worsened * improvementFactor);
    }

    private static void changeOverlapFunction(int firstCommunitySize, int secondCommunitySize, int overlapSize,
                                              int[][] h, Set<ChangedPosition> changedPositions, FunctionChange change,
                                              String overlapCase, Logger logger) {
        switch (change) {
            case INCREASE:
                h[firstCommunitySize][overlapSize] += 1;
                h[secondCommunitySize][overlapSize] += 1;
                changedPositions.add(new ChangedPosition(firstCommunitySize, overlapSize));
                changedPositions.add(new ChangedPosition(secondCommunitySize, overlapSize));
                break;
            case DECREASE:
                h[firstCommunitySize][overlapSize] -= 1;
                h[secondCommunitySize][overlapSize] -= 1;
                changedPositions.add(new ChangedPosition(firstCommunitySize, overlapSize));
                changedPositions.add(new ChangedPosition(secondCommunitySize, overlapSize));

                // if (h[firstCommunitySize][overlapSize] == 0 || h[secondCommunitySize][overlapSize] == 0) {
                //     System.out.println("Zero state reached in case " + overlapCase + " !");
                //     System.out.println("Overlap Value 1: " + firstCommunitySize + "; " + overlapSize + " => " + h[firstCommunitySize][overlapSize]);
                //     System.out.println("Overlap Value 2: " + secondCommunitySize + "; " + overlapSize + " => " + h[secondCommunitySize][overlapSize]);

                //     break;
                // }

                if (h[firstCommunitySize][overlapSize] < 0 || h[secondCommunitySize][overlapSize] < 0) {
                    logger.log("Invalid state reached in case " + overlapCase + " !", LogLevel.ERROR);
                    logger.log("Overlap Value 1: " + firstCommunitySize + "; " + overlapSize + " => " +
                            h[firstCommunitySize][overlapSize], LogLevel.ERROR);
                    logger.log("Overlap Value 2: " + secondCommunitySize + "; " + overlapSize + " => " +
                            h[secondCommunitySize][overlapSize], LogLevel.ERROR);
                    break;
                }
                break;
            default:
                logger.log("Invalid function change mode provided!", LogLevel.ERROR, change);
        }
    }

    /**
     *
     * @param firstMemberId Member Id
     * @param secondMemberId Member Id
     * @param membersStubs Current members
     * @return All communities members are a part of
     */
    private static Set<Integer> identifyAffectedCommunities(int firstMemberId, int secondMemberId,
                                                            HashMap<Integer, Member> membersStubs) {
        Set<Integer> affectedCommunities = new HashSet<>(membersStubs.get(firstMemberId).getCommunities());
        affectedCommunities.addAll(membersStubs.get(secondMemberId).getCommunities());
        return affectedCommunities;
    }

    /**
     *
     * @param communityAdjacencies All community adjacencies
     * @param affectedCommunities Communities affected by a swap
     * @return the adjacencies of the communities that will be affected by the swap
     */
    private static HashMap<Integer, HashMap<Integer, CommunityAdjacency>> backupCommunityAdjacencies(
            HashMap<Integer, HashMap<Integer, CommunityAdjacency>> communityAdjacencies, Set<Integer> affectedCommunities) {
        HashMap<Integer, HashMap<Integer, CommunityAdjacency>> backup = new HashMap<>();
        for (Integer communityId : affectedCommunities) {
            if (communityAdjacencies.containsKey(communityId)) {
                HashMap<Integer, CommunityAdjacency> innerMap = new HashMap<>();
                for (Map.Entry<Integer, CommunityAdjacency> entry : communityAdjacencies.get(communityId).entrySet()) {
                    CommunityAdjacency adjacency = entry.getValue();
                    innerMap.put(entry.getKey(), new CommunityAdjacency(adjacency.getId(), adjacency.getOverlapSize()));
                }
                backup.put(communityId, innerMap);
            }
        }
        return backup;
    }

    private static int[][] copyMatrix(int[][] matrix) {
        int[][] copy = new int[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            copy[i] = Arrays.copyOf(matrix[i], matrix[i].length);
        }
        return copy;
    }

    private static HashMap<Integer, HashMap<Integer, CommunityAdjacency>> copyCommunityAdjacencies(
            HashMap<Integer, HashMap<Integer, CommunityAdjacency>> communityAdjacencies) {
        HashMap<Integer, HashMap<Integer, CommunityAdjacency>> copy = new HashMap<>();
        for (Map.Entry<Integer, HashMap<Integer, CommunityAdjacency>> entry : communityAdjacencies.entrySet()) {
            HashMap<Integer, CommunityAdjacency> deepCopy = new HashMap<>();
            for (Map.Entry<Integer, CommunityAdjacency> deepEntry : entry.getValue().entrySet()) {
                CommunityAdjacency communityAdjacency = new CommunityAdjacency(deepEntry.getValue().getId(), deepEntry.getValue().getOverlapSize());
                deepCopy.put(deepEntry.getKey(), communityAdjacency);
            }
            copy.put(entry.getKey(), deepCopy);
        }
        return copy;
    }

    private static double calculateQuadraticError(int[][] h, int[][] hGenerated) {
        double error = 0.0;
        for (int i = 0; i < hGenerated.length; i++) {
            for (int j = 0; j < hGenerated[0].length; j++) {
                error += Math.pow(((h.length > i && h[0].length > j) ? h[i][j] : 0) - hGenerated[i][j], 2);
            }
        }
        return error;
    }

    private static void cleanOldTimestamps(List<Date> timestamps) {
        Date oneHourAgo = new Date(System.currentTimeMillis() - 3600000); // 3600000 milliseconds = 1 hour
        timestamps.removeIf(timestamp -> timestamp.before(oneHourAgo));
    }
}
