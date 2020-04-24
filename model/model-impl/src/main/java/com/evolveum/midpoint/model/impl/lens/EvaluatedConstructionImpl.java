/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.api.context.EvaluatedConstruction;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class EvaluatedConstructionImpl<AH extends AssignmentHolderType> implements EvaluatedConstruction {

    @NotNull final private Construction<AH> construction;
    @NotNull final private ResourceShadowDiscriminator rsd;

    private LensProjectionContext projectionContext;

    /**
     * @pre construction is already evaluated and not ignored (has resource)
     */
    EvaluatedConstructionImpl(@NotNull final Construction<AH> construction, @NotNull final ResourceShadowDiscriminator rsd) {
        this.construction = construction;
        this.rsd = rsd;
    }

    public void initialize() {
        projectionContext = construction.getLensContext().findProjectionContext(rsd);
        // projection context may not exist yet (existence might not be yet decided)
    }

    @Override
    public PrismObject<ResourceType> getResource() {
        return construction.getResource().asPrismObject();
    }

    @Override
    public ShadowKindType getKind() {
        return rsd.getKind();
    }

    @Override
    public String getIntent() {
        return rsd.getIntent();
    }

    @Override
    public String getTag() {
        return rsd.getTag();
    }

    @Override
    public boolean isDirectlyAssigned() {
        return construction.getAssignmentPath() == null || construction.getAssignmentPath().size() == 1;
    }

    @Override
    public AssignmentPath getAssignmentPath() {
        return construction.getAssignmentPath();
    }

    @Override
    public boolean isWeak() {
        return construction.isWeak();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, "EvaluatedConstruction", indent);
        DebugUtil.debugDumpWithLabelShortDumpLn(sb, "discriminator", rsd, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "construction", construction, indent + 1);
        DebugUtil.debugDumpWithLabelToString(sb, "projectionContext", projectionContext, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "EvaluatedConstruction(" +
                "discriminator=" + rsd +
                ", construction=" + construction +
                ", projectionContext='" + projectionContext +
                ')';
    }
}
