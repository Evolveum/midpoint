package com.evolveum.midpoint.smart.impl;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.impl.scoring.MappingsQualityAssessor;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

@Component
public class MappingSuggestionOperationFactory {

    private final MappingsQualityAssessor mappingsQualityAssessor;

    public MappingSuggestionOperationFactory(MappingsQualityAssessor mappingsQualityAssessor) {
        this.mappingsQualityAssessor = mappingsQualityAssessor;
    }

    public MappingsSuggestionOperation create(ServiceClient client, String resourceOid,
            ResourceObjectTypeIdentification typeIdentification, CurrentActivityState<?> activityState, Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        return MappingsSuggestionOperation.init(client, resourceOid, typeIdentification, activityState,
                this.mappingsQualityAssessor, task, parentResult);
    }
}
