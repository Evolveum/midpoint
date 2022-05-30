/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import com.evolveum.midpoint.model.impl.sync.tasks.ProcessingScope;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;

public class ReconciliationResult implements DebugDumpable {

    private TaskRunResult runResult;
    private PrismObject<ResourceType> resource;
    private ResourceObjectDefinition resourceObjectDefinition;
    private long etime; // seems unused
    private long unOpsTime; // seems unused
    private long resourceReconTime; // seems unused
    private long shadowReconTime; // seems unused
    private long unOpsCount;
    private long resourceReconCount;
    private long resourceReconErrors;
    private long shadowReconCount;

    static ReconciliationResult fromActivityRun(@NotNull ReconciliationActivityRun execution,
            @NotNull ActivityRunResult executionResult) {
        ReconciliationResult result = new ReconciliationResult();
        result.runResult = executionResult.createTaskRunResult();
        ProcessingScope processingScope = findProcessingScope(execution);
        if (processingScope != null) {
            result.resource = processingScope.resource.asPrismObject();
            result.resourceObjectDefinition = processingScope.getResourceObjectDefinition();
        }
        OperationCompletionActivityRun operationCompletionExecution = execution.getOperationCompletionExecution();
        if (operationCompletionExecution != null) {
            result.unOpsCount = operationCompletionExecution.getUnOpsCount();
        }
        ResourceObjectsReconciliationActivityRun resourceReconciliationExecution = execution.getResourceReconciliationExecution();
        if (resourceReconciliationExecution != null) {
            result.resourceReconCount = resourceReconciliationExecution.getResourceReconCount();
            result.resourceReconErrors = resourceReconciliationExecution.getResourceReconErrors();
        }
        RemainingShadowsActivityRun remainingShadowsExecution = execution.getRemainingShadowsExecution();
        if (remainingShadowsExecution != null) {
            result.shadowReconCount = remainingShadowsExecution.getShadowReconCount();
        }
        return result;
    }

    private static ProcessingScope findProcessingScope(ReconciliationActivityRun run) {
        for (PartialReconciliationActivityRun partialActivityRun : run.getPartialActivityRunsList()) {
            if (partialActivityRun.processingScope != null) {
                return partialActivityRun.processingScope;
            }
        }
        return null;
    }

    public TaskRunResult getRunResult() {
        return runResult;
    }

    public void setRunResult(TaskRunResult runResult) {
        this.runResult = runResult;
    }

    public PrismObject<ResourceType> getResource() {
        return resource;
    }

    public String getResourceOid() {
        return resource != null ? resource.getOid() : null;
    }

    public void setResource(PrismObject<ResourceType> resource) {
        this.resource = resource;
    }

    public ResourceObjectDefinition getResourceObjectDefinition() {
        return resourceObjectDefinition;
    }

    public void setResourceObjectDefinition(
            ResourceObjectClassDefinition refinedObjectclassDefinition) {
        this.resourceObjectDefinition = refinedObjectclassDefinition;
    }

    public long getEtime() {
        return etime;
    }

    public void setEtime(long etime) {
        this.etime = etime;
    }

    public long getUnOpsTime() {
        return unOpsTime;
    }

    public void setUnOpsTime(long unOpsTime) {
        this.unOpsTime = unOpsTime;
    }

    public long getResourceReconTime() {
        return resourceReconTime;
    }

    public void setResourceReconTime(long resourceReconTime) {
        this.resourceReconTime = resourceReconTime;
    }

    public long getShadowReconTime() {
        return shadowReconTime;
    }

    public void setShadowReconTime(long shadowReconTime) {
        this.shadowReconTime = shadowReconTime;
    }

    public long getUnOpsCount() {
        return unOpsCount;
    }

    public void setUnOpsCount(long unOpsCount) {
        this.unOpsCount = unOpsCount;
    }

    public long getResourceReconCount() {
        return resourceReconCount;
    }

    public void setResourceReconCount(long resourceReconCount) {
        this.resourceReconCount = resourceReconCount;
    }

    public long getResourceReconErrors() {
        return resourceReconErrors;
    }

    public void setResourceReconErrors(long resourceReconErrors) {
        this.resourceReconErrors = resourceReconErrors;
    }

    public long getShadowReconCount() {
        return shadowReconCount;
    }

    public void setShadowReconCount(long shadowReconCount) {
        this.shadowReconCount = shadowReconCount;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ReconciliationTaskResult");
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "runResult", runResult.toString(), indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "resource", resource.toString(), indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "rOCD", String.valueOf(resourceObjectDefinition), indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "etime", etime, indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "unOpsCount", unOpsCount, indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "unOpsTime", unOpsTime, indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "resourceReconCount", resourceReconCount, indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "resourceReconErrors", resourceReconErrors, indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "resourceReconTime", resourceReconTime, indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "shadowReconCount", shadowReconCount, indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "shadowReconTime", shadowReconTime, indent);
        return sb.toString();
    }

}
