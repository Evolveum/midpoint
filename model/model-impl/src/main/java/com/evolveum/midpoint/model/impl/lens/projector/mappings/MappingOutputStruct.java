/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author semancik
 *
 */
public class MappingOutputStruct<V extends PrismValue> implements DebugDumpable {

    private PrismValueDeltaSetTriple<V> outputTriple = null;
    private boolean strongMappingWasUsed = false;
    private boolean weakMappingWasUsed = false;
    private boolean pushChanges = false;

    public PrismValueDeltaSetTriple<V> getOutputTriple() {
        return outputTriple;
    }

    public void setOutputTriple(PrismValueDeltaSetTriple<V> outputTriple) {
        this.outputTriple = outputTriple;
    }

    public boolean isStrongMappingWasUsed() {
        return strongMappingWasUsed;
    }

    public void setStrongMappingWasUsed(boolean strongMappingWasUsed) {
        this.strongMappingWasUsed = strongMappingWasUsed;
    }

    public boolean isPushChanges() {
        return pushChanges;
    }

    public void setPushChanges(boolean pushChanges) {
        this.pushChanges = pushChanges;
    }

    public boolean isWeakMappingWasUsed() {
        return weakMappingWasUsed;
    }

    public void setWeakMappingWasUsed(boolean weakMappingWasUsed) {
        this.weakMappingWasUsed = weakMappingWasUsed;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(MappingOutputStruct.class, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "outputTriple", outputTriple, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "strongMappingWasUsed", strongMappingWasUsed, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "weakMappingWasUsed", weakMappingWasUsed, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "pushChanges", pushChanges, indent + 1);
        return sb.toString();
    }

}
