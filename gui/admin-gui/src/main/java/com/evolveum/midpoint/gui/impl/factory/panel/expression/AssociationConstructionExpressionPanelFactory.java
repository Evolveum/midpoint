/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.expression;

import com.evolveum.midpoint.gui.impl.component.input.expression.ExpressionPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationConstructionExpressionEvaluatorType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeOutboundMappingsDefinitionType;

import org.springframework.stereotype.Component;

@Component
public class AssociationConstructionExpressionPanelFactory extends AssociationReferenceAttributeExpressionPanelFactory{

    @Override
    protected Class<? extends Containerable> getMappingsClass() {
        return AttributeOutboundMappingsDefinitionType.class;
    }

    @Override
    protected Class<? extends Containerable> getEvaluatorClass() {
        return AssociationConstructionExpressionEvaluatorType.class;
    }

    @Override
    protected ExpressionPanel.RecognizedEvaluator expressionEvaluatorForRemove() {
        return ExpressionPanel.RecognizedEvaluator.SHADOW_OWNER_REFERENCE_SEARCH;
    }
}
