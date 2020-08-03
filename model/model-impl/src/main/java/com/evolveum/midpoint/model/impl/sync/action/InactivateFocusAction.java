/*
 * Copyright (c) 2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.action;

import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.sync.SynchronizationSituation;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 *
 */
public class InactivateFocusAction extends BaseAction {

    @Override
    public <F extends FocusType> void handle(LensContext<F> context, SynchronizationSituation<F> situation,
            Map<QName, Object> parameters, Task task, OperationResult parentResult) {
        ActivationStatusType desiredStatus = ActivationStatusType.DISABLED;

        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext != null) {
            PrismObject<F> objectCurrent = focusContext.getObjectCurrent();
            ItemPath pathAdminStatus = SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS;
            if (objectCurrent != null) {
                PrismProperty<Object> administrativeStatusProp = objectCurrent.findProperty(pathAdminStatus);
                if (administrativeStatusProp != null) {
                    if (desiredStatus.equals(administrativeStatusProp.getRealValue())) {
                        // Desired status already set, nothing to do
                        return;
                    }
                }
            }
            ObjectDelta<F> activationDelta = getPrismContext().deltaFactory().object().createModificationReplaceProperty(focusContext.getObjectTypeClass(),
                    focusContext.getOid(), pathAdminStatus, desiredStatus);
            focusContext.setPrimaryDelta(activationDelta);
        }

    }

}
