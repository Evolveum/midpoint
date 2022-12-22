package com.evolveum.midpoint.repo.sqale.qmodel.simulation;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.accesscert.QAccessCertificationCaseMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType.*;

public class QSimulationResultMapping extends QObjectMapping<SimulationResultType, QSimulationResult, MSimulationResult> {

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
        addContainerTableMapping(F_PROCESSED_OBJECT,
                QProcessedObjectMapping.initProcessedResultMapping(repositoryContext),
                joinOn((o, processed) -> o.oid.eq(processed.ownerOid)));
        addItemMapping(F_USE_OWN_PARTITION_FOR_PROCESSED_OBJECTS, booleanMapper(q -> q.partitioned));
    }

    @Override
    protected QSimulationResult newAliasInstance(String alias) {
        return new QSimulationResult(alias);
    }


    @Override
    public @NotNull MSimulationResult toRowObjectWithoutFullObject(SimulationResultType schemaObject,
            JdbcSession jdbcSession) {
        MSimulationResult row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        //row.partitioned = schemaObject.isUseOwnPartitionForProcessedObjects();
        return row;
    }

    @Override
    public MSimulationResult newRowObject() {
        return new MSimulationResult();
    }

    @Override
    protected PathSet fullObjectItemsToSkip() {
        return PathSet.of(SimulationResultType.F_PROCESSED_OBJECT);
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
}
