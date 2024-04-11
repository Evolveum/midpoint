/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.sync;

import com.evolveum.midpoint.model.impl.sync.tasks.ResourceSetTaskWorkDefinition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ResourceObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.schema.util.task.work.ResourceObjectSetUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LiveSyncWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;

public class LiveSyncWorkDefinition extends ResourceSetTaskWorkDefinition implements ResourceObjectSetSpecificationProvider {

    private final Integer batchSize;
    private final boolean updateLiveSyncTokenInDryRun;
    private final boolean updateLiveSyncTokenInPreviewMode;

    LiveSyncWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
        super(info);
        var typedDefinition = (LiveSyncWorkDefinitionType) info.getBean();
        batchSize = typedDefinition.getBatchSize();
        updateLiveSyncTokenInPreviewMode = Boolean.TRUE.equals(typedDefinition.isUpdateLiveSyncTokenInPreviewMode());
        ResourceObjectSetUtil.removeQuery(getResourceObjectSetSpecification());
        updateLiveSyncTokenInDryRun = Boolean.TRUE.equals(typedDefinition.isUpdateLiveSyncTokenInDryRun());
    }

    Integer getBatchSize() {
        return batchSize;
    }

    boolean isUpdateLiveSyncTokenInDryRun() {
        return updateLiveSyncTokenInDryRun;
    }

    boolean isUpdateLiveSyncTokenInPreviewMode() {
        return updateLiveSyncTokenInPreviewMode;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObjects", getResourceObjectSetSpecification(), indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "batchSize", batchSize, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "updateLiveSyncTokenInDryRun", updateLiveSyncTokenInDryRun, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "updateLiveSyncTokenInPreviewMode", updateLiveSyncTokenInPreviewMode, indent+1);
    }
}
