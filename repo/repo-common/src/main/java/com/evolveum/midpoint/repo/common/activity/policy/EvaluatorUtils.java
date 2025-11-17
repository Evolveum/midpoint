/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

public class EvaluatorUtils {

    public enum ThresholdType {
        BELOW, EXCEEDS
    }

    public static String createDefaultMessage(
            String name, String constraintName, String formattedValue, String formattedThreshold, ThresholdType type) {

        String msg = type == ThresholdType.EXCEEDS ?
                "%s is %s, which exceeds the limits of constraint %s (%s)" :
                "%s is %s, which is below the limits of constraint %s (%s)";

        return msg.formatted(name, formattedValue, constraintName, formattedThreshold);
    }

    public static String createDefaultShortMessage(String name, String constraintName, ThresholdType type) {
        String msg = type == ThresholdType.EXCEEDS ?
                "%s exceeded for constraint %s" :
                "%s below the threshold for constraint %s";

        return msg.formatted(name, constraintName);
    }
}
