/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.api.context.EvaluatedConstruction;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * @author mederly
 */
public class EvaluatedConstructionImpl implements EvaluatedConstruction {

    final private PrismObject<ResourceType> resource;
    final private ShadowKindType kind;
    final private String intent;
    final private boolean directlyAssigned;
    final private AssignmentPath assignmentPath;
    final private boolean weak;

    /**
     * @pre construction is already evaluated and not ignored (has resource)
     */
    public <AH extends AssignmentHolderType> EvaluatedConstructionImpl(Construction<AH> construction) {
        resource = construction.getResource().asPrismObject();
        kind = construction.getKind();
        intent = construction.getIntent();
        assignmentPath = construction.getAssignmentPath();
        directlyAssigned = assignmentPath == null || assignmentPath.size() == 1;
        weak = construction.isWeak();
    }

    @Override
    public PrismObject<ResourceType> getResource() {
        return resource;
    }

    @Override
    public ShadowKindType getKind() {
        return kind;
    }

    @Override
    public String getIntent() {
        return intent;
    }

    @Override
    public boolean isDirectlyAssigned() {
        return directlyAssigned;
    }

    @Override
    public AssignmentPath getAssignmentPath() {
        return assignmentPath;
    }

    @Override
    public boolean isWeak() {
        return weak;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, "EvaluatedConstruction", indent);
        DebugUtil.debugDumpWithLabelLn(sb, "resource", resource, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "kind", kind.value(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "intent", intent, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "directlyAssigned", directlyAssigned, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "weak", weak, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "EvaluatedConstruction(" +
                "resource=" + resource +
                ", kind=" + kind +
                ", intent='" + intent +
                ')';
    }
}
