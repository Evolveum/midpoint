package com.evolveum.midpoint.repo.sqale.qmodel.simulation;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType.*;

import java.util.Objects;

public class QProcessedObjectMapping extends QContainerMapping<SimulationResultProcessedObjectType, QProcessedObject, MProcessedObject, MSimulationResult> {

    public static final String DEFAULT_ALIAS_NAME = "po";

    private static QProcessedObjectMapping instance;

    public static @NotNull QProcessedObjectMapping initProcessedResultMapping(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QProcessedObjectMapping(repositoryContext);
        }
        return instance;
    }

    public static QProcessedObjectMapping getProcessedObjectMapping() {
        return Objects.requireNonNull(instance);
    }

    public QProcessedObjectMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QProcessedObject.TABLE_NAME,
                DEFAULT_ALIAS_NAME,
                SimulationResultProcessedObjectType.class,
                QProcessedObject.class,
                repositoryContext);

        addRelationResolver(PrismConstants.T_PARENT,
                // mapping supplier is used to avoid cycles in the initialization code
                TableRelationResolver.usingJoin(
                        QSimulationResultMapping::getSimulationResultMapping,
                        (q, p) -> q.ownerOid.eq(p.oid)));
        // This could be done also by auditRefMapping, but we currently do not support filters which allows searching for target name
        // without dereferencing, which may be unusable for new simulated objects

        addItemMapping(F_OID, uuidMapper(q -> q.oid));
        addItemMapping(F_NAME, polyStringMapper(
                q -> q.nameOrig, q -> q.nameNorm));
        // addItemMapping(F_OBJECT_TYPE, ));

        addItemMapping(F_STATE, enumMapper(q -> q.state));
        addItemMapping(F_METRIC_IDENTIFIER, multiStringMapper(q -> q.metricIdentifiers));
    }

    /*
    @Override
    public @NotNull Path<?>[] selectExpressions(QProcessedObject row,
            Collection<SelectorOptions<GetOperationOptions>> options) {
        return new Path<?>[] {row.ownerOid, row.cid, row.fullObject};
    }
    */

    @Override
    public SimulationResultProcessedObjectType toSchemaObject(MProcessedObject row) throws SchemaException {
        var object = parseSchemaObject(row.fullObject, row.ownerOid + "," + row.cid);
        if (ShadowType.COMPLEX_TYPE.equals(object.getType())) {
            System.out.println("before = " + object.getBefore() + ", after = " + object.getAfter());
        }
        return object;
    }

    @Override
    public MProcessedObject newRowObject() {
        return new MProcessedObject();
    }

    @Override
    protected QProcessedObject newAliasInstance(String alias) {
        return new QProcessedObject(alias);
    }

    @Override
    public MProcessedObject newRowObject(MSimulationResult ownerRow) {
        MProcessedObject row = newRowObject();
        row.ownerOid = ownerRow.oid;
        return row;
    }

    @Override
    protected PathSet fullObjectItemsToSkip() {
        // Do not store full objects (TEMPORARILY DISABLED because of the need to apply definitions in shadow deltas)
        // return PathSet.of(F_BEFORE, F_AFTER);
        return PathSet.empty();
    }

    @Override
    public MProcessedObject insert(SimulationResultProcessedObjectType object, MSimulationResult ownerRow,
            JdbcSession jdbcSession) throws SchemaException {
        MProcessedObject row = initRowObject(object, ownerRow);
        //row.oid
        row.oid = SqaleUtils.oidToUUid(object.getOid());
        if (object.getName() != null) {
            row.nameOrig = object.getName().getOrig();
            row.nameNorm = object.getName().getNorm();
        }
        row.state = object.getState();

        row.metricIdentifiers = stringsToArray(object.getMetricIdentifier());
        row.fullObject = createFullObject(object);

        // Before / After not serialized
        insert(row, jdbcSession);
        return row;
    }
}
