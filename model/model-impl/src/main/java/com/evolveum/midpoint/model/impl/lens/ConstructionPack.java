/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author semancik
 *
 */
public class ConstructionPack<T extends AbstractConstruction> implements DebugDumpable {

    private final Collection<PrismPropertyValue<T>> constructions = new ArrayList<>();
    private boolean forceRecon;
    private boolean hasValidAssignment = false;

    public boolean isForceRecon() {
        return forceRecon;
    }

    public void setForceRecon(boolean forceRecon) {
        this.forceRecon = forceRecon;
    }

    public Collection<PrismPropertyValue<T>> getConstructions() {
        return constructions;
    }

    public void add(PrismPropertyValue<T> construction) {
        constructions.add(construction);
    }

    public boolean hasValidAssignment() {
        return hasValidAssignment;
    }

    public void setHasValidAssignment(boolean hasValidAssignment) {
        this.hasValidAssignment = hasValidAssignment;
    }

    public boolean hasStrongConstruction() {
        for (PrismPropertyValue<T> construction: constructions) {
            if (!construction.getValue().isWeak()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "ConstructionPack(" + SchemaDebugUtil.prettyPrint(constructions) + (forceRecon ? ", forceRecon" : "") + ")";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabel(sb, "ConstructionPack", indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "forceRecon", forceRecon, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "hasValidAssignment", hasValidAssignment, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "Constructions", constructions, indent + 1);
        return sb.toString();
    }

}
