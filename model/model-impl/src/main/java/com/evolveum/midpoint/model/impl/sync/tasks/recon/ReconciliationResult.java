/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import com.evolveum.midpoint.model.impl.sync.tasks.ResourceObjectClassSpecification;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.execution.ActivityExecutionResult;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;

public class ReconciliationResult implements DebugDumpable {

    private TaskRunResult runResult;
    private PrismObject<ResourceType> resource;
    private ObjectClassComplexTypeDefinition objectclassDefinition;
    private long etime; // seems unused
    private long unOpsTime; // seems unused
    private long resourceReconTime; // seems unused
    private long shadowReconTime; // seems unused
    private long unOpsCount;
    private long resourceReconCount;
    private long resourceReconErrors;
    private long shadowReconCount;

    static ReconciliationResult fromActivityExecution(@NotNull ReconciliationActivityExecution execution,
            @NotNull ActivityExecutionResult executionResult) {
        ReconciliationResult result = new ReconciliationResult();
        result.runResult = executionResult.createTaskRunResult();
        ResourceObjectClassSpecification resourceObjectClassSpecification = findTargetInfo(execution);
        if (resourceObjectClassSpecification != null) {
            result.resource = resourceObjectClassSpecification.resource.asPrismObject();
            result.objectclassDefinition = resourceObjectClassSpecification.getObjectClassDefinition();
        }
        OperationCompletionActivityExecution operationCompletionExecution = execution.getOperationCompletionExecution();
        if (operationCompletionExecution != null) {
            result.unOpsCount = operationCompletionExecution.getUnOpsCount();
        }
        ResourceObjectsReconciliationActivityExecution resourceReconciliationExecution = execution.getResourceReconciliationExecution();
        if (resourceReconciliationExecution != null) {
            result.resourceReconCount = resourceReconciliationExecution.getResourceReconCount();
            result.resourceReconErrors = resourceReconciliationExecution.getResourceReconErrors();
        }
        RemainingShadowsActivityExecution remainingShadowsExecution = execution.getRemainingShadowsExecution();
        if (remainingShadowsExecution != null) {
            result.shadowReconCount = remainingShadowsExecution.getShadowReconCount();
        }
        return result;
    }

    private static ResourceObjectClassSpecification findTargetInfo(ReconciliationActivityExecution execution) {
        for (PartialReconciliationActivityExecution<?> partialActivityExecution : execution.getPartialActivityExecutions()) {
            if (partialActivityExecution.objectClassSpec != null) {
                return partialActivityExecution.objectClassSpec;
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

    public ObjectClassComplexTypeDefinition getObjectclassDefinition() {
        return objectclassDefinition;
    }

    public void setObjectclassDefinition(
            ObjectClassComplexTypeDefinition refinedObjectclassDefinition) {
        this.objectclassDefinition = refinedObjectclassDefinition;
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
        DebugUtil.debugDumpWithLabel(sb, "rOCD", String.valueOf(objectclassDefinition), indent);
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
