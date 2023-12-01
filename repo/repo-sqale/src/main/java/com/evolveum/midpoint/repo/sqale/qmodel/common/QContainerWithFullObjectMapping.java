package com.evolveum.midpoint.repo.sqale.qmodel.common;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QSeparatelySerializedItem;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

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
    public Predicate allOwnedBy(Q q, List<UUID> oidList) {
        return q.ownerOid.in(oidList);
    }

    @Override
    public boolean hasFullObject(R row) {
        return row.fullObject != null;
    }

    @Override
    public UUID getOwner(R row) {
        return row.ownerOid;
    }

    @Override
    public PrismValue toSchemaObjectEmbedded(R row) throws SchemaException {
        return toSchemaObject(row).asPrismContainerValue();
    }

    @Override
    public Class<? extends Item<? extends PrismValue, ?>> getPrismItemType() {
        return (Class) PrismContainer.class;
    }

    @Override
    public Q createAlias() {
        return defaultAlias();
    }
}
