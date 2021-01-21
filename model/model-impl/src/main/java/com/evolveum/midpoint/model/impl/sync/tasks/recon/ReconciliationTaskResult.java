/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

public class ReconciliationTaskResult implements DebugDumpable {

    private TaskRunResult runResult;
    private PrismObject<ResourceType> resource;
    private ObjectClassComplexTypeDefinition objectclassDefinition;
    private long etime;
    private long unOpsTime;
    private long resourceReconTime;
    private long shadowReconTime;
    private long unOpsCount;
    private long resourceReconCount;
    private long resourceReconErrors;
    private long shadowReconCount;

    public TaskRunResult getRunResult() {
        return runResult;
    }

    public void setRunResult(TaskRunResult runResult) {
        this.runResult = runResult;
    }

    public PrismObject<ResourceType> getResource() {
        return resource;
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
        DebugUtil.debugDumpWithLabel(sb, "rOCD", objectclassDefinition.toString(), indent);
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
