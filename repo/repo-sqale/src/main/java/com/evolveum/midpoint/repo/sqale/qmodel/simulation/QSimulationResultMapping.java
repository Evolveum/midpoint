package com.evolveum.midpoint.repo.sqale.qmodel.simulation;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType.*;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.delta.item.ContainerTableDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.mapping.ContainerTableRelationResolver;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleItemSqlMapper;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.task.QTaskMapping;
import com.evolveum.midpoint.repo.sqale.update.RootUpdateContext;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;

public class QSimulationResultMapping
        extends QAssignmentHolderMapping<SimulationResultType, QSimulationResult, MSimulationResult> {

    public static final String DEFAULT_ALIAS_NAME = "sr";

    private static QSimulationResultMapping instance;

    public static QSimulationResultMapping initSimulationResultMapping(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QSimulationResultMapping(repositoryContext);
        }
        return instance;
    }

    public static QSimulationResultMapping getSimulationResultMapping() {
        return Objects.requireNonNull(instance);
    }


    private QSimulationResultMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QSimulationResult.TABLE_NAME, DEFAULT_ALIAS_NAME,
                SimulationResultType.class, QSimulationResult.class, repositoryContext);
        var processedMapping = QProcessedObjectMapping.initProcessedResultMapping(repositoryContext);
        BiFunction<QSimulationResult, QProcessedObject, Predicate> processedJoin = (o, processed) -> o.oid.eq(processed.ownerOid);

        // We need special delta handler, so we can not use addContainerMapping
        addRelationResolver(F_PROCESSED_OBJECT,
                new ContainerTableRelationResolver<>(processedMapping, processedJoin));

        addItemMapping(F_PROCESSED_OBJECT, new SqaleItemSqlMapper<>(
                ctx -> new ProcessedObjectTableDeltaProcessor(ctx, processedMapping)));


        addNestedMapping(F_DEFINITION, SimulationDefinitionType.class)
            .addItemMapping(SimulationDefinitionType.F_USE_OWN_PARTITION_FOR_PROCESSED_OBJECTS, booleanMapper(q -> q.partitioned));

        // startTimestamp
        addItemMapping(F_START_TIMESTAMP, timestampMapper(q -> q.startTimestamp));
        addItemMapping(F_END_TIMESTAMP, timestampMapper(q -> q.endTimestamp));

        // endTimestamp
        addRefMapping(F_ROOT_TASK_REF,
                q -> q.rootTaskRefTargetOid,
                q -> q.rootTaskRefTargetType,
                q -> q.rootTaskRefRelationId,
                QTaskMapping::get);
    }

    @Override
    protected QSimulationResult newAliasInstance(String alias) {
        return new QSimulationResult(alias);
    }


    @Override
    public @NotNull Path<?>[] selectExpressions(QSimulationResult entity,
            Collection<SelectorOptions<GetOperationOptions>> options) {
        // We always want to know if result is partitioned
        return appendPaths(super.selectExpressions(entity,options), entity.partitioned);
    }

    @Override
    public @NotNull MSimulationResult toRowObjectWithoutFullObject(SimulationResultType schemaObject,
            JdbcSession jdbcSession) {
        MSimulationResult row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        if (schemaObject.getDefinition() != null) {
            row.partitioned = schemaObject.getDefinition().isUseOwnPartitionForProcessedObjects();
        }

        row.startTimestamp = MiscUtil.asInstant(schemaObject.getStartTimestamp());
        row.endTimestamp = MiscUtil.asInstant(schemaObject.getEndTimestamp());
        setReference(schemaObject.getRootTaskRef(),
                o -> row.rootTaskRefTargetOid = o,
                t -> row.rootTaskRefTargetType = t,
                r -> row.rootTaskRefRelationId = r);
        return row;
    }

    @Override
    public MSimulationResult newRowObject() {
        return new MSimulationResult();
    }

    @Override
    protected void customizeFullObjectItemsToSkip(PathSet mutableSet) {
        mutableSet.add(F_PROCESSED_OBJECT);
    }

    @Override
    public void storeRelatedEntities(@NotNull MSimulationResult row, @NotNull SimulationResultType schemaObject,
            @NotNull JdbcSession jdbcSession) throws SchemaException {
        super.storeRelatedEntities(row, schemaObject, jdbcSession);
        List<SimulationResultProcessedObjectType> processed = schemaObject.getProcessedObject();
        if (!processed.isEmpty()) {
            for (var c : processed) {
                QProcessedObjectMapping.getProcessedObjectMapping().insert(c, row, jdbcSession);
            }
        }
    }

    private static class ProcessedObjectTableDeltaProcessor extends ContainerTableDeltaProcessor<SimulationResultProcessedObjectType, QProcessedObject, MProcessedObject, QSimulationResult, MSimulationResult> {

        public ProcessedObjectTableDeltaProcessor(
                @NotNull SqaleUpdateContext<?, QSimulationResult, MSimulationResult> context,
                @NotNull QContainerMapping<SimulationResultProcessedObjectType, QProcessedObject, MProcessedObject, MSimulationResult> containerTableMapping) {
            super(context, containerTableMapping);
        }

        @Override
        public void setRealValues(Collection<?> values) throws SchemaException {
            if (values == null || values.isEmpty()) {
                // We are replacing with empty value, so we can drop partition if exists
                deleteOrDrop();

            } else {
                deleteRowsOnly();
                addRealValues(values);
            }
        }

        @Override
        public void delete() {
            deleteOrDrop();
        }

        private void deleteRowsOnly() {
            var c = getContainerTableMapping().defaultAlias();
            getContext().jdbcSession().newDelete(c)
                    .where(c.isOwnedBy(getContext().row()))
                    .execute();
        }

        private void deleteOrDrop() {
            if (!isPartitioned()) {
                // table is not partitioned
                deleteRowsOnly();
                return;
            }

            getContext().jdbcSession().executeStatement("DROP TABLE IF EXISTS " + QProcessedObjectMapping.partitionName(getRootObject().getOid()) + ";");

        }

        @SuppressWarnings("unchecked")

        private SimulationResultType getRootObject() {
            return ((RootUpdateContext<SimulationResultType, QSimulationResult, MSimulationResult>) getContext()).getPrismObject().asObjectable();
        }

        private boolean isPartitioned() {
            var root = getRootObject();
            if (root.getDefinition() == null) {
                return false;
            }
            return Boolean.TRUE.equals(root.getDefinition().isUseOwnPartitionForProcessedObjects());
        }
    }

    /**
     * returns false, reindex is not feasible for Simulation Result
     *
     * Simulation Result stores large quantity of data and forceReindex, operation is Read, Remove, Add
     * - which with SQL constraints would require reading whole simulation (and all results)
     * storing it in-memory and then dumping it in one transaction into repository which is currently not feasible.
     */
    @Override
    public boolean isReindexSupported() {
        return false;
    }
}
