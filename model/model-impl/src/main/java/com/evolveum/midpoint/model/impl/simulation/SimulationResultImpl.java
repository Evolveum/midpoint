package com.evolveum.midpoint.model.impl.simulation;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MarkTypeUtil;
import com.evolveum.midpoint.task.api.SimulationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Implementation note: must be thread-safe.
 */
public class SimulationResultImpl implements SimulationResult {

    private static final Trace LOGGER = TraceManager.getTrace(SimulationResultImpl.class);

    private final @NotNull String oid;
    /** The definition, immutable. */
    private final @NotNull SimulationDefinitionType simulationDefinition;

    /**
     * Whether given metric is enabled or not - serves as a cache.
     */
    private final Map<SimulationMetricReference, Boolean> metricEnabledMap = new ConcurrentHashMap<>();

    SimulationResultImpl(
            @NotNull String resultOid, @NotNull SimulationDefinitionType simulationDefinition) {
        this.oid = resultOid;
        this.simulationDefinition = CloneUtil.toImmutable(simulationDefinition);
    }

    @Override
    public SimulationTransactionImpl getTransaction(@NotNull String transactionId) {
        return new SimulationTransactionImpl(this, transactionId);
    }

    @Override
    public void close(OperationResult result) throws ObjectNotFoundException {
        try {
            ClosedResultsChecker.INSTANCE.markClosed(oid);
            // Note that all transactions should be already committed and thus deleted from the holder.
            // So this is just the housekeeping for unusual situations.
            ModelBeans.get().simulationResultManager.getOpenResultTransactionsHolder()
                    .removeWholeResult(oid);
            ModelBeans.get().cacheRepositoryService.modifyObject(
                    SimulationResultType.class,
                    oid,
                    PrismContext.get().deltaFor(SimulationResultType.class)
                            .item(SimulationResultType.F_END_TIMESTAMP)
                            .replace(ModelBeans.get().clock.currentTimeXMLGregorianCalendar())
                            .asItemDeltas(),
                    result);
        } catch (ObjectNotFoundException e) {
            throw e;
        } catch (CommonException e) {
            // TODO do we want to propagate some of these exceptions upwards in their original form?
            throw new SystemException("Couldn't close simulation result " + this + ": " + e.getMessage(), e);
        }
    }

    @Override
    public @NotNull String getResultOid() {
        return oid;
    }

    @Override
    public @NotNull SimulationDefinitionType getSimulationDefinition() {
        return simulationDefinition;
    }

    boolean isCustomMetricEnabled(@NotNull SimulationMetricDefinitionType metricDefinition) {
        return metricEnabledMap.computeIfAbsent(
                SimulationMetricReference.forMetricId(metricDefinition.getIdentifier()),
                (r) -> computeCustomMetricEnabled(metricDefinition));
    }

    public boolean isEventMarkEnabled(@NotNull MarkType eventMark) {
        return metricEnabledMap.computeIfAbsent(
                SimulationMetricReference.forMark(eventMark.getOid()),
                (r) -> computeMarkEnabled(eventMark));
    }

    private boolean computeCustomMetricEnabled(SimulationMetricDefinitionType definition) {
        boolean enabledByDefault = !Boolean.FALSE.equals(definition.isEnabledByDefault());
        SimulationMetricsUseType metricUse = simulationDefinition.getMetrics();
        if (metricUse == null) {
            return enabledByDefault;
        }
        SimulationOtherMetricsUseType customMetricsUse = metricUse.getMetrics();
        if (customMetricsUse != null) {
            String id = Objects.requireNonNull(definition.getIdentifier());
            for (String included : customMetricsUse.getInclude()) {
                if (id.equals(included)) {
                    return true;
                }
            }
            for (String excluded : customMetricsUse.getExclude()) {
                if (id.equals(excluded)) {
                    return false;
                }
            }
        }
        return enabledByDefault;
    }

    private boolean computeMarkEnabled(MarkType eventMark) {
        boolean enabledByDefault = !Boolean.FALSE.equals(MarkTypeUtil.isEnabledByDefault(eventMark));
        SimulationMetricsUseType metricUse = simulationDefinition.getMetrics();
        SimulationEventMarksUseType marksUse = metricUse != null ? metricUse.getEventMarks() : null;
        if (marksUse != null) {
            try {
                MatchingRuleRegistry matchingRuleRegistry = SchemaService.get().matchingRuleRegistry();
                for (SearchFilterType includeFilter : marksUse.getInclude()) {
                    var filter = PrismContext.get().getQueryConverter().parseFilter(includeFilter, MarkType.class);
                    if (filter.match(eventMark.asPrismContainerValue(), matchingRuleRegistry)) {
                        LOGGER.trace("computeMarkEnabled: Mark {} is enabled by include filter: {}", eventMark, filter);
                        return true;
                    }
                }
                for (SearchFilterType excludeFilter : marksUse.getExclude()) {
                    var filter = PrismContext.get().getQueryConverter().parseFilter(excludeFilter, MarkType.class);
                    if (filter.match(eventMark.asPrismContainerValue(), matchingRuleRegistry)) {
                        LOGGER.trace("computeMarkEnabled: Mark {} is disabled by exclude filter: {}", eventMark, filter);
                        return false;
                    }
                }
            } catch (SchemaException e) {
                throw new SystemException(
                        String.format("Couldn't evaluate event mark filter for %s: %s", eventMark, e.getMessage()), e);
            }
        }
        LOGGER.trace("computeMarkEnabled: Mark {} is enabled by default: {}", eventMark, enabledByDefault);
        return enabledByDefault;
    }
}
