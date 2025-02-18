/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType.*;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.path.PathSet;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.DateTimePath;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.sqale.ExtUtils;
import com.evolveum.midpoint.repo.sqale.ExtensionProcessor;
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
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

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


    private final ShadowPartitionManager partitionManager;

    private QShadowMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QShadow.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ShadowType.class, QShadow.class, repositoryContext);
        partitionManager = new ShadowPartitionManager(repositoryContext);
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
                timestampMapper(q -> q.fullSynchronizationTimestamp, true));
        addItemMapping(F_PRIMARY_IDENTIFIER_VALUE, stringMapper(q -> q.primaryIdentifierValue));
        addItemMapping(F_SYNCHRONIZATION_SITUATION, enumMapper(q -> q.synchronizationSituation));
        addItemMapping(F_SYNCHRONIZATION_TIMESTAMP,
                timestampMapper(q -> q.synchronizationTimestamp, true));
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

        addNestedMapping(F_ACTIVATION, ActivationType.class)
                .addItemMapping(ActivationType.F_DISABLE_REASON, uriMapper(q -> q.disableReasonId))
                .addItemMapping(ActivationType.F_ENABLE_TIMESTAMP,timestampMapper(q -> q.enableTimestamp))
                .addItemMapping(ActivationType.F_DISABLE_TIMESTAMP, timestampMapper(q -> q.disableTimestamp))
        ;
        addNestedMapping(F_BEHAVIOR, ShadowBehaviorType.class)
                .addItemMapping(ShadowBehaviorType.F_LAST_LOGIN_TIMESTAMP, timestampMapper(q -> q.lastLoginTimestamp));
        // Item mapping to update the count, relation resolver for query with EXISTS filter.
        addItemMapping(F_PENDING_OPERATION, new SqaleItemSqlMapper<>(
                ctx -> new CountItemDeltaProcessor<>(ctx, q -> q.pendingOperationCount)));
        addRelationResolver(F_PENDING_OPERATION,
                new CountMappingResolver<>(q -> q.pendingOperationCount));




        //addItemMapping(F_REFERENCE_ATTRIBUTES, new SqaleItemSqlMapper<>(
        //        ctx -> new RefTableItemFilterProcessor<>(ctx, referenceMapping),
        //        ctx -> new RefTableItemDeltaProcessor<>(ctx, referenceMapping)));

        // Needed for queries with ref/@/... paths, this resolves the "ref/" part before @.
        QShadowReferenceAttributeMapping referenceMapping = QShadowReferenceAttributeMapping.init(repositoryContext);
        addItemMapping(F_REFERENCE_ATTRIBUTES, new ShadowReferenceAttributesMapper());
        addRelationResolver(F_REFERENCE_ATTRIBUTES, new ShadowReferenceAttributesResolver(referenceMapping));

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
        var activation = shadow.getActivation();
        if (activation != null) {
            row.disableReasonId = processCacheableUri(activation.getDisableReason());
            row.enableTimestamp = MiscUtil.asInstant(activation.getEnableTimestamp());
            row.disableTimestamp = MiscUtil.asInstant(activation.getDisableTimestamp());
        }
        var behavior = shadow.getBehavior();
        if (behavior != null) {
            row.lastLoginTimestamp = MiscUtil.asInstant(behavior.getLastLoginTimestamp());
        }
        return row;
    }

    protected static void attachTimestamp(Consumer<XMLGregorianCalendar> setter, Tuple row, DateTimePath<Instant> path) {
        var timestamp = row.get(path);
        if (timestamp != null) {
            setter.accept(SqaleUtils.toCalendar(timestamp));
        } else {
            setter.accept(null);
        }
    }

    @Override
    protected void attachColumnOnlyData(@NotNull Tuple row, @NotNull QShadow entityPath, @NotNull ShadowType ret) {
        super.attachColumnOnlyData(row, entityPath, ret);
        attachTimestamp(ret::setSynchronizationTimestamp, row,  entityPath.synchronizationTimestamp);
        attachTimestamp(ret::setFullSynchronizationTimestamp, row,  entityPath.fullSynchronizationTimestamp);
    }

    @Override
    public void storeRelatedEntities(@NotNull MShadow row, @NotNull ShadowType shadow, @NotNull JdbcSession jdbcSession) throws SchemaException {
        super.storeRelatedEntities(row, shadow, jdbcSession);
        insertReferenceAttributes(shadow.getReferenceAttributes(), row, jdbcSession);

    }

    private void insertReferenceAttributes(ShadowReferenceAttributesType refAttrsBean, MShadow owner, JdbcSession jdbcSession) throws SchemaException {
        if (refAttrsBean == null) {
            return;
        }
        PrismContainerValue<?> refAttrs = refAttrsBean.asPrismContainerValue();
        for (var item : refAttrs.getItems()) {
            var name = item.getElementName();
            if (item instanceof PrismReference ref) {
                Integer pathId = null;
                for (var val : ref.getValues()) {
                    if (pathId == null) {
                        pathId = repositoryContext().processCacheableUri(name);
                    }
                    var ort = (ObjectReferenceType) val.getRealValue();
                    if (ort.getType() == null) {
                        // Target type should be shadow
                        ort.setType(COMPLEX_TYPE);
                    }
                    QShadowReferenceAttributeMapping.get().insert(pathId, ort, owner, jdbcSession);
                }
            }
        }
   }

    @Override
    public ShadowType toSchemaObject(@NotNull Tuple row, @NotNull QShadow entityPath,
            @NotNull JdbcSession jdbcSession, Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException {
        ShadowType shadowType = super.toSchemaObject(row, entityPath, jdbcSession, options);
        shadowType.asPrismObject().removeContainer(ShadowType.F_ASSOCIATIONS); // temporary
        addIndexOnlyAttributes(shadowType, row, entityPath);
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

        new ExtensionProcessor(repositoryContext()).extensionsToContainer(attributes, attributeContainer);
        // Data were loaded, lets mark it as complete.
        shadowType.asPrismObject().findItem(F_ATTRIBUTES).setIncomplete(false);

    }

    @Override
    public Collection<SelectorOptions<GetOperationOptions>> updateGetOptions(
            Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications, boolean forceReindex) {
        List<SelectorOptions<GetOperationOptions>> ret = new ArrayList<>(super.updateGetOptions(options, modifications, forceReindex));

        if (modifications.stream().anyMatch(m -> F_ATTRIBUTES.isSubPath(m.getPath()))) {
            ret.addAll(SchemaService.get().getOperationOptionsBuilder().item(F_ATTRIBUTES).retrieve().build());
        }
        return ret;
    }

    @Override
    public @NotNull Path<?>[] selectExpressions(QShadow entity,
            Collection<SelectorOptions<GetOperationOptions>> options) {
        var ret = super.selectExpressions(entity, options);
        if (isExcludeAll(options)) {
            return ret;
        }
        return appendPaths(ret, entity.attributes, entity.synchronizationTimestamp, entity.fullSynchronizationTimestamp);
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
                ((Item) attribute).applyDefinition(ExtUtils.createDefinition(itemName, itemInfo, false));
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

    @Override
    protected void customizeFullObjectItemsToSkip(PathSet mutableSet) {
        super.customizeFullObjectItemsToSkip(mutableSet);
        mutableSet.add(F_ATTRIBUTES);
    }


    @Override
    public ShadowPartitionManager getPartitionManager() {
        return partitionManager;
    }

    @Override
    public void preprocessCacheableUris(ShadowType shadow) {
        //QShadowReferenceAttributeMapping.get().preprocessCacheableUris();
        processCacheableUri(shadow.getObjectClass());
        var activation = shadow.getActivation();
        if (activation != null) {
            processCacheableUri(activation.getDisableReason());
        }
        var refAttrsBean = shadow.getReferenceAttributes();
        if (refAttrsBean == null) {
            return;
        }
        PrismContainerValue<?> refAttrs = refAttrsBean.asPrismContainerValue();
        for (var item : refAttrs.getItems()) {
            var name = item.getElementName();
            if (item instanceof PrismReference ref) {
                Integer pathId = null;
                for (var val : ref.getValues()) {
                    if (pathId == null) {
                        pathId = repositoryContext().processCacheableUri(name);
                    }
                }
            }
        }
    }
}
