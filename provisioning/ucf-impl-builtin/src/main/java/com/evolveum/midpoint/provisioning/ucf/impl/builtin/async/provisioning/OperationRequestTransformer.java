/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning;

import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningRequest;
import com.evolveum.midpoint.provisioning.ucf.api.async.StringAsyncProvisioningRequest;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PredefinedOperationRequestTransformationType;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PredefinedOperationRequestTransformationType.SIMPLIFIED_JSON;

/**
 * Transforms asynchronous operation (i.e. abstract description what has to be done) into specific request
 * (basically a message that is to be send to a given target).
 */
public class OperationRequestTransformer {

    @NotNull private final AsyncProvisioningConnectorInstance connectorInstance;
    @NotNull private final TransformerHelper transformerHelper;

    private static final String VAR_OPERATION_REQUESTED = "operationRequested";
    private static final String VAR_TRANSFORMER_HELPER = "transformerHelper";
    private static final String VAR_REQUEST_FORMATTER = "requestFormatter";

    public OperationRequestTransformer(@NotNull AsyncProvisioningConnectorInstance connectorInstance) {
        this.connectorInstance = connectorInstance;
        this.transformerHelper = new TransformerHelper(connectorInstance);
    }

    @NotNull
    public AsyncProvisioningRequest transformOperationRequested(@NotNull OperationRequested operationRequested,
            Task task, OperationResult result) {

        try {
            PredefinedOperationRequestTransformationType predefinedTransformation = connectorInstance.getPredefinedTransformation();
            if (predefinedTransformation != null) {
                return transformerHelper.applyPredefinedTransformation(operationRequested, predefinedTransformation);
            }

            ExpressionType transformExpression = connectorInstance.getTransformExpression();
            if (transformExpression != null) {

                VariablesMap variables = new VariablesMap();
                variables.put(VAR_OPERATION_REQUESTED, operationRequested, operationRequested.getClass());
                variables.put(VAR_TRANSFORMER_HELPER, transformerHelper, TransformerHelper.class);
                variables.put(VAR_REQUEST_FORMATTER, transformerHelper.jsonRequestFormatter(operationRequested), JsonRequestFormatter.class);

                List<?> list = connectorInstance.getUcfExpressionEvaluator().evaluate(transformExpression, variables,
                        SchemaConstantsGenerated.C_ASYNC_PROVISIONING_REQUEST, "creating asynchronous provisioning request",
                        task, result);
                if (list.isEmpty()) {
                    throw new IllegalStateException("Transformational script returned no value");
                }
                if (list.size() > 1) {
                    throw new IllegalStateException("Transformational script returned more than single value: " + list);
                }
                Object o = list.get(0);
                if (o == null) {
                    // In the future we can call e.g. default request creator here
                    throw new IllegalStateException("Transformational script returned no value");
                } else if (o instanceof AsyncProvisioningRequest) {
                    return (AsyncProvisioningRequest) o;
                } else if (o instanceof String) {
                    return StringAsyncProvisioningRequest.of((String) o);
                } else {
                    throw new IllegalStateException("Transformational script should provide an AsyncProvisioningRequest but created " + MiscUtil.getClass(o) + " instead");
                }
            } else {
                return transformerHelper.applyPredefinedTransformation(operationRequested, SIMPLIFIED_JSON);
            }
        } catch (RuntimeException | SchemaException | ObjectNotFoundException | SecurityViolationException |
                CommunicationException | ConfigurationException | ExpressionEvaluationException | IOException e) {
            throw new SystemException("Couldn't evaluate message transformation expression: " + e.getMessage(), e);
        }
    }
}
