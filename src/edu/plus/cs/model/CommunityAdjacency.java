package edu.plus.cs.model;

public class CommunityAdjacency {

    private int id;
    private int overlapSize;

    public CommunityAdjacency(int id, int overlapSize) {
        this.id = id;
        this.overlapSize = overlapSize;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOverlapSize() {
        return overlapSize;
    }

    public void setOverlapSize(int overlapSize) {
        this.overlapSize = overlapSize;
    }
}
