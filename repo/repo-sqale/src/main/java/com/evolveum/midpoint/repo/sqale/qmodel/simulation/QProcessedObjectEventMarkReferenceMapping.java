package com.evolveum.midpoint.repo.sqale.qmodel.simulation;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.tag.QMarkMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.querydsl.core.types.Predicate;

public class QProcessedObjectEventMarkReferenceMapping extends QReferenceMapping<QProcessedObjectEventMarkReference, MProcessedObjectEventMarkReference,
    QProcessedObject, MProcessedObject> {


    private static final String TABLE_NAME = "m_processed_object_event_mark";

    public static QProcessedObjectEventMarkReferenceMapping instance;

    public static QProcessedObjectEventMarkReferenceMapping init(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QProcessedObjectEventMarkReferenceMapping(
                    TABLE_NAME, "srpoet", repositoryContext,
                    QMarkMapping::getInstance);
        }
        return getInstance();
    }

    public static QProcessedObjectEventMarkReferenceMapping getInstance() {
        return Objects.requireNonNull(instance);
    }

    protected  <TQ extends QObject<TR>, TR extends MObject> QProcessedObjectEventMarkReferenceMapping(
            String tableName,
            String defaultAliasName,
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier) {
        super(tableName, defaultAliasName, QProcessedObjectEventMarkReference.class,
                repositoryContext, targetMappingSupplier);
    }

    @Override
    protected QProcessedObjectEventMarkReference newAliasInstance(String alias) {
        return new QProcessedObjectEventMarkReference(alias, TABLE_NAME);
    }

    @Override
    public MProcessedObjectEventMarkReference newRowObject(MProcessedObject ownerRow) {
        var row = new MProcessedObjectEventMarkReference();
        row.ownerOid = ownerRow.ownerOid;
        row.ownerType = MObjectType.SIMULATION_RESULT;
        row.processedObjectCid = ownerRow.cid;
        return row;
    }

    @Override
    public BiFunction<QProcessedObject, QProcessedObjectEventMarkReference, Predicate> correlationPredicate() {
        return (a, r) -> a.ownerOid.eq(r.ownerOid)
                .and(a.cid.eq(r.processedObjectCid));
    }

}
