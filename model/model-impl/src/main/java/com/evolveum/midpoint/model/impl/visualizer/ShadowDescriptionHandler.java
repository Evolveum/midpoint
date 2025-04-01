/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import com.evolveum.midpoint.model.api.visualizer.LocalizationCustomizationContext;
import com.evolveum.midpoint.model.api.visualizer.localization.LocalizationPart;
import com.evolveum.midpoint.model.api.visualizer.localization.WrapableLocalization;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
public class ShadowDescriptionHandler implements VisualizationDescriptionHandler {
    private static final LocalizableMessage ON = new SingleLocalizableMessage(
            "ShadowDescriptionHandler.shadow.on", null, "on");

    @Autowired
    private Resolver resolver;

    @Override
    public boolean match(VisualizationImpl visualization, VisualizationImpl parentVisualization) {
        PrismContainerValue<?> value = visualization.getSourceValue();
        return value != null && ShadowType.class.equals(value.getCompileTimeClass());
    }

    @Override
    public void apply(VisualizationImpl visualization, VisualizationImpl parentVisualization, Task task, OperationResult result) {
        PrismContainerValue<?> value = visualization.getSourceValue();
        ShadowType shadow = (ShadowType) value.asContainerable();

        ShadowKindType kind = shadow.getKind() != null ? shadow.getKind() : ShadowKindType.UNKNOWN;

        String name = getShadowName(shadow);

        ChangeType change = visualization.getChangeType();

        String intent = shadow.getIntent() != null ? "(" + shadow.getIntent() + ")" : "";

        ObjectReferenceType resourceRef = shadow.getResourceRef();

        final LocalizableMessage localizableResourceName;
        final String resourceName = resolver.resolveReferenceName(resourceRef, false, task, result);
        if (resourceName == null) {
            localizableResourceName = new SingleLocalizableMessage("ShadowDescriptionHandler.unknownResource",
                    new Object[] { resourceRef != null ? resourceRef.getOid() : null });
        } else {
            localizableResourceName =  new SingleLocalizableMessage("", null, resourceName);
        }

        final LocalizableMessage localizableKind = new SingleLocalizableMessage(
                "ShadowKindType." + kind.name());
        final LocalizableMessage localizableShadowName = new SingleLocalizableMessage(name);
        final LocalizableMessage localizableChange = new SingleLocalizableMessage(
                "ShadowDescriptionHandler.changeType." + change.name());
        final LocalizableMessage localizableIntent = new SingleLocalizableMessage("", null, intent);

        final WrapableLocalization<String, LocalizationCustomizationContext> customizableOverview =
                WrapableLocalization.of(
                        LocalizationPart.forObject(localizableKind, LocalizationCustomizationContext.empty()),
                        LocalizationPart.forObjectName(localizableShadowName, LocalizationCustomizationContext.empty()),
                        LocalizationPart.forAdditionalInfo(localizableIntent, LocalizationCustomizationContext.empty()),
                        LocalizationPart.forAction(localizableChange, LocalizationCustomizationContext.empty()),
                        LocalizationPart.forHelpingWords(ON),
                        LocalizationPart.forObjectName(localizableResourceName, LocalizationCustomizationContext.empty())
                );

        visualization.getName().setCustomizableOverview(customizableOverview);
        visualization.getName().setOverview(
                new SingleLocalizableMessage("ShadowDescriptionHandler.shadow", new Object[] {
                        localizableKind,
                        localizableShadowName,
                        intent,
                        localizableChange,
                        localizableResourceName
                })
        );
    }

    protected final String getShadowName(ShadowType shadow) {
        String name = null;

        PolyStringType nameBean = shadow.getName();
        if (nameBean != null) {
            if (nameBean.getTranslation() != null && StringUtils.isNotEmpty(nameBean.getTranslation().getKey())) {
                name = nameBean.getTranslation().getKey();
            } else if (StringUtils.isNotEmpty(nameBean.getOrig())) {
                name = nameBean.getOrig();
            }
        }

        if (StringUtils.isEmpty(name)) {
            ShadowSimpleAttribute<?> namingAttribute = ShadowUtil.getNamingAttribute(shadow);
            Object realName = namingAttribute != null ? namingAttribute.getRealValue() : null;
            name = realName != null ? realName.toString() : "ShadowDescriptionHandler.noName";
        }
        return name;
    }
}
