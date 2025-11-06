package com.evolveum.midpoint.smart.impl;

import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.impl.scoring.ObjectTypeFiltersValidator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

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
            Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        var ctx = OperationContext.init(client, resourceOid, objectClassName, task, parentResult);
        return new ObjectTypesSuggestionOperation(ctx, filtersValidator);
    }
}
