/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util.correlation;

import java.util.stream.IntStream;

public class Levenshtein {

    public double computeLevenshteinSimilarity(String lObject, String rObject, int levenshteinDistance) {
        return 1 - (((double) levenshteinDistance) / (Math.max(lObject.length(), rObject.length())));
    }

    public int computeLevenshteinDistance(String lObject, String rObject) {

        if (lObject.equals(rObject)) {
            return 0;
        }
        if (lObject.isEmpty()) {
            return rObject.length();
        } else if (rObject.isEmpty()) {
            return lObject.length();
        }

        int[][] distance = new int[lObject.length() + 1][rObject.length() + 1];

        IntStream.rangeClosed(0, lObject.length()).forEach(i -> distance[i][0] = i);
        IntStream.rangeClosed(1, rObject.length()).forEach(j -> distance[0][j] = j);

        for (int i = 1; i <= lObject.length(); i++) {
            for (int j = 1; j <= rObject.length(); j++) {
                int match = (lObject.charAt(i - 1) == rObject.charAt(j - 1)) ? 0 : 1;

                distance[i][j] = Math.min(
                        Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1),
                        distance[i - 1][j - 1] + match);
            }
        }
        return distance[lObject.length()][rObject.length()];
    }

}
