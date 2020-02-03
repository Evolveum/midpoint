/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

/**
 * @author semancik
 */
public class StrengthSelector {

    public static final StrengthSelector ALL = new StrengthSelector(true, true, true);
    public static final StrengthSelector ALL_EXCEPT_WEAK = new StrengthSelector(false, true, true);
    public static final StrengthSelector WEAK_ONLY = new StrengthSelector(true, false, false);

    private final boolean weak;
    private final boolean normal;
    private final boolean strong;

    public StrengthSelector(boolean weak, boolean normal, boolean strong) {
        super();
        this.weak = weak;
        this.normal = normal;
        this.strong = strong;
    }

    public boolean isWeak() {
        return weak;
    }

    public boolean isNormal() {
        return normal;
    }

    public boolean isStrong() {
        return strong;
    }

    public StrengthSelector notWeak() {
        return new StrengthSelector(false, normal, strong);
    }

    public boolean isNone() {
        return !weak && !normal && !strong;
    }

    @Override
    public String toString() {
        return "StrengthSelector(weak=" + weak + ", normal=" + normal + ", strong=" + strong + ")";
    }
}
