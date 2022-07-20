/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType.*;

import java.util.*;
import java.util.Map.Entry;
import javax.xml.namespace.QName;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Path;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.ExtUtils;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.delta.item.CountItemDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.mapping.CountMappingResolver;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleItemSqlMapper;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItem;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.QResourceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowCorrelationStateType;
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
        addItemMapping(F_DEAD, booleanMapper(q -> q.dead));
        addItemMapping(F_EXISTS, booleanMapper(q -> q.exist));
        addItemMapping(F_FULL_SYNCHRONIZATION_TIMESTAMP,
                timestampMapper(q -> q.fullSynchronizationTimestamp));
        addItemMapping(F_PRIMARY_IDENTIFIER_VALUE, stringMapper(q -> q.primaryIdentifierValue));
        addItemMapping(F_SYNCHRONIZATION_SITUATION, enumMapper(q -> q.synchronizationSituation));
        addItemMapping(F_SYNCHRONIZATION_TIMESTAMP,
                timestampMapper(q -> q.synchronizationTimestamp));
        addExtensionMapping(F_ATTRIBUTES, MExtItemHolderType.ATTRIBUTES, q -> q.attributes);
        addNestedMapping(F_CORRELATION, ShadowCorrelationStateType.class)
                .addItemMapping(ShadowCorrelationStateType.F_CORRELATION_START_TIMESTAMP,
                        timestampMapper(q -> q.correlationStartTimestamp))
                .addItemMapping(ShadowCorrelationStateType.F_CORRELATION_END_TIMESTAMP,
                        timestampMapper(q -> q.correlationEndTimestamp))
                .addItemMapping(ShadowCorrelationStateType.F_CORRELATION_CASE_OPEN_TIMESTAMP,
                        timestampMapper(q -> q.correlationCaseOpenTimestamp))
                .addItemMapping(ShadowCorrelationStateType.F_CORRELATION_CASE_CLOSE_TIMESTAMP,
                        timestampMapper(q -> q.correlationCaseCloseTimestamp))
                .addItemMapping(ShadowCorrelationStateType.F_SITUATION,
                        enumMapper(q -> q.correlationSituation));

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
        row.dead = shadow.isDead();
        row.exist = shadow.isExists();
        row.fullSynchronizationTimestamp =
                MiscUtil.asInstant(shadow.getFullSynchronizationTimestamp());
        row.pendingOperationCount = shadow.getPendingOperation().size();
        row.primaryIdentifierValue = shadow.getPrimaryIdentifierValue();
        row.synchronizationSituation = shadow.getSynchronizationSituation();
        row.synchronizationTimestamp = MiscUtil.asInstant(shadow.getSynchronizationTimestamp());
        row.attributes = processExtensions(shadow.getAttributes(), MExtItemHolderType.ATTRIBUTES);
        ShadowCorrelationStateType correlation = shadow.getCorrelation();
        if (correlation != null) {
            row.correlationStartTimestamp = MiscUtil.asInstant(correlation.getCorrelationStartTimestamp());
            row.correlationEndTimestamp = MiscUtil.asInstant(correlation.getCorrelationEndTimestamp());
            row.correlationCaseOpenTimestamp = MiscUtil.asInstant(correlation.getCorrelationCaseOpenTimestamp());
            row.correlationCaseCloseTimestamp = MiscUtil.asInstant(correlation.getCorrelationCaseCloseTimestamp());
            row.correlationSituation = correlation.getSituation();
        }
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
            Jsonb rowAttributes = row.get(entityPath.attributes);
            applyShadowAttributesDefinitions(shadowType, rowAttributes);
        }

        List<SelectorOptions<GetOperationOptions>> retrieveOptions = SelectorOptions.filterRetrieveOptions(options);
        if (retrieveOptions.isEmpty()) {
            return shadowType;
        }

        if (loadIndexOnly(retrieveOptions)) {
            addIndexOnlyAttributes(shadowType, row, entityPath);
        }
        return shadowType;
    }

    private void addIndexOnlyAttributes(ShadowType shadowType, Tuple row, QShadow entityPath) throws SchemaException {
        Jsonb rowAttributes = row.get(entityPath.attributes);
        if (rowAttributes == null) {
            return;
        }
        Map<String, Object> attributes = Jsonb.toMap(rowAttributes);
        if (attributes.isEmpty()) {
            return;
        }

        ShadowAttributesType attributeContainer = shadowType.getAttributes();
        if (attributeContainer == null) {
            attributeContainer = new ShadowAttributesType();
            shadowType.attributes(attributeContainer);
        }
        //noinspection unchecked
        PrismContainerValue<ShadowAttributesType> container = attributeContainer.asPrismContainerValue();
        // Now we retrieve indexOnly options
        for (Entry<String, Object> attribute : attributes.entrySet()) {
            MExtItem mapping = Objects.requireNonNull(
                    repositoryContext().getExtensionItem(Integer.valueOf(attribute.getKey())));
            QName itemName = QNameUtil.uriToQName(mapping.itemName);
            ItemDefinition<?> definition = definitionFrom(itemName, mapping, true);
            if (definition instanceof PrismPropertyDefinition) {
                var item = container.findOrCreateProperty((PrismPropertyDefinition<?>) definition);
                switch (mapping.cardinality) {
                    case SCALAR:
                        item.setRealValue(attribute.getValue());
                        break;
                    case ARRAY:
                        List<?> value = (List<?>) attribute.getValue();
                        item.setRealValues(value.toArray());
                        break;
                    default:
                        throw new IllegalStateException("");
                }
                if (item.isIncomplete() && (item.getDefinition() == null || !item.getDefinition().isIndexOnly())) {
                    // Item was not fully serialized / probably indexOnly item.
                    //noinspection unchecked
                    item.applyDefinition((PrismPropertyDefinition<Object>) definition);
                }
                item.setIncomplete(false);
            }
        }
    }

    @Override
    public Collection<SelectorOptions<GetOperationOptions>> updateGetOptions(
            Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications) {
        List<SelectorOptions<GetOperationOptions>> ret = new ArrayList<>(super.updateGetOptions(options, modifications));

        boolean attributes = false;
        for (ItemDelta<?, ?> modification : modifications) {
            if (F_ATTRIBUTES.isSubPath(modification.getPath())) {
                attributes = true;
                break;
            }
        }
        if (attributes) {
            ret.addAll(SchemaService.get().getOperationOptionsBuilder().item(F_ATTRIBUTES).retrieve().build());
        }
        return ret;
    }

    @Override
    public @NotNull Path<?>[] selectExpressions(QShadow entity,
            Collection<SelectorOptions<GetOperationOptions>> options) {
        var retrieveOptions = SelectorOptions.filterRetrieveOptions(options);
        boolean isRaw = GetOperationOptions.isRaw(SelectorOptions.findRootOptions(options));
        if (isRaw || loadIndexOnly(retrieveOptions)) {
            return new Path[] { entity.oid, entity.fullObject, entity.attributes };
        }
        return new Path[] { entity.oid, entity.fullObject };
    }

    private boolean loadIndexOnly(List<SelectorOptions<GetOperationOptions>> retrieveOptions) {
        for (SelectorOptions<GetOperationOptions> selectorOptions : retrieveOptions) {
            if (selectorOptions.getOptions() == null || !RetrieveOption.INCLUDE.equals(selectorOptions.getOptions().getRetrieve())) {
                continue;
            }

            ItemPath path = selectorOptions.getSelector() != null ? selectorOptions.getSelector().getPath() : null;
            if (path == null || path.isEmpty() || F_ATTRIBUTES.isSubPathOrEquivalent(path)) {
                return true;
            }

        }
        return false;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void applyShadowAttributesDefinitions(ShadowType shadowType, Jsonb rowAttributes) throws SchemaException {
        Map<QName, MExtItem> definitions = definitionsFrom(rowAttributes);

        if (shadowType.getAttributes() == null) {
            return;
        }
        PrismContainerValue<?> attributes = shadowType.getAttributes().asPrismContainerValue();

        for (Item<?, ?> attribute : attributes.getItems()) {
            ItemName itemName = attribute.getElementName();
            MExtItem itemInfo = definitions.get(itemName);
            if (itemInfo != null && attribute.getDefinition() == null) {
                ((Item) attribute).applyDefinition(definitionFrom(itemName, itemInfo, false), true);
            }
        }
    }

    private Map<QName, MExtItem> definitionsFrom(Jsonb rowAttributes) {
        Map<QName, MExtItem> definitions = new HashMap<>();
        if (rowAttributes == null) {
            return definitions;
        }
        Map<String, Object> attributes = Jsonb.toMap(rowAttributes);

        for (String id : attributes.keySet()) {
            var extItem = repositoryContext().getExtensionItem(Integer.valueOf(id));
            if (extItem != null) {
                QName key = QNameUtil.uriToQName(extItem.itemName);
                definitions.put(key, extItem);
            }
        }
        return definitions;
    }

    private ItemDefinition<?> definitionFrom(QName name, MExtItem itemInfo, boolean indexOnly) {
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
        def.setIndexOnly(indexOnly);
        return def;
    }
}
