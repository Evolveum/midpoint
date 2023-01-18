package com.evolveum.midpoint.repo.sqale.qmodel.simulation;

import java.awt.desktop.QuitEvent;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.tag.QTagMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.querydsl.core.types.Predicate;

public class QProcessedObjectEventTagReferenceMapping extends QReferenceMapping<QProcessedObjectEventTagReference, MProcessedObjectEventTagReference,
    QProcessedObject, MProcessedObject> {


    private static final String TABLE_NAME = "m_processed_object_event_tag";

    public static QProcessedObjectEventTagReferenceMapping instance;

    public static QProcessedObjectEventTagReferenceMapping init(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QProcessedObjectEventTagReferenceMapping(
                    TABLE_NAME, "srpoet", repositoryContext,
                    QTagMapping::getInstance);
        }
        return getInstance();
    }

    public static QProcessedObjectEventTagReferenceMapping getInstance() {
        return Objects.requireNonNull(instance);
    }

    protected  <TQ extends QObject<TR>, TR extends MObject> QProcessedObjectEventTagReferenceMapping(
            String tableName,
            String defaultAliasName,
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier) {
        super(tableName, defaultAliasName, QProcessedObjectEventTagReference.class,
                repositoryContext, targetMappingSupplier);
    }

    @Override
    protected QProcessedObjectEventTagReference newAliasInstance(String alias) {
        return new QProcessedObjectEventTagReference(alias, TABLE_NAME);
    }

    @Override
    public MProcessedObjectEventTagReference newRowObject(MProcessedObject ownerRow) {
        var row = new MProcessedObjectEventTagReference();
        row.ownerOid = ownerRow.ownerOid;
        row.ownerType = MObjectType.SIMULATION_RESULT;
        row.processedObjectCid = ownerRow.cid;
        return row;
    }

    @Override
    public BiFunction<QProcessedObject, QProcessedObjectEventTagReference, Predicate> correlationPredicate() {
        return (a, r) -> a.ownerOid.eq(r.ownerOid)
                .and(a.cid.eq(r.processedObjectCid));
    }

}
