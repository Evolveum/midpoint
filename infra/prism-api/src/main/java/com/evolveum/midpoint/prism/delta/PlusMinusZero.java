/*
 * Copyright (c) 2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.delta;

/**
 * Simple enumeration that refers to the plus, minus or zero concepts
 * used in delta set triples.
 *
 * @author Radovan Semancik
 *
 */
public enum PlusMinusZero {

    PLUS, MINUS, ZERO;

    public static PlusMinusZero compute(PlusMinusZero mode1, PlusMinusZero mode2) {
        if (mode1 == null || mode2 == null) {
            return null;
        }
        switch (mode1) {
            case PLUS: switch (mode2) {
                case PLUS: return PlusMinusZero.PLUS;
                case ZERO: return PlusMinusZero.PLUS;
                case MINUS: return null;
            }
            case ZERO: switch (mode2) {
                case PLUS: return PlusMinusZero.PLUS;
                case ZERO: return PlusMinusZero.ZERO;
                case MINUS: return PlusMinusZero.MINUS;
            }
            case MINUS: switch (mode2) {
                case PLUS: return null;
                case ZERO: return PlusMinusZero.MINUS;
                case MINUS: return PlusMinusZero.MINUS;
            }
        }
        // notreached
        throw new IllegalStateException();
    }
}
