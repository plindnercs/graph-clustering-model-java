package edu.plus.cs.model;

import java.util.HashSet;

public class Community {

    private int id;

    private HashSet<Integer> members;

    public Community(int id, HashSet<Integer> members) {
        this.id = id;
        this.members = members;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public HashSet<Integer> getMembers() {
        return members;
    }

    public void setMembers(HashSet<Integer> members) {
        this.members = members;
    }

    public int getNumberOfMembers() {
        return members.size();
    }
}
