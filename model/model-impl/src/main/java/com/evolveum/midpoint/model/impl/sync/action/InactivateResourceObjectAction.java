/*
 * Copyright (c) 2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.action;

import com.evolveum.midpoint.model.impl.sync.reactions.ActionDefinitionClass;
import com.evolveum.midpoint.model.impl.sync.reactions.ActionInstantiationContext;

import com.evolveum.midpoint.model.impl.sync.reactions.ActionUris;

import com.evolveum.midpoint.xml.ns._public.common.common_3.InactivateResourceObjectSynchronizationActionType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
@ActionUris({
        "http://midpoint.evolveum.com/xml/ns/public/model/action-3#inactivateShadow",
        "http://midpoint.evolveum.com/xml/ns/public/model/action-3#disableAccount",
        "http://midpoint.evolveum.com/xml/ns/public/model/action-3#inactivateResourceObject" // not officially supported
})
@ActionDefinitionClass(InactivateResourceObjectSynchronizationActionType.class)
class InactivateResourceObjectAction<F extends FocusType> extends BaseClockworkAction<F> {

    InactivateResourceObjectAction(@NotNull ActionInstantiationContext<F> ctx) {
        super(ctx);
    }

    @Override
    public void prepareContext(
            @NotNull LensContext<F> context,
            @NotNull OperationResult result) {
        ActivationStatusType desiredStatus = ActivationStatusType.DISABLED;

        ItemPath pathAdminStatus = SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS;
        LensProjectionContext projectionContext = context.getProjectionContextsIterator().next();
        PrismObject<ShadowType> objectCurrent = projectionContext.getObjectCurrent();
        if (objectCurrent != null) {
            PrismProperty<Object> administrativeStatusProp = objectCurrent.findProperty(pathAdminStatus);
            if (administrativeStatusProp != null) {
                if (desiredStatus.equals(administrativeStatusProp.getRealValue())) {
                    // Desired status already set, nothing to do
                    return;
                }
            }
        }
        ObjectDelta<ShadowType> activationDelta = PrismContext.get().deltaFactory().object()
                .createModificationReplaceProperty(
                        ShadowType.class,
                        projectionContext.getOid(),
                        pathAdminStatus,
                        desiredStatus);
        projectionContext.setPrimaryDelta(activationDelta);
    }

}
