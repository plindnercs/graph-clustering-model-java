package edu.plus.cs.model;

import java.util.Objects;

public class ChangedPosition {
    private int communitySize;
    private int overlapSize;

    public ChangedPosition(int communitySize, int overlapSize) {
        this.communitySize = communitySize;
        this.overlapSize = overlapSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangedPosition position = (ChangedPosition) o;
        return communitySize == position.communitySize && overlapSize == position.overlapSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(communitySize, overlapSize);
    }
}
