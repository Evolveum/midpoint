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
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

@Component
public class AssociationInboundExpressionWrapperFactory extends AssociationMappingExpressionWrapperFactory<AssociationSynchronizationExpressionEvaluatorType> {

    @Override
    protected ItemName getItemNameForContainer() {
        return ShadowAssociationDefinitionType.F_INBOUND;
    }

    @Override
    protected AssociationSynchronizationExpressionEvaluatorType getEvaluator(ExpressionType expression) throws SchemaException {
        return ExpressionUtil.getAssociationSynchronizationExpressionValue(expression);
    }

    @Override
    protected Class<AssociationSynchronizationExpressionEvaluatorType> getContainerClass() {
        return AssociationSynchronizationExpressionEvaluatorType.class;
    }

    @Override
    protected PrismContainerWrapper<AssociationSynchronizationExpressionEvaluatorType> createWrapper(PrismContainerValueWrapper<?> parent, PrismContainer<AssociationSynchronizationExpressionEvaluatorType> childContainer, ItemStatus status) {
        return new AssociationInboundExpressionWrapper(parent, childContainer, status, parent.getPath().append(getExpressionPropertyItemName()), getExpressionBean(parent));
    }
}
