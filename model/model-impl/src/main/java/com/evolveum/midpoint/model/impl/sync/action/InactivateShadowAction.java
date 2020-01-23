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
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class InactivateShadowAction extends BaseAction {

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.model.sync.Action#handle(com.evolveum.midpoint.model.lens.LensContext, com.evolveum.midpoint.model.sync.SynchronizationSituation, java.util.Map, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
     */
    @Override
    public <F extends FocusType> void handle(LensContext<F> context, SynchronizationSituation<F> situation,
            Map<QName, Object> parameters, Task task, OperationResult parentResult) {
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
        ObjectDelta<ShadowType> activationDelta = getPrismContext().deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                projectionContext.getOid(), pathAdminStatus, desiredStatus);
        projectionContext.setPrimaryDelta(activationDelta);
    }

}
