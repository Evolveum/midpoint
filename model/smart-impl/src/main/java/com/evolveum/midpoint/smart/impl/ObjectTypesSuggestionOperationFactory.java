package com.evolveum.midpoint.smart.impl;

import java.util.List;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.RegenerateMode;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.impl.scoring.ObjectTypeFiltersValidator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

@Component
public class ObjectTypesSuggestionOperationFactory {

    private final ObjectTypeFiltersValidator filtersValidator;

    public ObjectTypesSuggestionOperationFactory(ObjectTypeFiltersValidator filtersValidator) {
        this.filtersValidator = filtersValidator;
    }

    public ObjectTypesSuggestionOperation create(
            ServiceClient client,
            String resourceOid,
            QName objectClassName,
            @Nullable RegenerateMode regenerateMode,
            @Nullable List<ResourceObjectTypeDefinitionType> previousObjectTypes,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {
        var ctx = OperationContext.init(client, resourceOid, objectClassName, task, parentResult);
        return new ObjectTypesSuggestionOperation(ctx, filtersValidator, regenerateMode, previousObjectTypes);
    }
}
