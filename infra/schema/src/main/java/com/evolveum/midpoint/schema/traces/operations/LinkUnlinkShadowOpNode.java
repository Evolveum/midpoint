/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.operations;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.schema.traces.OpResultInfo;
import com.evolveum.midpoint.schema.traces.TraceInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

import static com.evolveum.midpoint.schema.traces.OpType.getLast;

public class LinkUnlinkShadowOpNode extends OpNode {

    private final String operation;

    public LinkUnlinkShadowOpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
        operation = getLast(result.getOperation());
    }

    public boolean isLink() {
        return "linkShadow".equals(operation);
    }

    public boolean isUnlink() {
        return "unlinkShadow".equals(operation);
    }

    public String getLabel() {
        if (isLink()) {
            return "Link shadow";
        } else if (isUnlink()) {
            return "Unlink shadow";
        } else {
            return "?";
        }
    }
}
