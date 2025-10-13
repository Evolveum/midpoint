/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.traces;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TraceDictionaryEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingOutputType;

@Experimental
public class TraceInfo {
    private final TracingOutputType tracingOutput;

    public TraceInfo(TracingOutputType tracingOutput) {
        this.tracingOutput = tracingOutput;
    }

    public TracingOutputType getTracingOutput() {
        return tracingOutput;
    }

    public PrismObject<?> findObject(String oid) {
        if (oid == null || tracingOutput == null || tracingOutput.getDictionary() == null) {
            return null;
        }
        for (TraceDictionaryEntryType entry : tracingOutput.getDictionary().getEntry()) {
            if (oid.equals(entry.getObject().getOid())) {
                PrismObject<?> object = entry.getObject().getObject();
                if (object != null) {
                    return object;
                }
            }
        }
        return null;
    }
}
