package edu.plus.cs;


import edu.plus.cs.io.CommunityReader;
import edu.plus.cs.io.FunctionOutputWriter;
import edu.plus.cs.io.OverlapFunctionReader;
import edu.plus.cs.model.Community;
import edu.plus.cs.model.CommunityAdjacency;
import edu.plus.cs.model.Member;

import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static final String MACH2_DIR_PREFIX = "/home/d3000/d300342/mach2-home/mpicomm/scripts/test/";

    public static void main(String[] args) {
        String communitiesFile = args[0];
        String overlapFunctionFile = args[1];

        boolean onMach2 = false;
        if (args.length > 2) {
            onMach2 = Boolean.parseBoolean(args[2]);
        }

        if (onMach2) {
            communitiesFile = MACH2_DIR_PREFIX + communitiesFile;
            overlapFunctionFile = MACH2_DIR_PREFIX + overlapFunctionFile;
        }

        CommunityReader communityReader = new CommunityReader();
        HashMap<Integer, Community> communities = communityReader
                .readCommunitiesFromFile(communitiesFile);

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

        System.out.println("Generated initial data structures, starting random matching ...");

        System.out.println("Current timestamp: " + System.currentTimeMillis());
        randomMatching(communities, communitiesStubs, members, membersStubs, h, hGenerated, communityAdjacencies, numberOfTotalStubs);
        System.out.println("Current timestamp: " + System.currentTimeMillis());

        communitiesStubs = null;
        membersStubs = null;

        FunctionOutputWriter.writeToOutputFile(hGenerated, onMach2);

        System.out.println("Finished with randomly connecting the model!");
    }

    private static void randomMatching(HashMap<Integer, Community> communities, HashMap<Integer, Community> communitiesStubs,
                                       HashMap<Integer, Member> members, HashMap<Integer, Member> membersStubs,
                                       int[][] h, int[][] hGenerated,
                                       HashMap<Integer, HashMap<Integer, CommunityAdjacency>> communityAdjacencies,
                                       int targetNumberOfConnections) {
        Random random = new Random();
        List<Integer> eligibleMembersKeys = new ArrayList<>(members.keySet());
        List<Integer> eligibleCommunitiesKeys = new ArrayList<>(communities.keySet());
        int drawnConnections = 0;
        while (true) {
            int randomMemberIndex = random.nextInt(eligibleMembersKeys.size());
            int randomMemberId = eligibleMembersKeys.get(randomMemberIndex);
            if (members.get(randomMemberId).getCommunities().size() == membersStubs.get(randomMemberId).getCommunities().size()) {
                eligibleMembersKeys.remove(randomMemberIndex); // we do not want to check this member again
                // here we add a constant factor since it can happen, that due to unlucky matching we do not hit the exact
                // same amount of connected stubs
                if (membersStubs.values().stream().map(memberStub -> memberStub.getCommunities().size()).reduce(0, Integer::sum) * 1.05
                        >= targetNumberOfConnections) {
                    break;
                }
                continue;
            }

            int randomCommunityId;
            while (true) {
                int randomCommunityIndex = random.nextInt(eligibleCommunitiesKeys.size());
                randomCommunityId = eligibleCommunitiesKeys.get(randomCommunityIndex);
                if (communities.get(randomCommunityId).getNumberOfMembers() == communitiesStubs.get(randomCommunityId).getNumberOfMembers()) {
                    eligibleCommunitiesKeys.remove(randomCommunityIndex); // we do not want to check this community again
                    continue;
                }

                // is the chosen member already in the chosen community? yes => skip
                if (communitiesStubs.get(randomCommunityId).getMembers().contains(randomMemberId)) {
                    continue;
                }

                addMemberAndUpdateAdjacencies(membersStubs.get(randomMemberId), communitiesStubs.get(randomCommunityId),
                        communitiesStubs, communityAdjacencies, hGenerated);

                drawnConnections += 1;

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