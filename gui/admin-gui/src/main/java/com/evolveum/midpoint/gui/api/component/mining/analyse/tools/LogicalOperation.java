/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools;

public class LogicalOperation {

    public static int conjunction(int value1, int value2) {
        if (value1 == 1 && value2 == 1) {
            return 1;
        }

        return 0;
    }

    public int disjunction(int value1, int value2) {
        if (value1 == 0 && value2 == 0) {
            return 0;
        }

        return 1;
    }

    public static int calculatePointUP(int roleCount, int i, int j, int[][] B, int[][] A) {

        //Cij = V l=1 -> k (Ail ∧ Blj).
        int sum = 0;
        for (int l = 0; l < roleCount; l++) {
            sum = sum + conjunction(A[i][l], B[l][j]);
        }

        if (sum == 0) {
            return 0;
        }
        return 1;
    }

    public static int[][] calculateUP(int roleCount, int userCount, int permissionCount, int[][] B, int[][] A) {

        int[][] matrix = new int[userCount][permissionCount];

        for (int i = 0; i < userCount; i++) {
            for (int j = 0; j < permissionCount; j++) {

                //Cij = V l=1 -> k (Ail ∧ Blj).
                int sum = 0;
                for (int l = 0; l < roleCount; l++) {
                    sum = sum + conjunction(A[i][l], B[l][j]);
                }

                if (sum == 0) {
                    matrix[i][j] = 0;
                } else {
                    matrix[i][j] = 1;
                }
            }
        }

        return matrix;
    }

}
