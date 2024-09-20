/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType.F_SOURCE;

import java.util.Objects;
import java.util.UUID;

import com.evolveum.axiom.concepts.CheckedFunction;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistryState;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerType;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerWithFullObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.QResourceMapping;

import com.evolveum.midpoint.util.exception.SystemException;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Mapping between {@link QFocusIdentity} and {@link FocusIdentityType}.
 *
 * @param <OR> type of the owner row
 */
public class QFocusIdentityMapping<OR extends MFocus>
        extends QContainerWithFullObjectMapping<FocusIdentityType, QFocusIdentity<OR>, MFocusIdentity, OR> {

    public static final String DEFAULT_ALIAS_NAME = "fi";

    public static final ItemPath PATH = ItemPath.create(FocusType.F_IDENTITIES, FocusIdentitiesType.F_IDENTITY);
    private static QFocusIdentityMapping<?> instance;

    private final SchemaRegistryState.DerivationKey<ItemDefinition<?>> derivationKey;

    private final CheckedFunction<SchemaRegistryState, ItemDefinition<?>, SystemException> derivationMapping;



    public static <OR extends MFocus> QFocusIdentityMapping<OR> init(
            @NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QFocusIdentityMapping<>(repositoryContext);
        }
        return get();
    }

    public static <OR extends MFocus> QFocusIdentityMapping<OR> get() {
        //noinspection unchecked
        return (QFocusIdentityMapping<OR>) Objects.requireNonNull(instance);
    }

    // We can't declare Class<QFocusIdentity<OR>>.class, so we cheat a bit.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private QFocusIdentityMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QFocusIdentity.TABLE_NAME, DEFAULT_ALIAS_NAME,
                FocusIdentityType.class, (Class) QFocusIdentity.class, repositoryContext);
        this.derivationKey = SchemaRegistryState.derivationKeyFrom(getClass(), "DEFINITION");
        this.derivationMapping = (registry) -> {
            var focusDef = registry.findObjectDefinitionByCompileTimeClass(FocusType.class);
            return focusDef.findItemDefinition(ItemPath.create(FocusType.F_IDENTITIES, FocusIdentitiesType.F_IDENTITY));
        };
        addRelationResolver(PrismConstants.T_PARENT,
                // mapping supplier is used to avoid cycles in the initialization code
                TableRelationResolver.usingJoin(
                        QFocusMapping::getFocusMapping,
                        (q, p) -> q.ownerOid.eq(p.oid)));

        addNestedMapping(F_SOURCE, FocusIdentitySourceType.class)
                .addRefMapping(FocusIdentitySourceType.F_RESOURCE_REF,
                        q -> q.sourceResourceRefTargetOid,
                        null,
                        null,
                        QResourceMapping::get);
    }
    @Override
    protected QFocusIdentity<OR> newAliasInstance(String alias) {
        return new QFocusIdentity<>(alias);
    }

    @Override
    public MFocusIdentity newRowObject() {
        return new MFocusIdentity();
    }

    @Override
    public MFocusIdentity newRowObject(OR ownerRow) {
        MFocusIdentity row = newRowObject();
        row.ownerOid = ownerRow.oid;
        return row;
    }

    @Override
    public MFocusIdentity insert(
            FocusIdentityType schemaObject, OR ownerRow, JdbcSession jdbcSession) throws SchemaException {
        MFocusIdentity row = initRowObjectWithFullObject(schemaObject, ownerRow);

        FocusIdentitySourceType source = schemaObject.getSource();
        if (source != null) {
            ObjectReferenceType resourceRef = source.getResourceRef();
            if (resourceRef != null) {
                row.sourceResourceRefTargetOid = UUID.fromString(resourceRef.getOid());
            }
        }

        insert(row, jdbcSession);
        return row;
    }

    @Override
    public ItemPath getItemPath() {
        return PATH;
    }

    @Override
    public FocusIdentityType toSchemaObjectLegacy(MFocusIdentity row) throws SchemaException {
        return parseSchemaObject(
                row.fullObject,
                "identity for " + row.ownerOid + "," + row.cid,
                FocusIdentityType.class);
    }

    @Override
    public void afterModify(SqaleUpdateContext<FocusIdentityType, QFocusIdentity<OR>, MFocusIdentity> updateContext)
            throws SchemaException {
        PrismContainer<FocusIdentityType> identityContainer =
                updateContext.findValueOrItem(FocusType.F_IDENTITIES, FocusIdentitiesType.F_IDENTITY);
        // row in context already knows its CID
        PrismContainerValue<FocusIdentityType> pcv = identityContainer.findValue(updateContext.row().cid);
        byte[] fullObject = createFullObject(pcv.asContainerable());
        updateContext.set(updateContext.entityPath().fullObject, fullObject);
    }

    @Override
    public OrderSpecifier<?> orderSpecifier(QFocusIdentity<OR> orqFocusIdentity) {
        return new OrderSpecifier<>(Order.ASC, orqFocusIdentity.cid);
    }

    @Override
    protected SchemaRegistryState.DerivationKey<ItemDefinition<?>> definitionDerivationKey() {
        return derivationKey;
    }

    @Override
    protected CheckedFunction<SchemaRegistryState, ItemDefinition<?>, SystemException> definitionDerivation() {
        return derivationMapping;
    }
}
