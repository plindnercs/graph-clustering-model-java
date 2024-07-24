package edu.plus.cs.util;

import edu.plus.cs.model.CommunityAdjacency;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CommunityHelper {
    public static void updateCommunityAdjacencies(
            HashMap<Integer, HashMap<Integer, CommunityAdjacency>> communityAdjacencies,
            int communityId1, int communityId2, int newOverlapSize) {

        // setting overlap size to 0 is equivalent to new overlap => delete the reference
        if (newOverlapSize == 0) {
            communityAdjacencies.get(communityId1).remove(communityId2);
            communityAdjacencies.get(communityId2).remove(communityId1);
            return;
        }

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
    }


    public static <T> int getIntersectionSize(Set<T> members1, Set<T> members2) {
        Set<T> copy = new HashSet<>(members1); // create copy of members1
        copy.retainAll(members2); // compute intersection of members1 and members2
        return copy.size();
    }
}
