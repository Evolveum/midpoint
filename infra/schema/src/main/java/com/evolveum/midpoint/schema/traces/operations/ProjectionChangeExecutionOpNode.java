/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.operations;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.schema.traces.OpResultInfo;
import com.evolveum.midpoint.schema.traces.TraceInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

public class ProjectionChangeExecutionOpNode extends AbstractChangeExecutionOpNode {

    private final String resourceName;

    // todo discriminator

    public ProjectionChangeExecutionOpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
        resourceName = determineResourceName();
    }

    private String determineResourceName() {
        String paramResource = getParameter("resource");
        int i = paramResource.indexOf('(');
        int j = paramResource.lastIndexOf(')');
        if (i < 0 || j < 0 || i >= j) {
            return paramResource;
        } else {
            return paramResource.substring(i+1, j);
        }
    }

    public String getResourceName() {
        return resourceName;
    }

    @Override
    protected void postProcess() {
        initialize();
        setDisabled(getInfoSegments().isEmpty());
    }

    public String getInfo() {
        return String.join(", ", getInfoSegments());
    }

    public boolean hasDelta() {
        ObjectDeltaType objectDelta = getObjectDelta();
        return objectDelta != null || !getChildren(ChangeExecutionDeltaOpNode.class).isEmpty();
    }

    public List<String> getInfoSegments() {
        initialize();
        List<String> infos = new ArrayList<>();

        ObjectDeltaType objectDelta = getObjectDelta();
        if (objectDelta == null) {
            if (hasDelta()) {
                infos.add("delta");
            }
        } else {
            infos.add(getDeltaInfo(objectDelta));
        }

        for (LinkUnlinkShadowOpNode linkUnlinkChild : getChildren(LinkUnlinkShadowOpNode.class)) {
            if (linkUnlinkChild.isLink()) {
                infos.add("link");
            } else if (linkUnlinkChild.isUnlink()) {
                infos.add("unlink");
            }
        }

        for (UpdateShadowSituationOpNode updateChild : getChildren(UpdateShadowSituationOpNode.class)) {
            String situation = updateChild.getInfo();
            if (!situation.isEmpty()) { // for some reason we sometimes try to set situation on non-existing shadows
                infos.add("set " + situation);
            } else {
                infos.add("unset situation");
            }
        }

        return infos;
    }

}
