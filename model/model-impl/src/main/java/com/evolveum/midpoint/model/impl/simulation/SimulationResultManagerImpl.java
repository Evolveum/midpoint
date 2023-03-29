package com.evolveum.midpoint.model.impl.simulation;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.SIMULATION_RESULT_DEFAULT_TRANSACTION_ID;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.simulation.SimulationResultManager;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.merger.simulation.SimulationDefinitionMergeOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.SimulationResult;
import com.evolveum.midpoint.task.api.Task;
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

    /** Global definitions provided by the system configuration. */
    @NotNull private volatile List<SimulationDefinitionType> simulationDefinitions = new ArrayList<>();

    /** Global metric definitions provided by the system configuration. Keyed by identifier. Immutable. */
    @NotNull private volatile Map<String, SimulationMetricDefinitionType> explicitMetricDefinitions = new HashMap<>();

    @Override
    public @NotNull SimulationDefinitionType defaultDefinition() throws ConfigurationException {
        List<SimulationDefinitionType> allDefinitions = this.simulationDefinitions;
        if (allDefinitions.size() == 1) {
            return allDefinitions.get(0).clone(); // regardless of whether it's marked as default
        }
        List<SimulationDefinitionType> defaultOnes = allDefinitions.stream()
                .filter(SimulationDefinitionType::isDefault)
                .collect(Collectors.toList());
        if (defaultOnes.size() > 1) {
            throw new ConfigurationException("More than one default simulation definition present: " +
                    getIdentifiers(defaultOnes));
        } else if (defaultOnes.size() == 1) {
            return defaultOnes.get(0).clone();
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
    public @NotNull SimulationResultImpl createSimulationResult(
            @Nullable SimulationDefinitionType definition,
            @Nullable Task rootTask,
            @Nullable ConfigurationSpecificationType configurationSpecification,
            @NotNull OperationResult result)
            throws ConfigurationException {
        if (definition == null) {
            definition = defaultDefinition();
        }
        SimulationDefinitionType expandedDefinition = expandDefinition(definition, new HashSet<>());
        long now = clock.currentTimeMillis();
        SimulationResultType newResult = new SimulationResultType()
                .name(getResultName(expandedDefinition, now, rootTask))
                .definition(expandedDefinition.clone())
                .startTimestamp(XmlTypeConverter.createXMLGregorianCalendar(now))
                .rootTaskRef(
                        rootTask != null ? rootTask.getSelfReference() : null)
                .configurationUsed(
                        configurationSpecification != null ? configurationSpecification.clone() : null);

        String storedOid;
        try {
            storedOid = repository.addObject(newResult.asPrismObject(), null, result);
        } catch (ObjectAlreadyExistsException | SchemaException e) {
            // Neither of these exceptions should normally occur
            throw SystemException.unexpected(e, "when creating a simulation result");
        }

        LOGGER.debug("Created simulation result {}", newResult);

        return new SimulationResultImpl(storedOid, expandedDefinition);
    }

    /** TODO improve this method (e.g. by formatting the timestamp? by configuring the name? i18n?) */
    private String getResultName(SimulationDefinitionType expandedDefinition, long now, @Nullable Task rootTask) {
        String identifier = expandedDefinition.getIdentifier();
        StringBuilder sb = new StringBuilder();
        sb.append("Simulation result"); // TODO i18n
        if (identifier != null) {
            sb.append("(").append(identifier).append(") ");
        }
        sb.append(": ");
        if (rootTask != null) {
            sb.append(getOrig(rootTask.getName())).append(", ");
        }
        sb.append(XmlTypeConverter.createXMLGregorianCalendar(now));
        return sb.toString();
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

    /**
     * Removes all processed object records from this transaction - if there are any.
     *
     * If they exist, they were probably left there from the previously suspended (and now resumed) execution.
     */
    void deleteTransactionIfPresent(String simulationResultOid, String transactionId, OperationResult result) {
        try {
            repository.deleteSimulatedProcessedObjects(simulationResultOid, transactionId, result);
        } catch (SchemaException | ObjectNotFoundException e) {
            throw new SystemException(e);
        }
    }

    @Override
    public void update(@Nullable SystemConfigurationType value) {
        var configuration = value != null ? value.getSimulation() : null;
        if (configuration != null) {
            simulationDefinitions = CloneUtil.cloneCollectionMembers(configuration.getSimulation());
            explicitMetricDefinitions = configuration.getMetric().stream()
                    .map(def -> CloneUtil.toImmutable(def))
                    .collect(Collectors.toMap(
                            def -> def.getIdentifier(),
                            def -> def));
        } else {
            simulationDefinitions = List.of();
            explicitMetricDefinitions = Map.of();
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

    /** TEMPORARY. Retrieves stored deltas. May be replaced by something more general in the future. */
    @VisibleForTesting
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
    public SimulationResult getSimulationResult(@NotNull String resultOid, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        var simResult = repository
                .getObject(SimulationResultType.class, resultOid, null, result)
                .asObjectable();
        assertNotClosed(simResult);
        return new SimulationResultImpl(
                resultOid,
                Objects.requireNonNullElseGet(simResult.getDefinition(), SimulationDefinitionType::new));
    }

    private static void assertNotClosed(SimulationResultType simResult) {
        XMLGregorianCalendar endTimestamp = simResult.getEndTimestamp();
        stateCheck(endTimestamp == null,
                "Trying to update already closed simulation result %s (%s)", simResult, endTimestamp);
    }

    @NotNull Collection<SimulationMetricDefinitionType> getExplicitMetricDefinitions() {
        return explicitMetricDefinitions.values();
    }

    @Override
    public <X> X executeWithSimulationResult(
            @NotNull TaskExecutionMode mode,
            @Nullable SimulationDefinitionType simulationDefinition,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull SimulatedFunctionCall<X> functionCall)
            throws CommonException {

        argCheck(task.isTransient(), "Not supported for persistent tasks: %s", task);
        SimulationResultImpl simulationResult =
                createSimulationResult(simulationDefinition, null, mode.toConfigurationSpecification(), result);
        SimulationTransactionImpl simulationTransaction =
                simulationResult.getTransaction(SIMULATION_RESULT_DEFAULT_TRANSACTION_ID);
        simulationTransaction.open(result);

        TaskExecutionMode oldMode = task.setExecutionMode(mode);
        X returnValue;
        try {
            task.setSimulationTransaction(simulationTransaction);
            returnValue = functionCall.execute();
        } finally {
            task.setSimulationTransaction(null);
            task.setExecutionMode(oldMode);
            simulationTransaction.commit(result);
            simulationResult.close(result);
        }
        return returnValue;
    }

    @Override
    public @Nullable SimulationMetricDefinitionType getMetricDefinition(@NotNull String identifier) {
        return explicitMetricDefinitions.get(identifier);
    }

    @NotNull OpenResultTransactionsHolder getOpenResultTransactionsHolder() {
        return openResultTransactionsHolder;
    }
}
