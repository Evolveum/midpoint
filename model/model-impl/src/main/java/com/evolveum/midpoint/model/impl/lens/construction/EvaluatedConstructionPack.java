/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class EvaluatedConstructionPack<EC extends EvaluatedAbstractConstruction> implements DebugDumpable {

    private final Collection<EC> evaluatedConstructions = new ArrayList<>();
    private boolean forceRecon;
    private boolean hasValidAssignment = false;

    public boolean isForceRecon() {
        return forceRecon;
    }

    public void setForceRecon(boolean forceRecon) {
        this.forceRecon = forceRecon;
    }

    public @NotNull Collection<EC> getEvaluatedConstructions() {
        return evaluatedConstructions;
    }

    public void add(EC evaluatedConstruction) {
        evaluatedConstructions.add(evaluatedConstruction);
    }

    public boolean hasValidAssignment() {
        return hasValidAssignment;
    }

    public void setHasValidAssignment(boolean hasValidAssignment) {
        this.hasValidAssignment = hasValidAssignment;
    }

    public boolean hasNonWeakConstruction() {
        for (EC evaluatedConstruction: evaluatedConstructions) {
            if (!evaluatedConstruction.getConstruction().isWeak()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "EvaluatedConstructionPack(" + SchemaDebugUtil.prettyPrint(evaluatedConstructions) + (forceRecon ? ", forceRecon" : "") + ")";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, "EvaluatedConstructionPack", indent);
        DebugUtil.debugDumpWithLabelLn(sb, "forceRecon", forceRecon, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "hasValidAssignment", hasValidAssignment, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "evaluatedConstructions", evaluatedConstructions, indent + 1);
        return sb.toString();
    }

}
