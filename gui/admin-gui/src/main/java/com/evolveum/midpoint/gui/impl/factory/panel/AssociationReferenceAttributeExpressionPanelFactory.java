/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.component.input.expression.ExpressionPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ExpressionWrapper;
import com.evolveum.midpoint.web.component.input.AssociationExpressionValuePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AssociationReferenceAttributeExpressionPanelFactory extends AssociationAndExpressionPanelFactory {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!super.match(wrapper,valueWrapper)) {
            return false;
        }

        var parent = wrapper.getParentContainerValue(ResourceAttributeDefinitionType.class);
        if (parent == null || !parent.getParent().getItemName().equivalent(ShadowAssociationDefinitionType.F_OBJECT_REF)) {
            return false;
        }

        return true;
    }

    @Override
    protected List<ExpressionPanel.RecognizedEvaluator> getChoices(List<ExpressionPanel.RecognizedEvaluator> parentChoices) {
        return parentChoices;
    }

    @Override
    public Integer getOrder() {
        return 99;
    }
}
