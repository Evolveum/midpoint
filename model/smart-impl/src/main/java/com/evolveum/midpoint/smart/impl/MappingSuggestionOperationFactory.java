package com.evolveum.midpoint.smart.impl;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.impl.mappings.CategoricalAttributeRegistry;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaService;
import com.evolveum.midpoint.smart.impl.mappings.heuristics.HeuristicRuleMatcher;
import com.evolveum.midpoint.smart.impl.scoring.MappingScriptValidator;
import com.evolveum.midpoint.smart.impl.scoring.MappingsQualityAssessor;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;

@Component
public class MappingSuggestionOperationFactory {

    private final MappingsQualityAssessor mappingsQualityAssessor;
    private final MappingScriptValidator mappingScriptValidator;
    private final OwnedShadowsProvider ownedShadowsProvider;
    private final WellKnownSchemaService wellKnownSchemaService;
    private final HeuristicRuleMatcher heuristicRuleMatcher;
    private final CategoricalAttributeRegistry categoricalAttributeRegistry;

    public MappingSuggestionOperationFactory(MappingsQualityAssessor mappingsQualityAssessor,
            MappingScriptValidator mappingScriptValidator,
            OwnedShadowsProvider ownedShadowsProvider,
            WellKnownSchemaService wellKnownSchemaService,
            HeuristicRuleMatcher heuristicRuleMatcher,
            CategoricalAttributeRegistry categoricalAttributeRegistry) {
        this.mappingsQualityAssessor = mappingsQualityAssessor;
        this.mappingScriptValidator = mappingScriptValidator;
        this.ownedShadowsProvider = ownedShadowsProvider;
        this.wellKnownSchemaService = wellKnownSchemaService;
        this.heuristicRuleMatcher = heuristicRuleMatcher;
        this.categoricalAttributeRegistry = categoricalAttributeRegistry;
    }

    public MappingsSuggestionOperation create(ServiceClient client, String resourceOid,
            ResourceObjectTypeIdentification typeIdentification, CurrentActivityState<?> activityState,
            boolean isInbound, boolean useAiService,
            @Nullable ShadowObjectClassStatisticsType objectTypeStatistics,
            int retryCount,
            Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        return MappingsSuggestionOperation.init(
                TypeOperationContext.init(client, resourceOid, typeIdentification, activityState, task, parentResult),
                this.mappingsQualityAssessor,
                this.mappingScriptValidator,
                this.ownedShadowsProvider,
                this.wellKnownSchemaService,
                this.heuristicRuleMatcher,
                this.categoricalAttributeRegistry,
                isInbound,
                useAiService,
                objectTypeStatistics,
                retryCount);
    }
}
