/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper.resourceAssociation;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.AssociationInboundExpressionWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.AssociationOutboundExpressionWrapper;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationConstructionExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationSynchronizationExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationDefinitionType;

import org.springframework.stereotype.Component;

@Component
public class AssociationOutboundExpressionWrapperFactory extends AssociationMappingExpressionWrapperFactory<AssociationConstructionExpressionEvaluatorType> {

    @Override
    protected ItemName getItemNameForContainer() {
        return ShadowAssociationDefinitionType.F_OUTBOUND;
    }

    @Override
    protected AssociationConstructionExpressionEvaluatorType getEvaluator(ExpressionType expression) throws SchemaException {
        return ExpressionUtil.getAssociationConstructionExpressionValue(expression);
    }

    @Override
    protected Class<AssociationConstructionExpressionEvaluatorType> getContainerClass() {
        return AssociationConstructionExpressionEvaluatorType.class;
    }

    @Override
    protected PrismContainerWrapper<AssociationConstructionExpressionEvaluatorType> createWrapper(PrismContainerValueWrapper<?> parent, PrismContainer<AssociationConstructionExpressionEvaluatorType> childContainer, ItemStatus status) {
        return new AssociationOutboundExpressionWrapper(parent, childContainer, status, parent.getPath().append(getExpressionPropertyItemName()), getExpressionBean(parent));
    }

}
