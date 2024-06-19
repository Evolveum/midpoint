package com.evolveum.midpoint.common.mining.utils.values;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class FrequencyItem implements Serializable {

    public static class Neighbour implements Serializable {
        private ObjectReferenceType neighbourRef;

        private double similarity;

        public Neighbour(
                ObjectReferenceType neighbourRef,
                double similarity) {
            this.neighbourRef = neighbourRef;
            this.similarity = similarity;
        }

        public ObjectReferenceType getNeighbourRef() {
            return neighbourRef;
        }

        public void setNeighbourRef(ObjectReferenceType neighbourRef) {
            this.neighbourRef = neighbourRef;
        }

        public double getSimilarity() {
            return similarity;
        }

        public void setSimilarity(double similarity) {
            this.similarity = similarity;
        }
    }

    double frequency;
    private Status status;
    double confidence;
    double zScore;
    List<Neighbour> neighbours = new ArrayList<>();

    public enum Status {
        INCLUDE,
        NEGATIVE_EXCLUDE,
        POSITIVE_EXCLUDE;
    }

    public void setNegativeExclude() {
        this.status = Status.NEGATIVE_EXCLUDE;
    }

    public void setPositiveExclude() {
        this.status = Status.POSITIVE_EXCLUDE;
    }

    public void setInclude() {
        this.status = Status.INCLUDE;
    }

    public FrequencyItem(double frequency) {
        this.frequency = frequency;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public double getzScore() {
        return zScore;
    }

    public void setzScore(double zScore) {
        this.zScore = zScore;
    }

    public List<Neighbour> getNeighbours() {
        return neighbours;
    }

    public void setNeighbours(List<Neighbour> neighbours) {
        this.neighbours = neighbours;
    }

    public void addNeighbour(Neighbour neighbour) {
        neighbours.add(neighbour);
    }

}
