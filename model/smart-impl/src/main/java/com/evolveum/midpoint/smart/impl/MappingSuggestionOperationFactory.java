package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.impl.mappings.CategoricalAttributeRegistry;
import com.evolveum.midpoint.smart.impl.shadowsampling.ObjectsSamplerProvider;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaService;
import com.evolveum.midpoint.smart.impl.mappings.heuristics.HeuristicRuleMatcher;
import com.evolveum.midpoint.smart.impl.scoring.MappingScriptValidator;
import com.evolveum.midpoint.smart.impl.scoring.MappingsQualityAssessor;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetStatisticsType;

@Component
public class MappingSuggestionOperationFactory {

    private final MappingsQualityAssessor mappingsQualityAssessor;
    private final MappingScriptValidator mappingScriptValidator;
    private final ShadowsWithOwnersProvider shadowsWithOwnersProvider;
    private final ObjectsSamplerProvider samplerProvider;
    private final WellKnownSchemaService wellKnownSchemaService;
    private final HeuristicRuleMatcher heuristicRuleMatcher;
    private final CategoricalAttributeRegistry categoricalAttributeRegistry;

    public MappingSuggestionOperationFactory(MappingsQualityAssessor mappingsQualityAssessor,
            ShadowsWithOwnersProvider shadowsWithOwnersProvider,
            ObjectsSamplerProvider samplerProvider,
            MappingScriptValidator mappingScriptValidator,
            WellKnownSchemaService wellKnownSchemaService,
            HeuristicRuleMatcher heuristicRuleMatcher,
            CategoricalAttributeRegistry categoricalAttributeRegistry) {
        this.mappingsQualityAssessor = mappingsQualityAssessor;
        this.mappingScriptValidator = mappingScriptValidator;
        this.shadowsWithOwnersProvider = shadowsWithOwnersProvider;
        this.samplerProvider = samplerProvider;
        this.wellKnownSchemaService = wellKnownSchemaService;
        this.heuristicRuleMatcher = heuristicRuleMatcher;
        this.categoricalAttributeRegistry = categoricalAttributeRegistry;
    }

    MappingsSuggestionOperation create(ServiceClient client, String resourceOid,
            ResourceObjectTypeIdentification typeIdentification, CurrentActivityState<?> activityState,
            boolean isInbound, boolean useAiService,
            @Nullable ObjectSetStatisticsType objectTypeStatistics,
            int retryCount,
            Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {
        return MappingsSuggestionOperation.init(
                TypeOperationContext.init(client, resourceOid, typeIdentification, activityState, task, parentResult),
                this.mappingsQualityAssessor,
                this.mappingScriptValidator,
                this.shadowsWithOwnersProvider,
                this.samplerProvider,
                this.wellKnownSchemaService,
                this.heuristicRuleMatcher,
                this.categoricalAttributeRegistry,
                isInbound,
                useAiService,
                objectTypeStatistics,
                retryCount);
    }
}
