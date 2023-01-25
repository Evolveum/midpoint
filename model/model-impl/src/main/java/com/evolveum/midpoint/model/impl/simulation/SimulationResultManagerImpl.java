package com.evolveum.midpoint.model.impl.simulation;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.simulation.SimulationResultContext;
import com.evolveum.midpoint.model.api.simulation.SimulationResultManager;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.merger.simulation.SimulationDefinitionMergeOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.simulation.SimulationMetricComputer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class SimulationResultManagerImpl implements SimulationResultManager, SystemConfigurationChangeListener {

    private static final Trace LOGGER = TraceManager.getTrace(SimulationResultManagerImpl.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repository;
    @Autowired private Clock clock;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;
    @Autowired private OpenResultTransactionsHolder openResultTransactionsHolder;
    @Autowired private PrismContext prismContext;

    /** Global definitions provided by the system configuration. */
    @NotNull private volatile List<SimulationDefinitionType> simulationDefinitions = new ArrayList<>();

    /** Global metric definitions provided by the system configuration. */
    @NotNull private volatile List<SimulationMetricDefinitionType> metricDefinitions = new ArrayList<>();

    /** Primitive way of checking we do not write to closed results. */
    @VisibleForTesting
    @NotNull private final ClosedResultsChecker closedResultsChecker = new ClosedResultsChecker();

    @Override
    public SimulationDefinitionType defaultDefinition() throws ConfigurationException {
        List<SimulationDefinitionType> allDefinitions = this.simulationDefinitions;
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
    public @NotNull SimulationResultContext openNewSimulationResult(
            @Nullable SimulationDefinitionType definition,
            @Nullable String rootTaskOid,
            @Nullable ConfigurationSpecificationType configurationSpecification,
            @NotNull OperationResult result)
            throws ConfigurationException {
        if (definition == null) {
            definition = defaultDefinition();
        }
        SimulationDefinitionType expandedDefinition = expandDefinition(definition, new HashSet<>());
        long now = clock.currentTimeMillis();
        SimulationResultType newResult = new SimulationResultType()
                .name(getResultName(expandedDefinition, now))
                .definition(expandedDefinition.clone())
                .startTimestamp(XmlTypeConverter.createXMLGregorianCalendar(now))
                .rootTaskRef(
                        rootTaskOid != null ? createObjectRef(rootTaskOid, ObjectTypes.TASK) : null)
                .configurationUsed(
                        configurationSpecification != null ? configurationSpecification.clone() : null);

        String storedOid;
        try {
            storedOid = repository.addObject(newResult.asPrismObject(), null, result);
        } catch (ObjectAlreadyExistsException | SchemaException e) {
            // Neither of these exceptions should normally occur
            throw SystemException.unexpected(e, "when creating a simulation result");
        }

        return new SimulationResultContextImpl(this, storedOid);
    }

    /** TODO improve this method (e.g. by formatting the timestamp? by configuring the name? i18n?) */
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
        List<SimulationDefinitionType> matching = simulationDefinitions.stream()
                .filter(def -> id.equals(def.getIdentifier()))
                .collect(Collectors.toList());
        return MiscUtil.extractSingletonRequired(
                matching,
                () -> new ConfigurationException("Multiple simulation definitions with id '" + id + "' were found"),
                () -> new ConfigurationException("No simulation definition with id '" + id + "' was found"));
    }

    @Override
    public void closeSimulationResult(@NotNull String simulationResulOid, Task task, OperationResult result)
            throws ObjectNotFoundException {
        try {
            closedResultsChecker.markClosed(simulationResulOid);
            // Note that all transactions should be already committed and thus deleted from the holder.
            // So this is just the housekeeping for unusual situations.
            openResultTransactionsHolder.removeSimulationResult(simulationResulOid);
            repository.modifyObject(
                    SimulationResultType.class,
                    simulationResulOid,
                    PrismContext.get().deltaFor(SimulationResultType.class)
                            .item(SimulationResultType.F_END_TIMESTAMP)
                            .replace(clock.currentTimeXMLGregorianCalendar())
                            .asItemDeltas(),
                    result);
        } catch (ObjectNotFoundException e) {
            throw e;
        } catch (CommonException e) {
            // TODO do we want to propagate some of these exceptions upwards in their original form?
            throw new SystemException("Couldn't close simulation result " + simulationResulOid + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void openSimulationResultTransaction(
            @NotNull String simulationResultOid, @NotNull String transactionId, OperationResult result) {

        LOGGER.trace("Opening simulation result transaction {}:{}", simulationResultOid, transactionId);

        deleteTransactionIfPresent(simulationResultOid, transactionId, result);

        openResultTransactionsHolder.removeTransaction(simulationResultOid, transactionId);
    }

    /**
     * Removes all processed object records from this transaction - if there are any.
     *
     * If they exist, they were probably left there from the previously suspended (and now resumed) execution.
     */
    private void deleteTransactionIfPresent(String simulationResultOid, String transactionId, OperationResult result) {
        // TODO implement
    }

    @Override
    public void commitSimulationResultTransaction(
            @NotNull String simulationResultOid, @NotNull String transactionId, OperationResult result) {
        try {
            LOGGER.trace("Committing simulation result transaction {}:{}", simulationResultOid, transactionId);

            repository.modifyObjectDynamically(
                    SimulationResultType.class,
                    simulationResultOid,
                    null,
                    oldResult ->
                        prismContext.deltaFor(SimulationResultType.class)
                                .item(SimulationResultType.F_METRIC)
                                .replaceRealValues(
                                        computeUpdatedMetricsValues(simulationResultOid, oldResult.getMetric(), transactionId))
                                .asItemDeltas(),
                    null,
                    result);
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
            throw SystemException.unexpected(e, "when committing simulation result transaction");
        }

        openResultTransactionsHolder.removeTransaction(simulationResultOid, transactionId);
    }

    /**
     * Adds current in-memory metric values for the transaction being committed to the (aggregated) metrics values
     * that will go to the result.
     */
    private List<SimulationMetricValuesType> computeUpdatedMetricsValues(
            String simulationResultOid, List<SimulationMetricValuesType> old, String transactionId) {
        List<SimulationMetricValuesType> current =
                openResultTransactionsHolder.getMetricsValues(simulationResultOid, transactionId);
        List<SimulationMetricValuesType> sum = SimulationMetricComputer.add(old, current);
        // TODO consider removal of the following logging call (too verbose)
        LOGGER.trace("Computed updated metrics for {}:{}:\n OLD:\n{}\n CURRENT:\n{}\n SUM:\n{}",
                simulationResultOid, transactionId,
                DebugUtil.debugDumpLazily(old), DebugUtil.debugDumpLazily(current), DebugUtil.debugDumpLazily(sum));
        return sum;
    }

    @Override
    public void update(@Nullable SystemConfigurationType value) {
        var configuration = value != null ? value.getSimulation() : null;
        if (configuration != null) {
            simulationDefinitions = CloneUtil.cloneCollectionMembers(configuration.getSimulation());
            metricDefinitions = CloneUtil.cloneCollectionMembers(configuration.getMetric());
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
            @NotNull String oid,
            @NotNull String transactionId,
            @NotNull ProcessedObjectImpl<?> processedObject,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            LOGGER.trace("Storing processed object into {}:{}: {}", oid, transactionId, processedObject);
            closedResultsChecker.checkNotClosed(oid);
            SimulationResultProcessedObjectType processedObjectBean = processedObject.toBean();
            setTransactionIdSafely(processedObjectBean, transactionId);
            List<ItemDelta<?, ?>> modifications = PrismContext.get().deltaFor(SimulationResultType.class)
                    .item(SimulationResultType.F_PROCESSED_OBJECT)
                    .add(processedObjectBean.asPrismContainerValue())
                    .asItemDeltas();
            repository.modifyObject(SimulationResultType.class, oid, modifications, result);
            openResultTransactionsHolder.addProcessedObject(oid, transactionId, processedObject, task, result);
        } catch (CommonException e) {
            // TODO which exception to treat?
            throw SystemException.unexpected(e, "when storing processed object information");
        }
    }

    private void setTransactionIdSafely(SimulationResultProcessedObjectType processedObject, String newValue) {
        String existingValue = processedObject.getTransactionId();
        if (existingValue != null) {
            stateCheck(existingValue.equals(newValue),
                    "Attempted to overwrite transaction ID (%s -> %s) in %s",
                    existingValue, newValue, processedObject);
        } else {
            processedObject.setTransactionId(newValue);
        }
    }

    /** TEMPORARY. Retrieves stored deltas. May be replaced by something more general in the future. */
    @Override
    public @NotNull List<ProcessedObjectImpl<?>> getStoredProcessedObjects(@NotNull String oid, OperationResult result)
            throws SchemaException {
        ObjectQuery query = PrismContext.get().queryFor(SimulationResultProcessedObjectType.class)
                .ownerId(oid)
                .build();
        List<SimulationResultProcessedObjectType> processedObjectBeans =
                repository.searchContainers(SimulationResultProcessedObjectType.class, query, null, result);
        List<ProcessedObjectImpl<?>> processedObjects = new ArrayList<>();
        for (SimulationResultProcessedObjectType processedObjectBean : processedObjectBeans) {
            processedObjects.add(
                    ProcessedObjectImpl.parse(processedObjectBean));
        }
        return processedObjects;
    }

    @Override
    public SimulationResultContext newSimulationContext(@NotNull String resultOid) {
        return new SimulationResultContextImpl(this, resultOid);
    }

    @NotNull List<SimulationMetricDefinitionType> getMetricDefinitions() {
        return metricDefinitions;
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
