/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.util.mock;

import com.evolveum.midpoint.model.api.context.Mapping;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.util.ClockworkInspector;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class MockLensDebugListener implements ClockworkInspector {

    private static final Trace LOGGER = TraceManager.getTrace(MockLensDebugListener.class);

    private static final String SEPARATOR = "############################################################################";

    private LensContext lastSyncContext;

    public <F extends ObjectType> LensContext<F> getLastSyncContext() {
        return lastSyncContext;
    }

    public <F extends ObjectType> void setLastSyncContext(ModelContext<F> lastSyncContext) {
        this.lastSyncContext = (LensContext) lastSyncContext;
    }

    @Override
    public <F extends ObjectType> void clockworkStart(ModelContext<F> context) {
        LOGGER.trace(SEPARATOR+"\nSYNC CONTEXT BEFORE SYNC\n{}\n"+SEPARATOR, context.debugDump());
    }

    @Override
    public <F extends ObjectType> void clockworkFinish(ModelContext<F> context) {
        LOGGER.trace(SEPARATOR+"\nSYNC CONTEXT AFTER SYNC\n{}\n"+SEPARATOR, context.debugDump());
        lastSyncContext = (LensContext) context;
    }

    @Override
    public <F extends ObjectType> void projectorStart(ModelContext<F> context) {
    }

    @Override
    public <F extends ObjectType> void projectorFinish(ModelContext<F> context) {
    }

    @Override
    public <F extends ObjectType> void afterMappingEvaluation(ModelContext<F> context, Mapping<?,?> evaluatedMapping) {
    }

    @Override
    public <F extends ObjectType> void clockworkStateSwitch(ModelContext<F> contextBefore, ModelState newState) {
    }

    @Override
    public void projectorComponentSkip(String componentName) {
    }

    @Override
    public void projectorComponentStart(String componentName) {
    }

    @Override
    public void projectorComponentFinish(String componentName) {
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(MockLensDebugListener.class, indent);
        DebugUtil.debugDumpWithLabelToString(sb, "lastSyncContext", lastSyncContext, indent + 1);
        return sb.toString();
    }
}
