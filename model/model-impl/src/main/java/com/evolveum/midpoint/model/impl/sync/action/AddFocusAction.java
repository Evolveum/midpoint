/*
 * Copyright (c) 2013-2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.action;

import com.evolveum.midpoint.model.impl.sync.reactions.ActionDefinitionClass;
import com.evolveum.midpoint.model.impl.sync.reactions.ActionInstantiationContext;

import com.evolveum.midpoint.model.impl.sync.reactions.ActionUris;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AddFocusSynchronizationActionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Creates a focus object for (typically unmatched) shadow.
 *
 * @author semancik
 */
@ActionUris({
        "http://midpoint.evolveum.com/xml/ns/public/model/action-3#addFocus",
        "http://midpoint.evolveum.com/xml/ns/public/model/action-3#addUser" })
@ActionDefinitionClass(AddFocusSynchronizationActionType.class)
public class AddFocusAction<F extends FocusType> extends BaseClockworkAction<F> {

    private static final Trace LOGGER = TraceManager.getTrace(AddFocusAction.class);

    public AddFocusAction(@NotNull ActionInstantiationContext<F> ctx) {
        super(ctx);
    }

    @Override
    public void prepareContext(@NotNull LensContext<F> context, @NotNull OperationResult result) throws SchemaException {
        LensFocusContext<F> focusContext = context.createFocusContext();
        Class<F> focusClass = focusContext.getObjectTypeClass();
        LOGGER.trace("addFocus action: add delta for {}", focusClass);
        PrismObjectDefinition<F> focusDefinition =
                PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusClass);
        PrismObject<F> emptyFocus = focusDefinition.instantiate();
        setArchetypeIfConfigured(emptyFocus.asObjectable());
        ObjectDelta<F> delta = emptyFocus.createAddDelta();
        delta.setObjectToAdd(emptyFocus);
        focusContext.setPrimaryDelta(delta);
    }

    private void setArchetypeIfConfigured(F focus) {
        String archetypeOid = syncCtx.getSynchronizationPolicy().getArchetypeOid();
        if (archetypeOid != null) {
            LOGGER.trace("Setting archetype to {} as configured in object type definition", archetypeOid);
            focus.assignment(new AssignmentType()
                    .targetRef(archetypeOid, ArchetypeType.COMPLEX_TYPE));
        }
    }
}
