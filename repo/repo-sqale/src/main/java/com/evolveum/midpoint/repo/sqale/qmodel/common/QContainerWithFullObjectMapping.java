package com.evolveum.midpoint.repo.sqale.qmodel.common;

import com.evolveum.axiom.concepts.CheckedFunction;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistryState;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QSeparatelySerializedItem;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public abstract class QContainerWithFullObjectMapping<S extends Containerable, Q extends QContainerWithFullObject<R, OR>, R extends MContainerWithFullObject, OR>
        extends QContainerMapping<S,Q,R,OR> implements QSeparatelySerializedItem<Q,R> {

    protected QContainerWithFullObjectMapping(@NotNull String tableName, @NotNull String defaultAliasName, @NotNull Class<S> schemaType, @NotNull Class<Q> queryType, @NotNull SqaleRepoContext repositoryContext) {
        super(tableName, defaultAliasName, schemaType, queryType, repositoryContext);
    }

    abstract public ItemPath getItemPath();

    public R initRowObjectWithFullObject(S schemaObject, OR ownerRow) throws SchemaException {
        R row =  super.initRowObject(schemaObject, ownerRow);
        row.fullObject = createFullObject(schemaObject);
        return row;
    }

    @Override
    public void afterModify(SqaleUpdateContext<S, Q, R> updateContext) throws SchemaException {
        super.afterModify(updateContext);
            // insert fullObject here
        PrismContainer<AssignmentType> identityContainer =
                updateContext.findValueOrItem(getItemPath());
        // row in context already knows its CID
        PrismContainerValue<AssignmentType> pcv = identityContainer.findValue(updateContext.row().cid);
        if (pcv == null) {
            return;
        }
        byte[] fullObject = createFullObject(pcv.asContainerable());
        updateContext.set(updateContext.entityPath().fullObject, fullObject);
    }

    @Override
    public final S toSchemaObject(R row) throws SchemaException {
        if (row.fullObject == null) {
            return toSchemaObjectLegacy(row);


        }
        return parseSchemaObject(
                row.fullObject,
                getItemPath() + " for " + row.ownerOid + "," + row.cid,
                schemaType());
    }

    protected abstract S toSchemaObjectLegacy(R row) throws SchemaException;

    @Override
    public Predicate allOwnedBy(Q q, Collection<UUID> oidList) {
        return q.ownerOid.in(oidList);
    }

    @Override
    public boolean hasFullObject(Tuple row, Q path) {
        return row.get(path.fullObject) != null;
    }

    @Override
    public Path<?>[] fullObjectExpressions(Q base) {
        return  new Path[] {base.ownerOid, base.cid, base.fullObject};
    }

    @Override
    public UUID getOwner(Tuple row, Q path) {
        return row.get(path.ownerOid);
    }

    @Override
    public PrismValue toSchemaObjectEmbedded(Tuple tuple, Q alias) throws SchemaException {
        byte[] fullObject = tuple.get(alias.fullObject);
        // Sometimes tuple is whole row and alias.fullObject does not work in that case
        if (fullObject == null) {
            var row = tuple.get(alias);
            if (row != null) {
                fullObject = row.fullObject;
            }
        }

        var obj =  parseSchemaObject(
                fullObject,
                getItemPath() + " for " + tuple.get(alias.ownerOid),
                schemaType()).asPrismContainerValue();
        attachContainerIdPath((S) obj.asContainerable(), tuple, alias);
        return obj;
    }

    @Override
    public Class<? extends Item<? extends PrismValue, ?>> getPrismItemType() {
        return (Class) PrismContainer.class;
    }

    @Override
    public Q createAlias() {
        return defaultAlias();
    }

    @Override
    public boolean useDeltaApplyResults() {
        return true;
    }

   @Override
    protected abstract SchemaRegistryState.DerivationKey<ItemDefinition<?>> definitionDerivationKey();

    @Override
    protected abstract CheckedFunction<SchemaRegistryState, ItemDefinition<?>, SystemException> definitionDerivation();
}
