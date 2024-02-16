package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

public class ClusterExplanation implements Serializable {
    private Set<AttributeMatchExplanation> attributeExplanation;
    private String attributeValue;
    private Double weight;

    public ClusterExplanation() {
    }

    public static String resolveClusterName(Set<ClusterExplanation> clusterExplanationSet) {
        if (clusterExplanationSet == null || clusterExplanationSet.isEmpty()) {
            return null;
        }
        if (clusterExplanationSet.size() == 1) {
            return getCandidateName(clusterExplanationSet.iterator().next().getAttributeExplanation());
        }
        return null;
    }

    public static String getClusterExplanationDescription(Set<ClusterExplanation> clusterExplanationSet) {
        if (clusterExplanationSet == null || clusterExplanationSet.isEmpty()) {
            return "No cluster explanation found.";
        }
        if (clusterExplanationSet.size() == 1) {
            return "There is a single cluster explanation.\n Cluster explanation: "
                    + getExplanation(clusterExplanationSet.iterator().next().getAttributeExplanation());
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("There are multiple cluster explanations. ");

            for (ClusterExplanation explanation : clusterExplanationSet) {
                sb.append("\nCluster explanation: ")
                        .append(getExplanation(explanation.getAttributeExplanation()));
            }
            return sb.toString();
        }
    }

    public static String getExplanation(Set<AttributeMatchExplanation> attributeExplanation) {
        if (attributeExplanation == null) {
            return null;
        }
        if (attributeExplanation.size() == 1) {
            AttributeMatchExplanation explanation = attributeExplanation.iterator().next();
            return "There is a single attribute match :\n Attribute path: "
                    + explanation.getAttributePath() + " with value " + explanation.getAttributeValue() + "\n";
        } else {

            StringBuilder sb = new StringBuilder();
            for (AttributeMatchExplanation attributeMatchExplanation : attributeExplanation) {
                sb.append("Attribute path: ")
                        .append(attributeMatchExplanation.getAttributePath())
                        .append(" with value ")
                        .append(attributeMatchExplanation.getAttributeValue()).append("\n");
            }

            return "There are " + attributeExplanation.size() + " multiple attribute matches: \n" + sb;
        }
    }

    public static String getCandidateName(Set<AttributeMatchExplanation> attributeExplanation) {
        if (attributeExplanation == null) {
            return null;
        }
        if (attributeExplanation.size() == 1) {
            AttributeMatchExplanation explanation = attributeExplanation.iterator().next();

            return explanation.getAttributePath() + "_" + explanation.getAttributeValue();
        }
        return null;
    }

    public ClusterExplanation(Set<AttributeMatchExplanation> attributeExplanation, String attributeValue, Double weight) {
        this.attributeExplanation = attributeExplanation;
        this.attributeValue = attributeValue;
        this.weight = weight;
    }

    public Set<AttributeMatchExplanation> getAttributeExplanation() {
        return attributeExplanation;
    }

    public void setAttributeExplanation(Set<AttributeMatchExplanation> attributeExplanation) {
        this.attributeExplanation = attributeExplanation;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        ClusterExplanation that = (ClusterExplanation) o;
        return Objects.equals(attributeExplanation, that.attributeExplanation) &&
                Objects.equals(attributeValue, that.attributeValue) &&
                Objects.equals(weight, that.weight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeExplanation, attributeValue, weight);
    }
}
