package edu.plus.cs;


import edu.plus.cs.io.*;
import edu.plus.cs.model.Community;
import edu.plus.cs.model.CommunityAdjacency;
import edu.plus.cs.model.Member;
import edu.plus.cs.util.Mode;

import java.util.*;

public class Main {

    public static final String MACH2_DIR_PREFIX = "/home/d3000/d300342/mach2-home/mpicomm/scripts/test/";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Invalid mode provided!");
            return;
        }

        Mode mode = Mode.valueOf(args[0].toUpperCase());
        boolean onMach2;
        switch (mode) {
            case RANDOM_MATCHING:
                if (args.length < 4) {
                    System.err.println("Invalid number of arguments for mode 'random_matching'!");
                    System.err.println("Use: random_matching <communitiesFile> <overlapFunctionFile> <randomMatchingFactor> (<onMach2>)");
                    return;
                }

                String communitiesFile = args[1];
                String overlapFunctionFile = args[2];
                double randomMatchingFactor = Double.parseDouble(args[3]);

                onMach2 = false;
                if (args.length > 4) {
                    onMach2 = Boolean.parseBoolean(args[4]);
                }

                prepareAndExecuteRandomMatching(communitiesFile, overlapFunctionFile, randomMatchingFactor, onMach2);

                break;
            case DRAW_EDGES:
                if (args.length < 4) {
                    System.err.println("Invalid number of arguments for mode 'draw_edges'!");
                    System.err.println("Use: draw_edges <matchedCommunitiesFile> <targetNumberOfEdges> <threshold> (<onMach2>)");
                    return;
                }

                String matchedCommunitiesFile = args[1];
                int targetNumberOfEdges = Integer.parseInt(args[2]);
                double threshold = Double.parseDouble(args[3]);

                onMach2 = false;
                if (args.length > 4) {
                    onMach2 = Boolean.parseBoolean(args[4]);
                }

                drawEdges(matchedCommunitiesFile, targetNumberOfEdges, threshold, onMach2);

                break;
        }
    }

    private static void drawEdges(String matchedCommunitiesFile, int targetNumberOfEdges, double threshold,
                                  boolean onMach2) {
        System.out.println("Starting drawing edges ...");

        if (onMach2) {
            matchedCommunitiesFile = MACH2_DIR_PREFIX + matchedCommunitiesFile;
        }

        HashMap<Integer, Community> matchedCommunities = CommunityReader.readCommunitiesFromFile(matchedCommunitiesFile);

        if (matchedCommunities == null) {
            System.err.println("Could not read matched communities!");
            return;
        }

        // first we create adjacencyLists for all member/vertices in our graph
        HashMap<Integer, List<Integer>> adjacencyLists = getEmptyAdjacencyLists(matchedCommunities);

        // we start with an initial edge probability of 0.5 and do a binary search inspired approach to hit the
        // target of drawn edges inside our graph
        double p = 0.5;
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
                            if (!adjacencyLists.get(memberId).contains(otherMemberId)) {
                                adjacencyLists.get(memberId).add(otherMemberId);
                            }

                            if (!adjacencyLists.get(otherMemberId).contains(memberId)) {
                                adjacencyLists.get(otherMemberId).add(memberId);
                            }
                        }
                    }
                }
            }

            currentNumberOfEdges = getNumberOfEdges(adjacencyLists);

            if (currentNumberOfEdges < (targetNumberOfEdges * threshold)) {
                p = p * 1.5;

                adjacencyLists = getEmptyAdjacencyLists(matchedCommunities);
            } else if (currentNumberOfEdges > (targetNumberOfEdges + (targetNumberOfEdges * (1 - threshold)))) {
                p = p / 2.0;

                adjacencyLists = getEmptyAdjacencyLists(matchedCommunities);
            } else {
                thresholdReached = true;
            }
        }

        GraphWriter.writeGraphToFile(adjacencyLists, currentNumberOfEdges, onMach2);

        System.out.println("Reached threshold of " + threshold + " with " + currentNumberOfEdges + " edges");
    }

    private static HashMap<Integer, List<Integer>> getEmptyAdjacencyLists(HashMap<Integer, Community> matchedCommunities) {
        List<Integer> matchedCommunityIds = new ArrayList<>(matchedCommunities.keySet());

        HashMap<Integer, List<Integer>> adjacencyLists = new HashMap<>();
        for (int matchedCommunityId : matchedCommunityIds) {
            Community currentCommunity = matchedCommunities.get(matchedCommunityId);
            for (int memberId : currentCommunity.getMembers()) {
                adjacencyLists.put(memberId, new LinkedList<>());
            }
        }

        return adjacencyLists;
    }

    private static int getNumberOfEdges(HashMap<Integer, List<Integer>> adjacencyLists) {
        int numberOfEdgeEntries = 0;

        List<Integer> verticesIds = new ArrayList<>(adjacencyLists.keySet());

        for (int vertexId : verticesIds) {
            numberOfEdgeEntries += adjacencyLists.get(vertexId).size();
        }

        return numberOfEdgeEntries / 2;
    }

    private static void prepareAndExecuteRandomMatching(String communitiesFile, String overlapFunctionFile,
                                                        double randomMatchingFactor, boolean onMach2) {
        if (onMach2) {
            communitiesFile = MACH2_DIR_PREFIX + communitiesFile;
            overlapFunctionFile = MACH2_DIR_PREFIX + overlapFunctionFile;
        }

        HashMap<Integer, Community> communities = CommunityReader.readCommunitiesFromFile(communitiesFile);
        HashMap<Integer, Member> members = new HashMap<>();

        for (Community community : communities.values()) {
            for (Integer memberId : community.getMembers()) {
                if (!members.containsKey(memberId)) {
                    members.put(memberId, new Member(memberId, new HashSet<>()));
                }
                members.get(memberId).getCommunities().add(community.getId());
            }
        }

        OverlapFunctionReader overlapFunctionReader = new OverlapFunctionReader(communities);
        int[][] h = overlapFunctionReader.readOverlapFunctionFromFile(overlapFunctionFile);

        // System.out.println(h[6][5]);

        // create "empty" objects for model generation
        int[][] hGenerated = new int[h.length][h[0].length];
        HashMap<Integer, Community> communitiesStubs = generateCommunityStubs(communities);
        HashMap<Integer, Member> membersStubs = generateMemberStubs(members);
        HashMap<Integer, HashMap<Integer, CommunityAdjacency>> communityAdjacencies = generateCommunityAdjacencies(communities);

        // check if number of member and community stubs are equivalent
        int numberOfTotalStubs = members.values().stream().map(member -> member.getCommunities().size()).reduce(0, Integer::sum);

        System.out.println("Generated initial data structures, starting random matching with factor: " + randomMatchingFactor + " ...");

        System.out.println("Current timestamp: " + System.currentTimeMillis());
        randomMatching(communities, communitiesStubs, members, membersStubs, h, hGenerated, communityAdjacencies, numberOfTotalStubs,
                randomMatchingFactor);

        System.out.println("Current timestamp: " + System.currentTimeMillis());

        CommunityWriter.writeCommunitiesToFile(communitiesStubs, membersStubs, onMach2);

        communitiesStubs = null;
        membersStubs = null;

        FunctionOutputWriter.writeToOutputFile(hGenerated, onMach2);

        System.out.println("Finished with randomly connecting the model!");
    }

    private static void randomMatching(HashMap<Integer, Community> communities, HashMap<Integer, Community> communitiesStubs,
                                       HashMap<Integer, Member> members, HashMap<Integer, Member> membersStubs,
                                       int[][] h, int[][] hGenerated,
                                       HashMap<Integer, HashMap<Integer, CommunityAdjacency>> communityAdjacencies,
                                       int targetNumberOfConnections, double randomMatchingFactor) {
        Random random = new Random();
        // List<Integer> eligibleMembersKeys = new ArrayList<>(members.keySet());
        // List<Integer> eligibleCommunitiesKeys = new ArrayList<>(communities.keySet());

        List<Integer> fairlyDistributedMembersKeys = new ArrayList<>();
        for (Integer memberKey : members.keySet()) {
            for (int i = 0; i < members.get(memberKey).getCommunities().size(); i++) {
                fairlyDistributedMembersKeys.add(memberKey);
            }
        }

        List<Integer> fairlyDistributedCommunitiesKeys = new ArrayList<>();
        for (Integer communityKey : communities.keySet()) {
            for (int i = 0; i < communities.get(communityKey).getMembers().size(); i++) {
                fairlyDistributedCommunitiesKeys.add(communityKey);
            }
        }

        int drawnConnections = 0;
        while (true) {
            int randomMemberIndex = random.nextInt(fairlyDistributedMembersKeys.size());
            int randomMemberId = fairlyDistributedMembersKeys.get(randomMemberIndex);
            if (members.get(randomMemberId).getCommunities().size() == membersStubs.get(randomMemberId).getCommunities().size()) {
                // here we add a constant factor since it can happen, that due to unlucky matching we do not hit the exact
                // same amount of connected stubs
                if (membersStubs.values().stream().map(memberStub -> memberStub.getCommunities().size()).reduce(0, Integer::sum) * randomMatchingFactor
                        >= targetNumberOfConnections) {
                    break;
                }
                continue;
            }

            int randomCommunityId;
            while (true) {
                int randomCommunityIndex = random.nextInt(fairlyDistributedCommunitiesKeys.size());
                randomCommunityId = fairlyDistributedCommunitiesKeys.get(randomCommunityIndex);

                // is the chosen member already in the chosen community? yes => skip
                if (communitiesStubs.get(randomCommunityId).getMembers().contains(randomMemberId)) {
                    continue;
                }

                addMemberAndUpdateAdjacencies(membersStubs.get(randomMemberId), communitiesStubs.get(randomCommunityId),
                        communitiesStubs, communityAdjacencies, hGenerated);

                drawnConnections += 1;

                fairlyDistributedMembersKeys.remove(randomMemberIndex); // we do not want to check this member stub again
                fairlyDistributedCommunitiesKeys.remove(randomCommunityIndex); // we do not want to check this community stub again

                break;
            }

            if (fairlyDistributedMembersKeys.isEmpty() || fairlyDistributedCommunitiesKeys.isEmpty()) {
                break;
            }

            if (drawnConnections % 100000 == 0) {
                System.out.println("Another 100k connections drawn, now at: " + drawnConnections);
                // System.out.println("Number of all members over all communities: " + communitiesStubs.values().stream().map(community -> community.getMembers().size()).reduce(0, Integer::sum));
            }
        }

        // calculate hGenerated
        for (Integer communityStubId : communitiesStubs.keySet()) {
            communityAdjacencies.get(communityStubId).values().stream().forEach(communityAdjacency -> {
                hGenerated[communitiesStubs.get(communityAdjacency.getId()).getNumberOfMembers()][communityAdjacency.getOverlapSize()] += 1;
            });
        }
    }

    private static void addMemberAndUpdateAdjacencies(
            Member member,
            Community newCommunity,
            HashMap<Integer, Community> communitiesStubs,
            HashMap<Integer, HashMap<Integer, CommunityAdjacency>> communityAdjacencies,
            int[][] hGenerated) {
        // update the new community's member list
        newCommunity.getMembers().add(member.getId());
        // iterate over all communities in which the new member is included
        for (Integer communityId : member.getCommunities()) {
            if (communityId.equals(newCommunity.getId())) {
                continue; // Skip the iteration if the newCommunity is the same as the existingCommunity
            }

            Community existingCommunity = communitiesStubs.get(communityId);
            // compute new overlap size and get the previous one
            int newOverlapSize = getIntersectionSize(newCommunity.getMembers(), existingCommunity.getMembers());
            /*int previousOverlapSize = getOverlapSize(communityAdjacencies, newCommunity.getId(), existingCommunity.getId());

            // update hGenerated when there already existed an overlap
            if (previousOverlapSize > 0) {
                hGenerated[existingCommunity.getNumberOfMembers()][previousOverlapSize] -= 1;
                hGenerated[newCommunity.getNumberOfMembers() - 1][previousOverlapSize] -= 1;
            }
            // compute new overlap values for drawn connection
            hGenerated[existingCommunity.getNumberOfMembers()][newOverlapSize] += 1;
            hGenerated[newCommunity.getNumberOfMembers()][newOverlapSize] += 1;*/

            // update the adjacency information
            updateCommunityAdjacencies(communityAdjacencies, newCommunity.getId(), existingCommunity.getId(), newOverlapSize);
        }
        // add the new community to the member's list of communities
        member.getCommunities().add(newCommunity.getId());
    }

    private static int getOverlapSize(
            HashMap<Integer, HashMap<Integer, CommunityAdjacency>> communityAdjacencies,
            int communityId1, int communityId2) {
        // this assumes that the symmetry of the overlap entries is established
        if (communityAdjacencies.get(communityId1).containsKey(communityId2)) {
            return communityAdjacencies.get(communityId1).get(communityId2).getOverlapSize();
        } else {
            // no overlap recorded yet
            return 0;
        }
        /*return communityAdjacencies.getOrDefault(communityId1, new HashMap<>())
                .getOrDefault(communityId2, new CommunityAdjacency(communityId2, null, 0))
                .getOverlapSize();*/
    }


    private static void updateCommunityAdjacencies(
            HashMap<Integer, HashMap<Integer, CommunityAdjacency>> communityAdjacencies,
            int communityId1, int communityId2, int newOverlapSize) {

        if (communityAdjacencies.get(communityId1).containsKey(communityId2)) {
            // overlap already recorded
            communityAdjacencies.get(communityId1).get(communityId2).setOverlapSize(newOverlapSize);
        } else {
            communityAdjacencies.get(communityId1).put(communityId2, new CommunityAdjacency(communityId2, newOverlapSize));
        }

        // maintain symmetry
        if (communityAdjacencies.get(communityId2).containsKey(communityId1)) {
            // overlap already recorded
            communityAdjacencies.get(communityId2).get(communityId1).setOverlapSize(newOverlapSize);
        } else {
            communityAdjacencies.get(communityId2).put(communityId1, new CommunityAdjacency(communityId1, newOverlapSize));
        }

        /*communityAdjacencies.computeIfAbsent(communityId1, k -> new HashMap<>())
                .computeIfAbsent(communityId2, k -> new CommunityAdjacency(communityId2, null, 0))
                .setOverlapSize(newOverlapSize);

        communityAdjacencies.computeIfAbsent(communityId2, k -> new HashMap<>())
                .computeIfAbsent(communityId1, k -> new CommunityAdjacency(communityId1, null, 0))
                .setOverlapSize(newOverlapSize);*/
    }


    public static <T> int getIntersectionSize(Set<T> members1, Set<T> members2) {
        Set<T> copy = new HashSet<>(members1); // create copy of members1
        copy.retainAll(members2); // compute intersection of members1 and members2
        return copy.size();
    }

    private static boolean isWithinThreshold(int[][] h, int[][] hGenerated, Community community1, Community community2,
                                             int overlapSize, double thresholdMultiplier) {
        return (hGenerated[community1.getNumberOfMembers()][overlapSize]
                < Math.floor(h[community1.getNumberOfMembers()][overlapSize] * thresholdMultiplier))
                || (hGenerated[community2.getNumberOfMembers()][overlapSize]
                < Math.floor(h[community2.getNumberOfMembers()][overlapSize] * thresholdMultiplier));
    }

    public static HashMap<Integer, HashMap<Integer, CommunityAdjacency>> generateCommunityAdjacencies(HashMap<Integer, Community> communities) {
        HashMap<Integer, HashMap<Integer, CommunityAdjacency>> communityAdjacencies = new HashMap<>();
        for (Integer communityId : communities.keySet()) {
            communityAdjacencies.put(communityId, new HashMap<>());
        }
        return communityAdjacencies;
    }


    public static HashMap<Integer, Community> generateCommunityStubs(HashMap<Integer, Community> communities) {
        HashMap<Integer, Community> communityStubs = new HashMap<>();
        for (Integer communityId : communities.keySet()) {
            Community originalCommunity = communities.get(communityId);
            // create stub
            Community stub = new Community(originalCommunity.getId(), new HashSet<>());
            communityStubs.put(communityId, stub);
        }
        return communityStubs;
    }

    public static HashMap<Integer, Member> generateMemberStubs(HashMap<Integer, Member> members) {
        HashMap<Integer, Member> memberStubs = new HashMap<>();
        for (Integer memberId : members.keySet()) {
            Member originalMember = members.get(memberId);
            // create stub
            Member stub = new Member(originalMember.getId(), new HashSet<>());
            memberStubs.put(memberId, stub);
        }
        return memberStubs;
    }

    // draw connection
    // old code after addMemberAndUpdateAdjacencies(...)
                /*communitiesStubs.get(randomCommunityId).getMembers().add(randomMemberId);
                membersStubs.get(randomMemberId).getCommunities().add(randomCommunityId);

                for (Integer membershipCommunityId : membersStubs.get(randomMemberId).getCommunities()) {
                    if (membershipCommunityId == randomCommunityId) {
                        continue;
                    }
                    int overlapSize = intersectionSize(communitiesStubs.get(randomCommunityId).getMembers(),
                            communitiesStubs.get(membershipCommunityId).getMembers());
                    if (overlapSize > 1) {
                        // overlap already existed
                        hGenerated[communitiesStubs.get(randomCommunityId).getNumberOfMembers() - 1][overlapSize - 1] -= 1;
                        hGenerated[communitiesStubs.get(membershipCommunityId).getNumberOfMembers()][overlapSize - 1] -= 1;

                        if (hGenerated[communitiesStubs.get(randomCommunityId).getNumberOfMembers() - 1][overlapSize - 1] < 0 ||
                                hGenerated[communitiesStubs.get(membershipCommunityId).getNumberOfMembers()][overlapSize - 1] < 0) {
                            System.err.println("Value below 0!");
                        }
                    }

                    hGenerated[communitiesStubs.get(randomCommunityId).getNumberOfMembers()][overlapSize] += 1;
                    hGenerated[communitiesStubs.get(membershipCommunityId).getNumberOfMembers()][overlapSize] += 1;
                }*/
    // old code before drawnConnections += 1 ...
}