/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.model.api.context.Mapping;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.schema.util.DiagnosticContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Interface used to intercept the ModelContext as it passes through the computation.
 *
 * It is mostly used in tests.
 *
 * EXPERIMENTAL
 *
 * @author Radovan Semancik
 *
 */
public interface ClockworkInspector extends DiagnosticContext {

    <F extends ObjectType> void clockworkStart(ModelContext<F> context);

    <F extends ObjectType> void clockworkStateSwitch(ModelContext<F> contextBefore, ModelState newState);

    <F extends ObjectType> void clockworkFinish(ModelContext<F> context);

    <F extends ObjectType> void projectorStart(ModelContext<F> context);

    void projectorComponentSkip(String componentName);

    void projectorComponentStart(String componentName);

    void projectorComponentFinish(String componentName);

    <F extends ObjectType> void projectorFinish(ModelContext<F> context);

    /**
     * May be used to gather profiling data, etc.
     */
    public <F extends ObjectType> void afterMappingEvaluation(ModelContext<F> context, Mapping<?,?> evaluatedMapping);

//    /**
//     * For all scripts expect for mappings.
//     * May be used to gather profiling data, etc.
//     */
//    public <F extends ObjectType, P extends ObjectType> void afterScriptEvaluation(LensContext<F,P> context, ScriptExpression scriptExpression);
}
