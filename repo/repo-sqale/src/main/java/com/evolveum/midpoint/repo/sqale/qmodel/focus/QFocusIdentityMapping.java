/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType.F_ITEMS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType.F_SOURCE;

import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
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
        extends QContainerMapping<FocusIdentityType, QFocusIdentity<OR>, MFocusIdentity, OR> {

    public static final String DEFAULT_ALIAS_NAME = "fi";

    private static QFocusIdentityMapping<?> instance;

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

        addRelationResolver(PrismConstants.T_PARENT,
                // mapping supplier is used to avoid cycles in the initialization code
                TableRelationResolver.usingJoin(
                        QFocusMapping::getFocusMapping,
                        (q, p) -> q.ownerOid.eq(p.oid)));

        // This mapping should only work when ProjectionFocusIdentitySourceType is used.
        addNestedMapping(F_SOURCE, ProjectionFocusIdentitySourceType.class)
                .addRefMapping(ProjectionFocusIdentitySourceType.F_RESOURCE_REF,
                        q -> q.sourceResourceRefTargetOid,
                        null,
                        null,
                        QFocusMapping::getFocusMapping);

        // TODO EXTENSION or ATTRIBUTES or something new here?
        addNestedMapping(F_ITEMS, FocusIdentityItemsType.class)
                .addExtensionMapping(FocusIdentityItemsType.F_ORIGINAL,
                        MExtItemHolderType.EXTENSION, q -> q.itemsOriginal, repositoryContext)
                .addExtensionMapping(FocusIdentityItemsType.F_NORMALIZED,
                        MExtItemHolderType.EXTENSION, q -> q.itemsNormalized, repositoryContext);
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
        MFocusIdentity row = initRowObject(schemaObject, ownerRow);

        AbstractFocusIdentitySourceType source = schemaObject.getSource();
        if (source != null) {
            row.fullSource = createFullObject(schemaObject.getSource());

            if (source instanceof ProjectionFocusIdentitySourceType) {
                ObjectReferenceType resourceRef = ((ProjectionFocusIdentitySourceType) source).getResourceRef();
                if (resourceRef != null) {
                    row.sourceResourceRefTargetOid = UUID.fromString(resourceRef.getOid());
                }
            }
        }

        FocusIdentityItemsType items = schemaObject.getItems();
        if (items != null) {
            row.itemsOriginal = processExtensions(items.getOriginal(), MExtItemHolderType.EXTENSION);
            row.itemsNormalized = processExtensions(items.getNormalized(), MExtItemHolderType.EXTENSION);
        }

        insert(row, jdbcSession);
        return row;
    }

    @Override
    public FocusIdentityType toSchemaObject(MFocusIdentity row) {
        // TODO fill in source full object and both items containers from ext columns
        return new FocusIdentityType();
    }
}
