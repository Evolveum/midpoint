package com.evolveum.midpoint.repo.sqale.qmodel.simulation;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType.*;

import java.util.Objects;

public class QProcessedObjectMapping extends QContainerMapping<SimulationResultProcessedObjectType, QProcessedObject, MProcessedObject, MSimulationResult> {

    public static final String DEFAULT_ALIAS_NAME = "po";

    public static final String PARTITION_PREFIX = "m_sr_processed_object_";

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
        //addItemMapping(F_TYPE, ));
        addItemMapping(F_TRANSACTION_ID, stringMapper(q -> q.transactionId));
        addItemMapping(F_STATE, enumMapper(q -> q.state));
        addRefMapping(F_EVENT_MARK_REF, QProcessedObjectEventMarkReferenceMapping.init(repositoryContext));
        addItemMapping(F_FOCUS_RECORD_ID, longMapper(q -> q.focusRecordId));
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
        return parseSchemaObject(row.fullObject, row.ownerOid + "," + row.cid);
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
        row.oid = SqaleUtils.oidToUuid(object.getOid());
        if (object.getName() != null) {
            row.nameOrig = object.getName().getOrig();
            row.nameNorm = object.getName().getNorm();
        }
        row.state = object.getState();

        if (object.getType() != null) {
            row.objectType = MObjectType.fromTypeQName(object.getType());
        }
        row.fullObject = createFullObject(object);
        row.transactionId = object.getTransactionId();
        row.focusRecordId = object.getFocusRecordId();
        // Before / After not serialized
        insert(row, jdbcSession);
        // We store event marks
        storeRefs(row, object.getEventMarkRef(), QProcessedObjectEventMarkReferenceMapping.getInstance(), jdbcSession);
        return row;
    }

    public static String partitionName(String oid) {
        return PARTITION_PREFIX + oid.replace('-', '_');
    }
}
