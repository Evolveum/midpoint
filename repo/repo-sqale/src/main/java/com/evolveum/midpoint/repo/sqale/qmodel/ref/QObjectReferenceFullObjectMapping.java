package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QSeparatelySerializedItem;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class QObjectReferenceFullObjectMapping<OS extends ObjectType, OQ extends QObject<OR>, OR extends MObject> extends QObjectReferenceMapping<OS, OQ, OR>
        implements QSeparatelySerializedItem<QObjectReferenceWithMeta<OR>,MReference> {

    private Lazy<PrismReferenceDefinition> itemDefinition;
    private final ItemPath path;

    public  <TQ extends QObject<TR>, TR extends MObject>  QObjectReferenceFullObjectMapping(
            Class<? extends ObjectType> baseType,
            ItemPath path,
            String tableName,
            String defaultAliasName,
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier,
            @Nullable Supplier<QueryTableMapping<OS, OQ, OR>> ownerMappingSupplier,
            @Nullable BiFunction<QObjectReference<OR>, OQ, Predicate> ownerJoin,
            @Nullable Class<?> ownerType, @Nullable ItemPath referencePath) {
        super(tableName, defaultAliasName, repositoryContext, (Class) QObjectReferenceWithMeta.class, targetMappingSupplier,
                ownerMappingSupplier, ownerJoin, ownerType, referencePath);
        this.path = path;
        this.itemDefinition = Lazy.from(() -> PrismContext.get().getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(baseType).findItemDefinition(path, PrismReferenceDefinition.class));
    }

    @Override
    protected QObjectReferenceWithMeta<OR> newAliasInstance(String alias) {
        return new QObjectReferenceWithMeta<>(alias, tableName());
    }

    @Override
    public MReference newRowObject(MObject ownerRow) {
        var row = new MObjectReferenceWithMeta();
        row.ownerOid = ownerRow.oid;
        row.ownerType = ownerRow.objectType;
        return row;
    }

    @Override
    protected void initRowObject(MReference row, ObjectReferenceType reference) throws SchemaException {
        super.initRowObject(row, reference);
        var casted = (MObjectReferenceWithMeta) row;
        casted.fullObject = createFullObject(reference);
    }

    public <C extends Containerable> byte[] createFullObject(ObjectReferenceType ref) throws SchemaException {
        var pref = ref.asReferenceValue();
        ObjectTypeUtil.normalizeRelation(pref, SchemaService.get().relationRegistry());
        return repositoryContext().createStringSerializer()
                .itemsToSkip(fullObjectItemsToSkip())
                .definition(itemDefinition.get())
                .options(SerializationOptions
                        .createSerializeReferenceNamesForNullOids()
                        .skipIndexOnly(true)
                        .skipTransient(true)
                        .skipWhitespaces(true))
                .serialize(pref)
                .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public MReference insert(ObjectReferenceType reference, OR ownerRow, JdbcSession jdbcSession) throws SchemaException {
        var inserted =  super.insert(reference, ownerRow, jdbcSession);
        return inserted;
    }

    @Override
    public void afterModify(SqaleUpdateContext<ObjectReferenceType, QObjectReference<OR>, MReference> updateContext) throws SchemaException {
        super.afterModify(updateContext);
    }

    @Override
    public boolean hasFullObject(Tuple row, QObjectReferenceWithMeta<OR> path) {
        return row.get(path.fullObject) != null;
    }

    @Override
    public Path<?>[] fullObjectExpressions(QObjectReferenceWithMeta<OR> base) {
        return new Path[] {base.ownerOid, base.fullObject};
    }

    @Override
    public Class<? extends Item<? extends PrismValue, ?>> getPrismItemType() {
        return (Class) PrismReference.class;
    }

    @Override
    public Predicate allOwnedBy(QObjectReferenceWithMeta<OR> q, Collection<UUID> oidList) {
        return q.ownerOid.in(oidList);
    }

    @Override
    public ObjectReferenceType toSchemaObject(MReference row) throws SchemaException {
        Referencable parsed = parseSchemaObject(((MObjectReferenceWithMeta) row).fullObject,
            "Reference", ObjectReferenceType.class);
        var ort = new ObjectReferenceType();
        ort.setupReferenceValue(parsed.asReferenceValue());
        return ort;
    }

    @Override
    public PrismReferenceValue toSchemaObjectEmbedded(Tuple row, QObjectReferenceWithMeta<OR> alias) throws SchemaException {
        byte[] fullObject = row.get(alias.fullObject);
        Referencable parsed = parseSchemaObject(fullObject, "Reference", ObjectReferenceType.class);
        return parsed.asReferenceValue();
    }

    @Override
    public UUID getOwner(Tuple row, QObjectReferenceWithMeta<OR> path) {
        return row.get(path.ownerOid);
    }

    @Override
    public QObjectReferenceWithMeta<OR> createAlias() {
        return newAliasInstance(defaultAliasName());
    }

    @Override
    public ItemPath getItemPath() {
        return path;
    }

    @Override
    protected boolean requiresParent(Tuple t, QObjectReference<OR> entityPath) {
        if (entityPath instanceof QObjectReferenceWithMeta<OR> casted) {
            return t.get(casted.fullObject) == null;
        }
        return true;
    }

    @Override
    protected void applyToOwner(ObjectType owner, ObjectReferenceType candidate) throws SchemaException {
        var ref = owner.asPrismObject().findOrCreateReference(getItemPath());
        ref.add(candidate.asReferenceValue());
    }

    @Override
    public OrderSpecifier<?> orderSpecifier(QObjectReferenceWithMeta<OR> orqObjectReferenceWithMeta) {
        return new OrderSpecifier<>(Order.ASC, orqObjectReferenceWithMeta.ownerOid);
    }
}
