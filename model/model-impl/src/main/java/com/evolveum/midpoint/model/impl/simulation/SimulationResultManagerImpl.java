package com.evolveum.midpoint.model.impl.simulation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.util.CloneUtil;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

import com.evolveum.midpoint.schema.internals.InternalsConfig;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.model.api.simulation.SimulationResultContext;
import com.evolveum.midpoint.model.api.simulation.SimulationResultManager;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.schema.merger.simulation.SimulationDefinitionMergeOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

@Component
public class SimulationResultManagerImpl implements SimulationResultManager, SystemConfigurationChangeListener {

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repository;
    @Autowired private Clock clock;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;

    /** Global definitions provided by the system configuration. */
    @NotNull private volatile List<SimulationDefinitionType> definitions = new ArrayList<>();

    /** Primitive way of checking we do not write to closed results. */
    @VisibleForTesting
    @NotNull private final ClosedResultsChecker closedResultsChecker = new ClosedResultsChecker();

    @Override
    public SimulationDefinitionType defaultDefinition() throws ConfigurationException {
        List<SimulationDefinitionType> allDefinitions = this.definitions;
        if (allDefinitions.size() == 1) {
            return allDefinitions.get(0); // regardless of whether it's marked as default
        }
        List<SimulationDefinitionType> defaultOnes = allDefinitions.stream()
                .filter(SimulationDefinitionType::isDefault)
                .collect(Collectors.toList());
        if (defaultOnes.size() > 1) {
            throw new ConfigurationException("More than one default simulation definition present: " +
                    getIdentifiers(defaultOnes));
        } else if (defaultOnes.size() == 1) {
            return defaultOnes.get(0);
        }
        if (!allDefinitions.isEmpty()) {
            throw new ConfigurationException("Multiple simulation definitions present, none marked as default: " +
                    getIdentifiers(allDefinitions));
        }
        return new SimulationDefinitionType();
    }

    private static List<String> getIdentifiers(List<SimulationDefinitionType> definitions) {
        return definitions.stream()
                .map(SimulationDefinitionType::getIdentifier)
                .collect(Collectors.toList());
    }

    @Override
    public @NotNull SimulationResultContext newSimulationResult(
            @Nullable SimulationDefinitionType definition, @NotNull OperationResult result) throws ConfigurationException {
        if (definition == null) {
            definition = defaultDefinition();
        }
        SimulationDefinitionType expandedDefinition = expandDefinition(definition, new HashSet<>());
        long now = clock.currentTimeMillis();
        SimulationResultType newResult = new SimulationResultType()
                .name(getResultName(expandedDefinition, now))
                .definition(expandedDefinition.clone())
                .startTimestamp(XmlTypeConverter.createXMLGregorianCalendar(now))
                .useOwnPartitionForProcessedObjects(expandedDefinition.isUseOwnPartitionForProcessedObjects());

        String storedOid;
        try {
            storedOid = repository.addObject(newResult.asPrismObject(), null, result);
        } catch (ObjectAlreadyExistsException | SchemaException e) {
            // Neither of these exceptions should normally occur
            throw SystemException.unexpected(e, "when creating a simulation result");
        }

        return new SimulationResultContextImpl(this, storedOid);
    }

    /** TODO improve this method (e.g. by formatting the timestamp? by configuring the name?) */
    private String getResultName(SimulationDefinitionType expandedDefinition, long now) {
        String identifier = expandedDefinition.getIdentifier();
        String timeInfo = String.valueOf(now);
        if (identifier != null) {
            return String.format("Simulation result (%s): %s", identifier, timeInfo);
        } else {
            return String.format("Simulation result: %s", timeInfo);
        }
    }

    private SimulationDefinitionType expandDefinition(
            @NotNull SimulationDefinitionType defToExpand, @NotNull Set<String> identifiersSeen)
            throws ConfigurationException {
        String superId = defToExpand.getSuper();
        if (superId == null) {
            return defToExpand;
        }
        if (!identifiersSeen.add(superId)) {
            throw new ConfigurationException("A cycle in super-definition structure; identifiers seen: " + identifiersSeen);
        }
        SimulationDefinitionType superDef =
                expandDefinition(
                        findDefinitionById(superId),
                        identifiersSeen);
        SimulationDefinitionType expandedDef = defToExpand.clone();
        try {
            new SimulationDefinitionMergeOperation(expandedDef, superDef, null)
                    .execute();
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "when expanding simulation definition");
        }
        return expandedDef;
    }

    private SimulationDefinitionType findDefinitionById(@NotNull String id) throws ConfigurationException {
        List<SimulationDefinitionType> matching = definitions.stream()
                .filter(def -> id.equals(def.getIdentifier()))
                .collect(Collectors.toList());
        return MiscUtil.extractSingletonRequired(
                matching,
                () -> new ConfigurationException("Multiple simulation definitions with id '" + id + "' were found"),
                () -> new ConfigurationException("No simulation definition with id '" + id + "' was found"));
    }

    @Override
    public void closeSimulationResult(@NotNull ObjectReferenceType simulationResultRef, OperationResult result)
            throws ObjectNotFoundException {
        try {
            String oid = Objects.requireNonNull(simulationResultRef.getOid(), "No oid in simulationResultRef");
            closedResultsChecker.markClosed(oid);
            Collection<SimulationMetricValueType> metricValues = computeMetricsValues(oid, result);
            repository.modifyObject(
                    SimulationResultType.class,
                    oid,
                    PrismContext.get().deltaFor(SimulationResultType.class)
                            .item(SimulationResultType.F_END_TIMESTAMP)
                            .replace(clock.currentTimeXMLGregorianCalendar())
                            .item(SimulationResultType.F_METRIC)
                            .replaceRealValues(metricValues)
                            .asItemDeltas(),
                    result);
        } catch (SchemaException | ObjectAlreadyExistsException e) {
            throw SystemException.unexpected(e, "when closing the simulation result " + simulationResultRef);
        }
    }

    /** TODO implement iteratively + more efficiently */
    private Collection<SimulationMetricValueType> computeMetricsValues(String oid, OperationResult result)
            throws SchemaException {
        Map<String, Integer> metricMap = new HashMap<>();
        for (ProcessedObject<?> processedObject : getStoredProcessedObjects(oid, result)) {
            for (String eventTag : processedObject.getEventTags()) {
                metricMap.compute(
                        eventTag,
                        (tag, count) -> or0(count) + 1);
            }
        }
        return metricMap.entrySet().stream()
                .map(e ->
                        new SimulationMetricValueType()
                                .identifier(e.getKey())
                                .value(e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public void update(@Nullable SystemConfigurationType value) {
        var configuration = value != null ? value.getSimulation() : null;
        if (configuration != null) {
            definitions = CloneUtil.cloneCollectionMembers(configuration.getDefinition());
        }
    }

    @PostConstruct
    public void init() {
        systemConfigurationChangeDispatcher.registerListener(this);
    }

    @PreDestroy
    public void shutdown() {
        systemConfigurationChangeDispatcher.unregisterListener(this);
    }

    void storeProcessedObject(
            @NotNull String oid, @NotNull SimulationResultProcessedObjectType processedObject, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        try {
            closedResultsChecker.checkNotClosed(oid);
            List<ItemDelta<?, ?>> modifications = PrismContext.get().deltaFor(SimulationResultType.class)
                    .item(SimulationResultType.F_PROCESSED_OBJECT)
                    .add(processedObject.asPrismContainerValue())
                    .asItemDeltas();
            repository.modifyObject(SimulationResultType.class, oid, modifications, result);
        } catch (ObjectAlreadyExistsException e) {
            throw SystemException.unexpected(e, "when storing processed object information");
        }
    }

    /** TEMPORARY. Retrieves stored deltas. May be replaced by something more general in the future. */
    @Override
    public @NotNull List<ProcessedObject<?>> getStoredProcessedObjects(@NotNull String oid, OperationResult result)
            throws SchemaException {
        ObjectQuery query = PrismContext.get().queryFor(SimulationResultProcessedObjectType.class)
                .ownerId(oid)
                .build();
        List<SimulationResultProcessedObjectType> processedObjectBeans =
                repository.searchContainers(SimulationResultProcessedObjectType.class, query, null, result);
        List<ProcessedObject<?>> processedObjects = new ArrayList<>();
        for (SimulationResultProcessedObjectType processedObjectBean : processedObjectBeans) {
            processedObjects.add(
                    ProcessedObject.parse(processedObjectBean));
        }
        return processedObjects;
    }

    @Override
    public SimulationResultContext newSimulationContext(@NotNull String resultOid) {
        return new SimulationResultContextImpl(this, resultOid);
    }

    /**
     * Checks that we do not write into closed {@link SimulationResultType}.
     * Assumes {@link InternalsConfig#consistencyChecks} be `true`, i.e. usually not employed in production.
     * (Does not detect problems when in cluster, anyway.)
     *
     * "Real" testing by fetching the whole {@link SimulationResultType} from the repository would be too slow and inefficient.
     */
    @VisibleForTesting
    private static class ClosedResultsChecker {

        private static final long DELETE_AFTER = 3600_000;

        /** Value is when the result was closed. */
        private final Map<String, Long> closedResults = new ConcurrentHashMap<>();

        void markClosed(String oid) {
            if (!InternalsConfig.consistencyChecks) {
                return;
            }
            long now = System.currentTimeMillis();
            closedResults.put(oid, now);

            // Deleting obsolete results - just to avoid growing the map forever, if turned on by chance in production.
            closedResults.entrySet()
                    .removeIf(e -> e.getValue() < now - DELETE_AFTER);
        }

        void checkNotClosed(String oid) {
            if (!InternalsConfig.consistencyChecks) {
                return;
            }
            stateCheck(!closedResults.containsKey(oid), "Trying to append to already closed simulation result: %s", oid);
        }
    }
}
