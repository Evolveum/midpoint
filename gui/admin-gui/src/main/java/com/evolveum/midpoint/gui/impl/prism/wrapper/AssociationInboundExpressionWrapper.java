/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationSynchronizationExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;

public class AssociationInboundExpressionWrapper extends AssociationMappingExpressionWrapper<AssociationSynchronizationExpressionEvaluatorType> {

    @Serial private static final long serialVersionUID = 1L;

    public AssociationInboundExpressionWrapper(
            PrismContainerValueWrapper<?> parent,
            PrismContainer<AssociationSynchronizationExpressionEvaluatorType> item,
            ItemStatus status,
            ItemPath wrapperPath,
            ExpressionType expression) {
        super(parent, item, status, wrapperPath, expression);
    }

    protected PrismPropertyValue<ExpressionType> createExpressionValue(PrismContainerValue<AssociationSynchronizationExpressionEvaluatorType> value) throws SchemaException {
        @NotNull AssociationSynchronizationExpressionEvaluatorType evaluatorBean = value.asContainerable();
        ExpressionType expressionClone = getExpression().clone();
        ExpressionUtil.updateAssociationSynchronizationExpressionValue(expressionClone, evaluatorBean);
        return PrismContext.get().itemFactory().createPropertyValue(expressionClone);
    }
}
