/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
public class ShadowDescriptionHandler implements VisualizationDescriptionHandler {

    @Autowired
    private Resolver resolver;

    @Override
    public boolean match(VisualizationImpl visualization) {
        PrismContainerValue<?> value = visualization.getSourceValue();
        return value != null && value.asContainerable() instanceof ShadowType;
    }

    @Override
    public void apply(VisualizationImpl visualization, Task task, OperationResult result) {
        PrismContainerValue<?> value = visualization.getSourceValue();
        ShadowType shadow = (ShadowType) value.asContainerable();

        ShadowKindType kind = shadow.getKind() != null ? shadow.getKind() : ShadowKindType.UNKNOWN;

        ResourceAttribute<?> namingAttribute = ShadowUtil.getNamingAttribute(shadow);
        Object realName = namingAttribute != null ? namingAttribute.getRealValue() : null;
        String name = realName != null ? realName.toString() : "";
        ChangeType change = visualization.getChangeType();

        String intent = shadow.getIntent() != null ? shadow.getIntent() : "";

        ObjectReferenceType resourceRef = shadow.getResourceRef();

        Object resourceName = resolver.resolveReferenceName(resourceRef, false, task, result);
        if (resourceName == null) {
            resourceName = new SingleLocalizableMessage("ShadowDescriptionHandler.unknownResource",
                    new Object[] { resourceRef != null ? resourceRef.getOid() : null });
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
