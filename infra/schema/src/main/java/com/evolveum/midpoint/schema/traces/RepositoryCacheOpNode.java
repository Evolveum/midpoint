/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.traces;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

public class RepositoryCacheOpNode extends RepositoryOpNode {

    public RepositoryCacheOpNode(PrismContext prismContext,
            OperationResultType result, OpResultInfo info, OpNode parent, TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
    }
}
