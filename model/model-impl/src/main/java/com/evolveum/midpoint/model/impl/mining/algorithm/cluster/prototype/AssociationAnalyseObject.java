/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.prototype;

/**
 * This class is part of ongoing research and is intended for research purposes only.
 * It contains experimental functionalities and methods developed for investigative purposes.
 * Please note that the methods and behaviors within this class may not follow conventional
 * implementation standards and should not be used in production environments.
 *
 * <p>This class serves as a platform for testing and exploring novel ideas and algorithms.
 */

// TODO delete after finish research
public class AssociationAnalyseObject {

    int min = 0;
    int max = 0;
    double average = 0;
    int objectCount = 0;
    int minMembers = 0;
    int maxMembers = 0;
    double averageMembers = 0;
    int membersCount = 0;

    public AssociationAnalyseObject() {
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public double getAverage() {
        return average;
    }

    public void setAverage(double average) {
        this.average = average;
    }

    public void resolveMinMaxAverage(int processedObjectsCount, int membersForAdd) {

        if (membersForAdd != 0) {
            if (minMembers == 0) {
                minMembers = membersForAdd;
            } else if (membersForAdd < minMembers) {
                this.minMembers = membersForAdd;
            }

            if (membersForAdd > maxMembers) {
                this.maxMembers = membersForAdd;
            }

            this.membersCount = membersCount + membersForAdd;
            int oldMembersCount = membersCount - membersForAdd;
            this.averageMembers = calculateMembersAverage(membersCount, averageMembers, membersForAdd, oldMembersCount);
        }

        if (processedObjectsCount == 0) {
            return;
        }

        if (min == 0) {
            this.min = processedObjectsCount;
        } else if (processedObjectsCount < min) {
            this.min = processedObjectsCount;
        }
        if (processedObjectsCount > max) {
            this.max = processedObjectsCount;
        }
        this.objectCount++;
        this.average = calculateObjectAverage(objectCount, average, processedObjectsCount);
    }

    private double calculateObjectAverage(int objectCount, double averageForUpgrade, double newValueForAverage) {
        double sumOfObjects = averageForUpgrade * (objectCount - 1);
        sumOfObjects += newValueForAverage;
        return sumOfObjects / objectCount;
    }

    private double calculateMembersAverage(int membersCount,
            double averageForUpgrade,
            double newValueForAverage,
            int oldMembersCount) {
        double sumOfObjects = averageForUpgrade * oldMembersCount;
        sumOfObjects += newValueForAverage;
        return sumOfObjects / membersCount;

    }
}
