/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

    private StrengthSelector(boolean weak, boolean normal, boolean strong) {
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
