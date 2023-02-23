/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
public class ShadowDescriptionHandler implements VisualizationDescriptionHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowDescriptionHandler.class);

    @Autowired
    private ModelService modelService;

    @Override
    public boolean match(VisualizationImpl visualization) {
        PrismContainerValue value = visualization.getSourceValue();
        if (value == null || !(value.asContainerable() instanceof ShadowType)) {
            return false;
        }

        return true;
    }

    @Override
    public void apply(VisualizationImpl visualization, Task task, OperationResult result) {
        PrismContainerValue value = visualization.getSourceValue();
        ShadowType shadow = (ShadowType) value.asContainerable();

        ShadowKindType kind = shadow.getKind() != null ? shadow.getKind() : ShadowKindType.UNKNOWN;

        ResourceAttribute namingAttribute = ShadowUtil.getNamingAttribute(shadow);
        Object realName = namingAttribute != null ? namingAttribute.getRealValue() : null;
        String name = realName != null ? realName.toString() : "";
        ChangeType change = visualization.getChangeType();

        String intent = shadow.getIntent() != null ? shadow.getIntent() : "";

        Object resourceName;
        ObjectReferenceType resourceRef = shadow.getResourceRef();
        try {
            PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, resourceRef.getOid(), GetOperationOptions.createRawCollection(), task, result);
            resourceName = resource.getName().getOrig();
        } catch (Exception ex) {
            resourceName = new SingleLocalizableMessage("ShadowDescriptionHandler.unknownResource", new Object[] { resourceRef.getOid() });

            LOGGER.debug("Couldn't get resource", ex);
        }

        visualization.getName().setOverview(
                new SingleLocalizableMessage("ShadowDescriptionHandler.shadow", new Object[] {
                        new SingleLocalizableMessage("ShadowKindType." + kind.name()),
                        name,
                        intent,
                        new SingleLocalizableMessage("ShadowDescriptionHandler.changeType." + change.name()),
                        resourceName
                })
        );
    }
}
