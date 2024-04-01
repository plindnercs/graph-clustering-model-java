package edu.plus.cs.model;

import java.util.HashSet;

public class Member {
    private int id;

    private HashSet<Integer> communities;

    public Member(int id, HashSet<Integer> communities) {
        this.id = id;
        this.communities = communities;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public HashSet<Integer> getCommunities() {
        return communities;
    }

    public void setCommunities(HashSet<Integer> communities) {
        this.communities = communities;
    }
}
