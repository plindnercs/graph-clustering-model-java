package edu.plus.cs.impl;

import edu.plus.cs.io.CommunityReader;
import edu.plus.cs.io.CommunityWriter;
import edu.plus.cs.io.FunctionOutputWriter;
import edu.plus.cs.io.OverlapFunctionReader;
import edu.plus.cs.model.Community;
import edu.plus.cs.model.CommunityAdjacency;
import edu.plus.cs.model.Member;
import edu.plus.cs.util.CommunityHelper;
import edu.plus.cs.util.Constants;
import edu.plus.cs.util.LogLevel;
import edu.plus.cs.util.Logger;

import java.util.*;

public class RandomMatchingImpl {

    public static void prepareAndExecuteRandomMatching(String communitiesFile, String overlapFunctionFile,
                                                        double randomMatchingFactor, boolean onMach2, Logger logger) {
        if (onMach2) {
            communitiesFile = Constants.MACH2_DIR_PREFIX + communitiesFile;
            overlapFunctionFile = Constants.MACH2_DIR_PREFIX + overlapFunctionFile;
        }

        HashMap<Integer, Community> communities = CommunityReader.readCommunitiesFromFile(communitiesFile, logger);
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
        int[][] h = overlapFunctionReader.readOverlapFunctionFromFile(overlapFunctionFile, logger);

        int maxCommunitySize = 0;
        for (Community community : communities.values()) {
            if (community.getMembers().size() > maxCommunitySize) {
                maxCommunitySize = community.getMembers().size();
            }
        }

        // create "empty" objects for model generation
        int[][] hGenerated = new int[maxCommunitySize + 1][maxCommunitySize + 1];
        HashMap<Integer, Community> communitiesStubs = generateCommunityStubs(communities);
        HashMap<Integer, Member> membersStubs = generateMemberStubs(members);
        HashMap<Integer, HashMap<Integer, CommunityAdjacency>> communityAdjacencies = generateEmptyCommunityAdjacencies(communities);

        // check if number of member and community stubs are equivalent
        int numberOfTotalStubs = members.values().stream().map(member -> member.getCommunities().size()).reduce(0, Integer::sum);

        logger.log("Generated initial data structures, starting random matching with factor: " +
                randomMatchingFactor + " ...", LogLevel.INFO, communitiesFile, overlapFunctionFile);

        randomMatching(communities, communitiesStubs, members, membersStubs, h, hGenerated, communityAdjacencies, numberOfTotalStubs,
                randomMatchingFactor, logger);

        CommunityWriter.writeCommunitiesToFile(communitiesStubs, "before_approx_", onMach2, logger);

        // communitiesStubs = null;
        // membersStubs = null;

        FunctionOutputWriter.writeToOutputFile(hGenerated, "before_approx_", onMach2, logger);

        logger.log("Finished with randomly connecting the model and writing the hGenerated!", LogLevel.INFO);

        logger.log("Starting with swapping connections until the target function is met...", LogLevel.INFO);

        ApproximateOverlapFunctionImpl.approximateOverlapFunction(communitiesStubs, membersStubs, communityAdjacencies,
                h, hGenerated, onMach2, logger);
    }

    private static void randomMatching(HashMap<Integer, Community> communities, HashMap<Integer, Community> communitiesStubs,
                                       HashMap<Integer, Member> members, HashMap<Integer, Member> membersStubs,
                                       int[][] h, int[][] hGenerated,
                                       HashMap<Integer, HashMap<Integer, CommunityAdjacency>> communityAdjacencies,
                                       int targetNumberOfConnections, double randomMatchingFactor, Logger logger) {
        Random random = new Random();

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
                logger.log("Another 100k connections drawn, now at: " + drawnConnections, LogLevel.DEBUG);
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
            int newOverlapSize = CommunityHelper.getIntersectionSize(newCommunity.getMembers(), existingCommunity.getMembers());
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
            CommunityHelper.updateCommunityAdjacencies(communityAdjacencies, newCommunity.getId(), existingCommunity.getId(), newOverlapSize);
        }
        // add the new community to the member's list of communities
        member.getCommunities().add(newCommunity.getId());
    }

    public static HashMap<Integer, HashMap<Integer, CommunityAdjacency>> generateEmptyCommunityAdjacencies(HashMap<Integer, Community> communities) {
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
}
