/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationConstructionExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationSynchronizationExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;

public class AssociationOutboundExpressionWrapper extends AssociationMappingExpressionWrapper<AssociationConstructionExpressionEvaluatorType> {

    @Serial private static final long serialVersionUID = 1L;

    public AssociationOutboundExpressionWrapper(
            PrismContainerValueWrapper<?> parent,
            PrismContainer<AssociationConstructionExpressionEvaluatorType> item,
            ItemStatus status,
            ItemPath wrapperPath,
            ExpressionType expression) {
        super(parent, item, status, wrapperPath, expression);
    }

    protected PrismPropertyValue<ExpressionType> createExpressionValue(PrismContainerValue<AssociationConstructionExpressionEvaluatorType> value) throws SchemaException {
        @NotNull AssociationConstructionExpressionEvaluatorType evaluatorBean = value.asContainerable();
        ExpressionType expressionClone = getExpression().clone();
        ExpressionUtil.updateAssociationConstructionExpressionValue(expressionClone, evaluatorBean);
        return PrismContext.get().itemFactory().createPropertyValue(expressionClone);
    }
}
