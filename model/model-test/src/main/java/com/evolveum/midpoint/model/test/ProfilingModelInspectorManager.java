/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.util.DiagnosticContextManager;
import com.evolveum.midpoint.model.common.util.ProfilingModelInspector;
import com.evolveum.midpoint.schema.util.DiagnosticContext;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 */
public class ProfilingModelInspectorManager implements DiagnosticContextManager, DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(ProfilingModelInspectorManager.class);

    private ProfilingModelInspector lastInspector = null;
    private int numberOfModelInvocations = 0;

    public void reset() {
        lastInspector = null;
        numberOfModelInvocations = 0;
    }

    @Override
    public DiagnosticContext createNewContext() {
        numberOfModelInvocations++;
        ProfilingModelInspector inspector = new ProfilingModelInspector();
        inspector.recordStart();
        return inspector;
    }

    @Override
    public void processFinishedContext(DiagnosticContext ctx) {
        if (ctx instanceof ProfilingModelInspector) {
            lastInspector = (ProfilingModelInspector)ctx;
            lastInspector.recordFinish();
        } else {
            lastInspector = null;
        }
        LOGGER.info("Model diagnostics:{}", ctx.debugDump(1));
    }

    public ModelContext<?> getLastLensContext() {
        if (lastInspector == null) {
            return null;
        }
        return lastInspector.getLastLensContext();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(ProfilingModelInspectorManager.class, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "numberOfModelInvocations", numberOfModelInvocations, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "lastInspector", lastInspector, indent + 1);
        return sb.toString();
    }

}
