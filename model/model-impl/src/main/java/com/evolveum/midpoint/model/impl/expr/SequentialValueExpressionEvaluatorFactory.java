/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.expr;

import java.util.Collection;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.AbstractAutowiredExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SequentialValueExpressionEvaluatorType;

/**
 * @author semancik
 */
@Component
public class SequentialValueExpressionEvaluatorFactory extends AbstractAutowiredExpressionEvaluatorFactory {

    private static final QName ELEMENT_NAME = SchemaConstantsGenerated.C_SEQUENTIAL_VALUE;

    @Autowired private Protector protector;
    @Autowired private PrismContext prismContext;
    @Autowired private RepositoryService repositoryService;

    @Override
    public QName getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public <V extends PrismValue, D extends ItemDefinition<?>> ExpressionEvaluator<V> createEvaluator(
            @NotNull Collection<JAXBElement<?>> evaluatorElements,
            @Nullable D outputDefinition,
            @Nullable ExpressionProfile expressionProfile,
            @NotNull ExpressionFactory expressionFactory,
            @NotNull String contextDescription,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException {

        SequentialValueExpressionEvaluatorType evaluatorBean = getSingleEvaluatorBeanRequired(evaluatorElements,
                SequentialValueExpressionEvaluatorType.class, contextDescription);

        if (evaluatorBean.getSequenceRef() != null && evaluatorBean.getSequenceRef().getOid() != null) {
            return new SequentialValueExpressionEvaluator<>(
                    ELEMENT_NAME,
                    evaluatorBean.getSequenceRef().getOid(),
                    evaluatorBean,
                    outputDefinition,
                    protector,
                    repositoryService);
        } else {
            throw new SchemaException("Missing sequence reference in sequentialValue expression evaluator in "+contextDescription);
        }
    }
}
