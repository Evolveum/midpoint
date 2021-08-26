/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType.*;

import java.util.Collection;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.querydsl.core.Tuple;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.sqale.ExtUtils;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.delta.item.CountItemDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.mapping.CountMappingResolver;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleItemSqlMapper;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItem;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.QResourceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Mapping between {@link QShadow} and {@link ShadowType}.
 */
public class QShadowMapping
        extends QObjectMapping<ShadowType, QShadow, MShadow> {

    public static final String DEFAULT_ALIAS_NAME = "sh";

    private static QShadowMapping instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QShadowMapping initShadowMapping(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QShadowMapping(repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QShadowMapping getShadowMapping() {
        return Objects.requireNonNull(instance);
    }

    private QShadowMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QShadow.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ShadowType.class, QShadow.class, repositoryContext);

        addItemMapping(ShadowType.F_OBJECT_CLASS, uriMapper(q -> q.objectClassId));
        addRefMapping(F_RESOURCE_REF,
                q -> q.resourceRefTargetOid,
                q -> q.resourceRefTargetType,
                q -> q.resourceRefRelationId,
                QResourceMapping::get);
        addItemMapping(F_INTENT, stringMapper(q -> q.intent));
        addItemMapping(F_TAG, stringMapper(q -> q.tag));
        addItemMapping(F_KIND, enumMapper(q -> q.kind));
        // TODO attemptNumber?
        addItemMapping(F_DEAD, booleanMapper(q -> q.dead));
        addItemMapping(F_EXISTS, booleanMapper(q -> q.exist));
        addItemMapping(F_FULL_SYNCHRONIZATION_TIMESTAMP,
                timestampMapper(q -> q.fullSynchronizationTimestamp));
        addItemMapping(F_PRIMARY_IDENTIFIER_VALUE, stringMapper(q -> q.primaryIdentifierValue));
        addItemMapping(F_SYNCHRONIZATION_SITUATION, enumMapper(q -> q.synchronizationSituation));
        addItemMapping(F_SYNCHRONIZATION_TIMESTAMP,
                timestampMapper(q -> q.synchronizationTimestamp));
        addExtensionMapping(F_ATTRIBUTES, MExtItemHolderType.ATTRIBUTES, q -> q.attributes);

        // Item mapping to update the count, relation resolver for query with EXISTS filter.
        addItemMapping(F_PENDING_OPERATION, new SqaleItemSqlMapper<>(
                ctx -> new CountItemDeltaProcessor<>(ctx, q -> q.pendingOperationCount)));
        addRelationResolver(F_PENDING_OPERATION,
                new CountMappingResolver<>(q -> q.pendingOperationCount));
    }

    @Override
    protected QShadow newAliasInstance(String alias) {
        return new QShadow(alias);
    }

    @Override
    public MShadow newRowObject() {
        return new MShadow();
    }

    @Override
    public @NotNull MShadow toRowObjectWithoutFullObject(
            ShadowType shadow, JdbcSession jdbcSession) {
        MShadow row = super.toRowObjectWithoutFullObject(shadow, jdbcSession);

        row.objectClassId = processCacheableUri(shadow.getObjectClass());
        setReference(shadow.getResourceRef(),
                o -> row.resourceRefTargetOid = o,
                t -> row.resourceRefTargetType = t,
                r -> row.resourceRefRelationId = r);
        row.intent = shadow.getIntent();
        row.tag = shadow.getTag();
        row.kind = shadow.getKind();
//        row.attemptNumber = shadow.att; TODO not set in RShadow, probably just with deltas? Where does it come from?
        row.dead = shadow.isDead();
        row.exist = shadow.isExists();
        row.fullSynchronizationTimestamp =
                MiscUtil.asInstant(shadow.getFullSynchronizationTimestamp());
        row.pendingOperationCount = shadow.getPendingOperation().size();
        row.primaryIdentifierValue = shadow.getPrimaryIdentifierValue();
        row.synchronizationSituation = shadow.getSynchronizationSituation();
        row.synchronizationTimestamp = MiscUtil.asInstant(shadow.getSynchronizationTimestamp());
        row.attributes = processExtensions(shadow.getAttributes(), MExtItemHolderType.ATTRIBUTES);
        return row;
    }

    @Override
    public ShadowType toSchemaObject(Tuple row, QShadow entityPath,
            Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException {
        ShadowType shadowType = super.toSchemaObject(row, entityPath, options);
        // FIXME: we store it because provisioning now sends it to repo, but it should be transient
        shadowType.asPrismObject().removeContainer(ShadowType.F_ASSOCIATION);

        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        if (GetOperationOptions.isRaw(rootOptions)) {
            // If raw=true, we populate attributes with types cached in repository
            applyShadowAttributesDefinitions(shadowType);
        }

        return shadowType;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void applyShadowAttributesDefinitions(ShadowType shadowType) throws SchemaException {
        if (shadowType.getAttributes() == null) {
            return;
        }
        PrismContainerValue<?> attributesOld = shadowType.getAttributes().asPrismContainerValue();

        for (Item<?, ?> attribute : attributesOld.getItems()) {
            ItemName itemName = attribute.getElementName();
            MExtItem itemInfo = repositoryContext().getExtensionItem(
                    MExtItem.itemNameKey(attribute.getElementName(), MExtItemHolderType.ATTRIBUTES));
            if (itemInfo != null && attribute.getDefinition() == null) {
                ((Item) attribute).applyDefinition(definitionFrom(itemName, itemInfo), true);
            }
        }
    }

    private ItemDefinition<?> definitionFrom(QName name, MExtItem itemInfo) {
        QName typeName = ExtUtils.getSupportedTypeName(itemInfo.valueType);
        final MutableItemDefinition<?> def;
        if (ObjectReferenceType.COMPLEX_TYPE.equals(typeName)) {
            def = PrismContext.get().definitionFactory().createReferenceDefinition(name, typeName);
        } else {
            def = PrismContext.get().definitionFactory().createPropertyDefinition(name, typeName);
        }
        def.setMinOccurs(0);
        def.setMaxOccurs(-1);
        def.setRuntimeSchema(true);
        def.setDynamic(true);
        return def;
    }
}
