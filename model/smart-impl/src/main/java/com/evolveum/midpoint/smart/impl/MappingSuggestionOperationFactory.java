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
import org.jetbrains.annotations.Nullable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsSuggestionFiltersType;

@Component
public class MappingSuggestionOperationFactory {

    private final MappingsQualityAssessor mappingsQualityAssessor;
    private final OwnedShadowsProvider ownedShadowsProvider;

    public MappingSuggestionOperationFactory(MappingsQualityAssessor mappingsQualityAssessor,
            OwnedShadowsProvider ownedShadowsProvider) {
        this.mappingsQualityAssessor = mappingsQualityAssessor;
        this.ownedShadowsProvider = ownedShadowsProvider;
    }

    public MappingsSuggestionOperation create(ServiceClient client, String resourceOid,
            ResourceObjectTypeIdentification typeIdentification, CurrentActivityState<?> activityState,
            MappingsSuggestionFiltersType filters, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        return MappingsSuggestionOperation.init(client, resourceOid, typeIdentification, activityState,
                this.mappingsQualityAssessor, this.ownedShadowsProvider, filters, task, parentResult);
    }
}
