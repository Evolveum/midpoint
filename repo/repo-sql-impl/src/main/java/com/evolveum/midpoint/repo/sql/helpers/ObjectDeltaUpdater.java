/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers;

import static com.evolveum.midpoint.repo.sql.helpers.modify.DeltaUpdaterUtils.*;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.collection.spi.PersistentCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.repo.sql.data.common.any.*;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.helpers.mapper.Mapper;
import com.evolveum.midpoint.repo.sql.helpers.modify.EntityRegistry;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.helpers.modify.PrismEntityMapper;
import com.evolveum.midpoint.repo.sql.helpers.modify.PrismEntityPair;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.repo.sql.util.PrismIdentifierGenerator;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.util.FullTextSearchConfigurationUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author Viliam Repan (lazyman).
 */
@Component
public class ObjectDeltaUpdater {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectDeltaUpdater.class);

    @Autowired private PrismContext prismContext;
    @Autowired private RepositoryService repositoryService;
    @Autowired private EntityRegistry entityRegistry;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private PrismEntityMapper prismEntityMapper;
    @Autowired private ExtItemDictionary extItemDictionary;
    @Autowired private BaseHelper baseHelper;

    private static final class Context {
        private final RepoModifyOptions options;
        private final PrismIdentifierGenerator<?> idGenerator;
        private final Session session;
        private final ObjectUpdater.AttemptContext attemptContext;

        private Context(RepoModifyOptions options, PrismIdentifierGenerator<?> idGenerator, Session session,
                ObjectUpdater.AttemptContext attemptContext) {
            this.options = options;
            this.idGenerator = idGenerator;
            this.session = session;
            this.attemptContext = attemptContext;
        }
    }

    /**
     * modify
     */
    public <T extends ObjectType> RObject modifyObject(Class<T> type, String oid,
            Collection<? extends ItemDelta<?, ?>> modifications,
            PrismObject<T> prismObject, RepoModifyOptions modifyOptions, Session session,
            ObjectUpdater.AttemptContext attemptContext) throws SchemaException {

        LOGGER.trace("Starting to build entity changes for {}, {}, \n{}", type, oid, DebugUtil.debugDumpLazily(modifications));

        // normalize reference.relation QNames like it's done here ObjectTypeUtil.normalizeAllRelations(prismObject);

        // how to generate identifiers correctly now? to repo entities and to full xml, ids in full XML are generated
        // on different place than we later create new containers...how to match them

        // set proper owner/ownerOid/ownerType for containers/references/result and others

        // todo implement transformation from prism to entity (PrismEntityMapper), probably ROperationResult missing

        // validate lookup tables and certification campaigns

        // mark newly added containers/references as transient

        // validate metadata/*, assignment/metadata/*, assignment/construction/resourceRef changes

        PrismIdentifierGenerator<T> idGenerator = new PrismIdentifierGenerator<>(PrismIdentifierGenerator.Operation.MODIFY);
        idGenerator.collectUsedIds(prismObject);

        Context ctx = new Context(modifyOptions, idGenerator, session, attemptContext);

        // Preprocess modifications: We want to process only real modifications. (As for assumeMissingItems, see MID-5280.)
        Collection<? extends ItemDelta> narrowedModifications = prismObject.narrowModifications(modifications, true);
        LOGGER.trace("Narrowed modifications:\n{}", DebugUtil.debugDumpLazily(narrowedModifications));

        Class<? extends RObject> objectClass = RObjectType.getByJaxbType(type).getClazz();
        //noinspection unchecked
        RObject object = session.byId(objectClass).getReference(oid);

        ManagedType<T> mainEntityType = entityRegistry.getJaxbMapping(type);

        boolean shadowPendingOperationModified = false;

        for (ItemDelta<?, ?> delta : narrowedModifications) {
            ItemPath path = delta.getPath();

            LOGGER.trace("Processing delta with path '{}'", path);

            if (isObjectExtensionDelta(path) || isShadowAttributesDelta(path)) {
                if (delta.getPath().size() == 1) {
                    handleObjectExtensionWholeContainerDelta(object, delta, ctx);
                } else {
                    handleObjectExtensionItemDelta(object, delta, ctx);
                }
            } else if (isOperationResult(delta)) {
                handleOperationResult(object, delta);
            } else if (ObjectType.F_METADATA.equivalent(delta.getPath())) {
                handleWholeMetadata(object, delta);
            } else if (FocusType.F_JPEG_PHOTO.equivalent(delta.getPath())) {
                handlePhoto(object, delta);
            } else {
                if (object instanceof RShadow && ShadowType.F_PENDING_OPERATION.equivalent(delta.getPath())) {
                    shadowPendingOperationModified = true;
                }
                handleRegularModification(object, delta, prismObject, mainEntityType, ctx);
            }
        }

        // the following will apply deltas to prismObject
        handleObjectCommonAttributes(type, narrowedModifications, prismObject, object, idGenerator);

        if (shadowPendingOperationModified) {
            ((RShadow) object).setPendingOperationCount(((ShadowType) prismObject.asObjectable()).getPendingOperation().size());
        }

        LOGGER.trace("Entity changes applied");

        return object;
    }

    private <T extends ObjectType> void handleRegularModification(RObject object, ItemDelta<?, ?> delta,
            PrismObject<T> prismObject, ManagedType<?> mainEntityType, Context ctx)
            throws SchemaException {
        TypeValuePair currentValueTypePair = new TypeValuePair();
        currentValueTypePair.type = mainEntityType;
        currentValueTypePair.value = object;

        ItemPath path = delta.getPath();
        Iterator<?> segments = path.getSegments().iterator();
        while (segments.hasNext()) {
            Object segment = segments.next();
            if (!ItemPath.isName(segment)) {
                LOGGER.trace("Segment {} in path {} is not name item, finishing entity update for delta", segment, path);
                break;
            }

            ItemName name = ItemPath.toName(segment);
            String nameLocalPart = name.getLocalPart();

            if (currentValueTypePair.value instanceof RAssignment) {
                if (QNameUtil.match(name, AssignmentType.F_EXTENSION)) {
                    if (segments.hasNext()) {
                        handleAssignmentExtensionItemDelta((RAssignment) currentValueTypePair.value, delta, ctx);
                    } else {
                        handleAssignmentExtensionWholeContainerDelta((RAssignment) currentValueTypePair.value, delta, ctx);
                    }
                    break;
                } else if (QNameUtil.match(name, AssignmentType.F_METADATA)) {
                    if (!segments.hasNext()) {
                        handleWholeMetadata((RAssignment) currentValueTypePair.value, delta);
                        break;
                    }
                }
            }

            Attribute attribute = findAttribute(currentValueTypePair, nameLocalPart, segments, name);
            if (attribute == null) {
                // there's no table/column that needs update
                break;
            }

            if (segments.hasNext()) {
                stepThroughAttribute(attribute, currentValueTypePair, segments);
                continue;
            }

            handleAttribute(attribute, currentValueTypePair.value, delta, prismObject, ctx.idGenerator);

            if ("name".equals(attribute.getName()) && RObject.class
                    .isAssignableFrom(attribute.getDeclaringType().getJavaType())) {
                // we also need to handle "nameCopy" column, we doesn't need path/segments/nameSegment for this call
                Attribute nameCopyAttribute = findAttribute(currentValueTypePair, "nameCopy", null, null);
                assert nameCopyAttribute != null;
                handleAttribute(nameCopyAttribute, currentValueTypePair.value, delta, prismObject, ctx.idGenerator);
            }
        }
    }

    private void handlePhoto(Object bean, ItemDelta delta) throws SchemaException {
        if (!(bean instanceof RFocus)) {
            throw new SystemException("Bean is not instance of " + RFocus.class + ", shouldn't happen");
        }

        RFocus focus = (RFocus) bean;
        Set<RFocusPhoto> photos = focus.getJpegPhoto();

        if (isDelete(delta)) {
            photos.clear();
            return;
        }

        MapperContext context = new MapperContext();

        context.setRepositoryContext(new RepositoryContext(repositoryService, prismContext, relationRegistry, extItemDictionary,
                baseHelper.getConfiguration()));
        context.setDelta(delta);
        context.setOwner(bean);

        PrismValue value = delta.getAnyValue();
        RFocusPhoto photo = prismEntityMapper.map(value.getRealValue(), RFocusPhoto.class, context);

        if (delta.isAdd()) {
            if (!photos.isEmpty()) {
                throw new SchemaException("Object '" + focus.getOid() + "' already contains photo");
            }

            photo.setTransient(true);
            photos.add(photo);
            return;
        }

        if (photos.isEmpty()) {
            photo.setTransient(true);
            photos.add(photo);
            return;
        }

        RFocusPhoto oldPhoto = photos.iterator().next();
        oldPhoto.setPhoto(photo.getPhoto());
    }

    private boolean isDelete(ItemDelta delta) {
        if (delta.isDelete()) {
            return true;
        }

        if (delta.isReplace()) {
            if (delta.getAnyValue() == null) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("Duplicates")
    private void handleWholeMetadata(Metadata<?> bean, ItemDelta delta) {
        PrismValue value = null;
        if (!delta.isDelete()) {
            value = delta.getAnyValue();
        }

        MapperContext context = new MapperContext();
        context.setRepositoryContext(new RepositoryContext(repositoryService, prismContext, relationRegistry, extItemDictionary,
                baseHelper.getConfiguration()));
        context.setDelta(delta);
        context.setOwner(bean);

        if (value != null) {
            prismEntityMapper.mapPrismValue(value, Metadata.class, context);
        } else {
            // todo clean this up
            // we know that mapper supports mapping null value, but still this code smells
            Mapper mapper = prismEntityMapper.getMapper(MetadataType.class, Metadata.class);
            //noinspection unchecked
            mapper.map(null, context);
        }
    }

    private boolean isOperationResult(ItemDelta delta) throws SchemaException {
        ItemDefinition def = delta.getDefinition();
        if (def == null) {
            throw new SchemaException("No definition in delta for item " + delta.getPath());
        }
        return OperationResultType.COMPLEX_TYPE.equals(def.getTypeName());
    }

    @SuppressWarnings("Duplicates")
    private void handleOperationResult(Object bean, ItemDelta delta) {
        if (!(bean instanceof OperationResult)) {
            throw new SystemException("Bean is not instance of " + OperationResult.class + ", shouldn't happen");
        }

        PrismValue value = null;
        if (!delta.isDelete()) {
            value = delta.getAnyValue();
        }

        MapperContext context = new MapperContext();
        context.setRepositoryContext(new RepositoryContext(repositoryService, prismContext, relationRegistry, extItemDictionary,
                baseHelper.getConfiguration()));
        context.setDelta(delta);
        context.setOwner(bean);

        if (value != null) {
            prismEntityMapper.mapPrismValue(value, OperationResult.class, context);
        } else {
            // todo clean this up
            // we know that mapper supports mapping null value, but still this code smells
            Mapper mapper = prismEntityMapper.getMapper(OperationResultType.class, OperationResult.class);
            //noinspection unchecked
            mapper.map(null, context);
        }
    }

    private <T extends ObjectType> void handleObjectCommonAttributes(Class<T> type, Collection<? extends ItemDelta> modifications,
            PrismObject<T> prismObject, RObject object, PrismIdentifierGenerator<T> idGenerator) throws SchemaException {

        // update version
        String strVersion = prismObject.getVersion();
        int version;
        if (StringUtils.isNotBlank(strVersion)) {
            try {
                version = Integer.parseInt(strVersion) + 1;
            } catch (NumberFormatException e) {
                version = 1;
            }
        } else {
            version = 1;
        }
        object.setVersion(version);

        // apply modifications, ids' for new containers already filled in delta values
        for (ItemDelta<?, ?> modification : modifications) {
            if (modification.getDefinition() == null || !modification.getDefinition().isIndexOnly()) {
                modification.applyTo(prismObject);
            } else {
                // There's no point in applying modifications to index-only items; they are not serialized.
                // (Presumably they are not indexed as well.)
            }
        }

        handleObjectTextInfoChanges(type, modifications, prismObject, object);

        // generate ids for containers that weren't handled in previous step (not processed by repository)
        idGenerator.generate(prismObject);

        // normalize all relations
        ObjectTypeUtil.normalizeAllRelations(prismObject, relationRegistry);

        // full object column will be updated later
    }

    private <T extends ObjectType> boolean isObjectTextInfoRecomputationNeeded(Class<T> type, Collection<? extends ItemDelta> modifications) {
        FullTextSearchConfigurationType config = repositoryService.getFullTextSearchConfiguration();
        if (!FullTextSearchConfigurationUtil.isEnabled(config)) {
            return false;
        }

        Set<ItemPath> paths = FullTextSearchConfigurationUtil.getFullTextSearchItemPaths(config, type);

        for (ItemDelta modification : modifications) {
            ItemPath namesOnly = modification.getPath().namedSegmentsOnly();
            for (ItemPath path : paths) {
                if (path.startsWith(namesOnly)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void handleObjectTextInfoChanges(Class<? extends ObjectType> type, Collection<? extends ItemDelta> modifications,
            PrismObject prismObject, RObject object) {
        // update object text info if necessary
        if (!isObjectTextInfoRecomputationNeeded(type, modifications)) {
            return;
        }

        Set<RObjectTextInfo> newInfos = RObjectTextInfo.createItemsSet((ObjectType) prismObject.asObjectable(), object,
                new RepositoryContext(repositoryService, prismContext, relationRegistry, extItemDictionary, baseHelper.getConfiguration()));

        if (newInfos == null || newInfos.isEmpty()) {
            object.getTextInfoItems().clear();
        } else {
            Set<String> existingTexts = object.getTextInfoItems().stream().map(info -> info.getText()).collect(Collectors.toSet());
            Set<String> newTexts = newInfos.stream().map(info -> info.getText()).collect(Collectors.toSet());

            object.getTextInfoItems().removeIf(existingInfo -> !newTexts.contains(existingInfo.getText()));
            for (RObjectTextInfo newInfo : newInfos) {
                if (!existingTexts.contains(newInfo.getText())) {
                    object.getTextInfoItems().add(newInfo);
                }
            }
        }
    }

    private boolean isObjectExtensionDelta(ItemPath path) {
        return path.startsWithName(ObjectType.F_EXTENSION);
    }

    private boolean isShadowAttributesDelta(ItemPath path) {
        return path.startsWithName(ShadowType.F_ATTRIBUTES);
    }

    private void handleAssignmentExtensionItemDelta(RAssignment assignment, ItemDelta delta, Context ctx) throws SchemaException {
        RAssignmentExtension extension;
        if (assignment.getExtension() != null) {
            extension = assignment.getExtension();
        } else {
            extension = new RAssignmentExtension();
            extension.setOwner(assignment);
            extension.setTransient(true);
            assignment.setExtension(extension);
        }
        handleExtensionDelta(delta, null, null, extension, RAssignmentExtensionType.EXTENSION, ctx);
    }

    private void processExtensionDeltaValueSet(Collection<? extends PrismValue> prismValuesFromDelta,
            Integer itemId, RAnyConverter.ValueType valueType, RObject object,
            RObjectExtensionType objectOwnerType, RAssignmentExtension assignmentExtension,
            RAssignmentExtensionType assignmentExtensionType,
            BiConsumer<Collection<? extends RAnyValue<?>>, Collection<PrismEntityPair<RAnyValue<?>>>> deltaValuesProcessor) {

        RAnyConverter converter = new RAnyConverter(prismContext, extItemDictionary);

        if (prismValuesFromDelta == null) {
            return;
        }

        try {
            Collection<PrismEntityPair<RAnyValue<?>>> rValuesFromDelta = new ArrayList<>();
            for (PrismValue prismValueFromDelta : prismValuesFromDelta) {
                RAnyValue<?> rValueFromDelta = converter.convertToRValue(prismValueFromDelta, object == null, itemId);
                rValuesFromDelta.add(new PrismEntityPair<>(prismValueFromDelta, rValueFromDelta));
            }

            if (object != null) {
                processObjectExtensionValues(object, objectOwnerType, valueType, rValuesFromDelta, deltaValuesProcessor);
            } else {
                processAssignmentExtensionValues(assignmentExtension, assignmentExtensionType, valueType, rValuesFromDelta, deltaValuesProcessor);
            }
        } catch (SchemaException ex) {
            throw new SystemException("Couldn't process extension attributes", ex);
        }
    }

    @SuppressWarnings("unused") // assignmentExtensionType will be used later
    private Collection<RAnyValue<?>> getMatchingValues(Collection<? extends RAnyValue<?>> existing, ItemDefinition def,
            RObjectExtensionType objectOwnerType, RAssignmentExtensionType assignmentExtensionType) {

        Collection<RAnyValue<?>> filtered = new ArrayList<>();
        RExtItem extItemDefinition = extItemDictionary.findItemByDefinition(def);
        if (extItemDefinition == null) {
            return filtered;
        }
        for (RAnyValue<?> value : existing) {
            if (value.getItemId() == null) {
                continue;       // suspicious
            }
            if (!value.getItemId().equals(extItemDefinition.getId())) {
                continue;
            }
            if (value instanceof ROExtValue) {
                ROExtValue oValue = (ROExtValue) value;
                if (!objectOwnerType.equals(oValue.getOwnerType())) {
                    continue;
                }
            } else if (value instanceof RAExtValue) {
                // we cannot filter on assignmentExtensionType because it is not present in database (yet)
//                RAExtValue aValue = (RAExtValue) value;
//                if (!assignmentExtensionType.equals(aValue.getExtensionType())) {
//                    continue;
//                }
            }

            filtered.add(value);
        }

        return filtered;
    }

    private void handleExtensionDelta(ItemDelta<?, ?> delta,
            RObject object, RObjectExtensionType objectExtensionType,
            RAssignmentExtension assignmentExtension, RAssignmentExtensionType assignmentExtensionType,
            Context ctx) throws SchemaException {

        ItemDefinition definition = delta.getDefinition();
        if (definition == null) {
            // todo consider simply returning with no action
            throw new IllegalStateException("Cannot process definition-less extension item: " + delta);
        }
        RAnyConverter.ValueType valueType = RAnyConverter.getValueType(definition, definition.getItemName(),
                RAnyConverter.areDynamicsOfThisKindIndexed(objectExtensionType), prismContext);
        if (valueType == null) {
            return;
        }

        Integer itemId = extItemDictionary.createOrFindItemDefinition(definition).getId();

        if (delta.getValuesToReplace() != null) {
            processExtensionDeltaValueSet(delta.getValuesToReplace(), itemId, valueType, object, objectExtensionType,
                    assignmentExtension, assignmentExtensionType,
                    (dbCollection, pairsFromDelta) -> replaceExtensionValues(objectExtensionType, assignmentExtensionType,
                            definition, dbCollection, pairsFromDelta, ctx));
        } else {
            processExtensionDeltaValueSet(delta.getValuesToDelete(), itemId, valueType, object, objectExtensionType, assignmentExtension,
                    assignmentExtensionType,
                    (dbCollection, pairsFromDelta) -> deleteExtensionValues(dbCollection, pairsFromDelta, ctx));

            processExtensionDeltaValueSet(delta.getValuesToAdd(), itemId, valueType, object, objectExtensionType, assignmentExtension,
                    assignmentExtensionType,
                    (dbCollection, pairsFromDelta) -> addExtensionValues(dbCollection, pairsFromDelta, ctx));
        }
    }

    private void addExtensionValues(Collection<? extends RAnyValue<?>> dbCollection,
            Collection<PrismEntityPair<RAnyValue<?>>> pairsFromDelta, Context ctx) {
        markNewValuesTransientAndAddToExistingNoFetch(dbCollection, pairsFromDelta, ctx);
    }

    /**
     * Similar to DeltaUpdaterUtils.markNewValuesTransientAndAddToExisting but avoids fetching the whole collection content.
     * See MID-5558.
     */
    private static <T> void markNewValuesTransientAndAddToExistingNoFetch(Collection<? extends RAnyValue<?>> dbCollection,
            Collection<PrismEntityPair<RAnyValue<?>>> newValues, Context ctx) {
        for (PrismEntityPair<RAnyValue<?>> item : newValues) {
            RAnyValue<?> rValue = item.getRepository();
            boolean exists;
            if (Boolean.TRUE.equals(RepoModifyOptions.getUseNoFetchExtensionValuesInsertion(ctx.options))) {
                exists = false;     // skipping the check
                ctx.attemptContext.noFetchExtensionValueInsertionAttempted = true;      // to know that CVE can be caused because of this
            } else {
                Serializable id = rValue.createId();
                exists = ctx.session.get(rValue.getClass(), id) != null;
            }
            if (!exists) {
                //noinspection unchecked
                ((Collection) dbCollection).add(rValue);
                ctx.session.persist(rValue);            // it looks that in this way we avoid SQL SELECT (at the cost of .persist that can be sometimes more costly)
            }
        }
    }

    /**
     * Similar to DeltaUpdaterUtils.markNewValuesTransientAndAddToExisting but simpler.
     * <p>
     * We rely on the fact that SAVE/UPDATE is now cascaded to extension items.
     */
    private static <T> void markNewValuesTransientAndAddToExistingNoFetchNoPersist(Collection<? extends RAnyValue<?>> dbCollection,
            Collection<PrismEntityPair<RAnyValue<?>>> newValues, Context ctx) {
        for (PrismEntityPair<RAnyValue<?>> item : newValues) {
            RAnyValue<?> rValue = item.getRepository();
            //noinspection unchecked
            ((Collection) dbCollection).add(rValue);
        }
    }

    private void deleteExtensionValues(Collection<? extends RAnyValue<?>> dbCollection,
            Collection<PrismEntityPair<RAnyValue<?>>> pairsFromDelta, Context ctx) {
        if (pairsFromDelta.isEmpty()) {
            return;
        }
        Collection<RAnyValue<?>> rValuesToDelete = pairsFromDelta.stream()
                .map(PrismEntityPair::getRepository)
                .collect(Collectors.toList());

        boolean collectionLoaded = dbCollection instanceof PersistentCollection &&
                ((PersistentCollection) dbCollection).wasInitialized();

        // Note: as for 4.0 "no fetch" deletion is available only for ROExtString (MID-5558).
        boolean noFetchDeleteSupported =
                Boolean.TRUE.equals(RepoModifyOptions.getUseNoFetchExtensionValuesDeletion(ctx.options)) &&
                        rValuesToDelete.stream().allMatch(rValue -> rValue instanceof ROExtString);

        //System.out.println("Collection loaded = " + collectionLoaded + ", noFetchDeleteSupported = " + noFetchDeleteSupported + " for " + rValuesToDelete);
        if (!collectionLoaded && noFetchDeleteSupported) {
            // We are quite sure we are deleting detached value that has NOT been loaded in this session.
            // But we don't generally use session.delete, because it first loads the transient/detached entity.
            //
            // Of course, we must NOT call dbCollection.remove(...) here. It causes all collection values to be fetched
            // from the database, contradicting MID-5558.

            boolean bulkDelete = false;
            //noinspection ConstantConditions
            if (bulkDelete) {
                List<Serializable> rValueIdList = rValuesToDelete.stream()
                        .map(RAnyValue::createId)
                        .collect(Collectors.toList());
                // This translates to potentially large "OR" query like
                // Query:["delete from m_object_ext_string where
                //           item_id=? and owner_oid=? and ownerType=? and stringValue=? or
                //           item_id=? and owner_oid=? and ownerType=? and stringValue=?"],
                // Params:[(5,6a39f871-4e8c-4c24-bfc1-abebc4a66ee6,0,weapon1,
                //          5,6a39f871-4e8c-4c24-bfc1-abebc4a66ee6,0,weapon2)]
                // I think we should avoid it.
                //
                // We could group related objects (same ownerOid, ownerType, itemId) into one DELETE operation,
                // but that's perhaps too much effort for too little gain.
                //
                // We could try batching but I don't know how to do this in Hibernate. Using native queries is currently
                // too complicated.
                ctx.session.createQuery("delete from ROExtString where id in (:id)")
                        .setParameterList("id", rValueIdList)
                        .executeUpdate();
            } else {
                for (RAnyValue<?> value : rValuesToDelete) {
                    ROExtString s = (ROExtString) value;
                    ctx.session.createQuery("delete from ROExtString where ownerOid = :ownerOid and "
                            + "ownerType = :ownerType and itemId = :itemId and value = :value")
                            .setParameter("ownerOid", s.getOwnerOid())
                            .setParameter("itemId", s.getItemId())
                            .setParameter("ownerType", s.getOwnerType())
                            .setParameter("value", s.getValue())
                            .executeUpdate();
                }
            }
        } else {
            // Traditional deletion
            dbCollection.removeAll(rValuesToDelete);

            // This approach works as well but is needlessly complicated:
//            dbCollection.size();            // to load the collection if it was not loaded before
//            for (RAnyValue<?> rValue : rValuesToDelete) {
//                RAnyValue<?> fromSession = ctx.session.get(rValue.getClass(), rValue.createId());    // there's no additional cost here as the value is already loaded
//                if (fromSession != null) {
//                    dbCollection.remove(fromSession);
//                    ctx.session.delete(fromSession);
//                }
//            }
        }
    }

    private void replaceExtensionValues(RObjectExtensionType objectExtensionType,
            RAssignmentExtensionType assignmentExtensionType, ItemDefinition definition, Collection<? extends RAnyValue<?>> dbCollection,
            Collection<PrismEntityPair<RAnyValue<?>>> pairsFromDelta, Context ctx) {
        Collection<? extends RAnyValue<?>> relevantInDb = getMatchingValues(dbCollection, definition, objectExtensionType, assignmentExtensionType);

        if (pairsFromDelta.isEmpty()) {
            // if there are not new values, we just remove existing ones
            deleteFromCollectionAndDb(dbCollection, relevantInDb, ctx.session);
            return;
        }

        Collection<RAnyValue<?>> rValuesToDelete = new ArrayList<>();
        Collection<PrismEntityPair<RAnyValue<?>>> pairsToAdd = new ArrayList<>();

        // BEWARE - this algorithm does not work for RAnyValues that have equal "value" but differ in other components
        // (e.g. references: OID vs. relation/type; poly strings: orig vs. norm)
        Set<Object> realValuesToAdd = new HashSet<>();
        for (PrismEntityPair<RAnyValue<?>> pair : pairsFromDelta) {
            realValuesToAdd.add(pair.getRepository().getValue());
        }

        for (RAnyValue value : relevantInDb) {
            if (realValuesToAdd.contains(value.getValue())) {
                // do not replace with the same one - don't touch
                realValuesToAdd.remove(value.getValue());
            } else {
                rValuesToDelete.add(value);
            }
        }

        for (PrismEntityPair<RAnyValue<?>> pair : pairsFromDelta) {
            if (realValuesToAdd.contains(pair.getRepository().getValue())) {
                pairsToAdd.add(pair);
            }
        }

        deleteFromCollectionAndDb(dbCollection, rValuesToDelete, ctx.session);
        markNewValuesTransientAndAddToExistingNoFetchNoPersist(dbCollection, pairsToAdd, ctx);
    }

    private void deleteFromCollectionAndDb(Collection<? extends RAnyValue<?>> dbCollection,
            Collection<? extends RAnyValue<?>> valuesToDelete, Session session) {
        //noinspection SuspiciousMethodCalls
        dbCollection.removeAll(valuesToDelete);     // do NOT use for handling regular ADD/DELETE value (fetches the whole collection)
        valuesToDelete.forEach(session::delete);
    }

    private void processAssignmentExtensionValues(RAssignmentExtension extension,
            RAssignmentExtensionType assignmentExtensionType,
            RAnyConverter.ValueType valueType,
            Collection<PrismEntityPair<RAnyValue<?>>> valuesFromDelta,
            BiConsumer<Collection<? extends RAnyValue<?>>, Collection<PrismEntityPair<RAnyValue<?>>>> deltaValuesProcessor) {

        valuesFromDelta.forEach(item -> {
            RAExtValue val = (RAExtValue) item.getRepository();
            val.setAnyContainer(extension);
            val.setExtensionType(assignmentExtensionType);
        });

        //noinspection Duplicates
        switch (valueType) {
            case BOOLEAN:
                deltaValuesProcessor.accept(extension.getDates(), valuesFromDelta);
                break;
            case LONG:
                deltaValuesProcessor.accept(extension.getLongs(), valuesFromDelta);
                break;
            case REFERENCE:
                deltaValuesProcessor.accept(extension.getReferences(), valuesFromDelta);
                break;
            case STRING:
                deltaValuesProcessor.accept(extension.getStrings(), valuesFromDelta);
                break;
            case POLY_STRING:
                deltaValuesProcessor.accept(extension.getPolys(), valuesFromDelta);
                break;
            default:
                throw new AssertionError("Wrong value type: " + valueType);
        }
    }

    private void processObjectExtensionValues(RObject object,
            RObjectExtensionType objectOwnerType, RAnyConverter.ValueType valueType,
            Collection<PrismEntityPair<RAnyValue<?>>> valuesFromDelta,
            BiConsumer<Collection<? extends RAnyValue<?>>, Collection<PrismEntityPair<RAnyValue<?>>>> deltaValuesProcessor) {

        valuesFromDelta.forEach(item -> {
            ROExtValue val = (ROExtValue) item.getRepository();
            val.setOwner(object);
            val.setOwnerType(objectOwnerType);
        });

        //noinspection Duplicates
        switch (valueType) {
            case BOOLEAN:
                deltaValuesProcessor.accept(object.getBooleans(), valuesFromDelta);
                break;
            case DATE:
                deltaValuesProcessor.accept(object.getDates(), valuesFromDelta);
                break;
            case LONG:
                deltaValuesProcessor.accept(object.getLongs(), valuesFromDelta);
                break;
            case REFERENCE:
                deltaValuesProcessor.accept(object.getReferences(), valuesFromDelta);
                break;
            case STRING:
                deltaValuesProcessor.accept(object.getStrings(), valuesFromDelta);
                break;
            case POLY_STRING:
                deltaValuesProcessor.accept(object.getPolys(), valuesFromDelta);
                break;
            default:
                throw new AssertionError("Wrong value type: " + valueType);
        }
    }

    private void handleObjectExtensionWholeContainerDelta(RObject object, ItemDelta delta, Context ctx) throws SchemaException {
        RObjectExtensionType ownerType = computeExtensionType(delta);

        // Because ADD for single-valued container is the same as REPLACE (really?) we treat ADD as a REPLACE here.
        clearExtension(object, ownerType, ctx.session);

        if (delta.isAdd() || delta.isReplace()) {
            PrismContainerValue<?> extension = (PrismContainerValue<?>) delta.getAnyValue();
            if (extension != null) {
                for (Item<?, ?> item : extension.getItems()) {
                    ItemDelta<?, ?> itemDelta = item.createDelta();
                    //noinspection unchecked
                    itemDelta.addValuesToAdd((Collection) item.getClonedValues());

                    handleExtensionDelta(itemDelta, object, ownerType, null, null, ctx);
                }
            }
        }
    }

    private void handleAssignmentExtensionWholeContainerDelta(RAssignment assignment, ItemDelta delta, Context ctx) throws SchemaException {
        RAssignmentExtension ext = assignment.getExtension();

        // Because ADD for single-valued container is the same as REPLACE (really?) we treat ADD as a REPLACE here.
        if (ext != null) {
            clearExtension(ext, ctx.session);
        }

        if (delta.isAdd() || delta.isReplace()) {
            if (ext == null) {
                ext = new RAssignmentExtension();
                ext.setOwner(assignment);
                assignment.setExtension(ext);
            }
            PrismContainerValue<?> extension = (PrismContainerValue<?>) delta.getAnyValue();
            if (extension != null) {
                for (Item<?, ?> item : extension.getItems()) {
                    ItemDelta itemDelta = item.createDelta();
                    //noinspection unchecked
                    itemDelta.addValuesToAdd(item.getClonedValues());

                    handleExtensionDelta(itemDelta, null, null, ext,
                            RAssignmentExtensionType.EXTENSION, ctx);
                }
            }
        }
    }

    private RObjectExtensionType computeExtensionType(ItemDelta delta) {
        if (isObjectExtensionDelta(delta.getPath())) {
            return RObjectExtensionType.EXTENSION;
        } else if (isShadowAttributesDelta(delta.getPath())) {
            return RObjectExtensionType.ATTRIBUTES;
        }

        throw new IllegalStateException("Unknown extension type, shouldn't happen");
    }

    private void handleObjectExtensionItemDelta(RObject object, ItemDelta delta, Context ctx) throws SchemaException {
        handleExtensionDelta(delta, object, computeExtensionType(delta), null, null, ctx);
    }

    private Attribute findAttribute(TypeValuePair typeValuePair, String nameLocalPart, Iterator<?> segments, ItemName name) {
        Attribute attribute = entityRegistry.findAttribute(typeValuePair.type, nameLocalPart);
        if (attribute != null) {
            return attribute;
        }

        Attribute<?, ?> attributeOverride = entityRegistry.findAttributeOverride(typeValuePair.type, nameLocalPart);
        if (attributeOverride != null) {
            return attributeOverride;
        }

        if (!segments.hasNext()) {
            return null;
        }

        // try to search path overrides like metadata/* or assignment/metadata/* or assignment/construction/resourceRef
        Object segment;
        ItemPath subPath = name;
        while (segments.hasNext()) {
            if (!entityRegistry.hasAttributePathOverride(typeValuePair.type, subPath)) {
                subPath = subPath.allUpToLastName();
                break;
            }

            segment = segments.next();
            if (!ItemPath.isName(segment)) {
                return null;
            }
            subPath = subPath.append(segment);
        }

        return entityRegistry.findAttributePathOverride(typeValuePair.type, subPath);
    }

    private void stepThroughAttribute(Attribute attribute, TypeValuePair step, Iterator<?> segments) {
        Method method = (Method) attribute.getJavaMember();

        switch (attribute.getPersistentAttributeType()) {
            case EMBEDDED:
                step.type = entityRegistry.getMapping(attribute.getJavaType());
                Object child = invoke(step.value, method);
                if (child == null) {
                    // embedded entity doesn't exist we have to create it first, so it can be populated later
                    Class childType = getRealOutputType(attribute);
                    try {
                        child = childType.newInstance();
                        PropertyUtils.setSimpleProperty(step.value, attribute.getName(), child);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                        throw new SystemException("Couldn't create new instance of '" + childType.getName()
                                + "', attribute '" + attribute.getName() + "'", ex);
                    }
                }
                step.value = child;
                break;
            case ONE_TO_MANY:
                // object extension is handled separately, only {@link Container} and references are handled here
                Class clazz = getRealOutputType(attribute);

                Long id = ItemPath.toId(segments.next());

                Collection c = (Collection) invoke(step.value, method);
                if (!Container.class.isAssignableFrom(clazz)) {
                    throw new SystemException("Don't know how to go through collection of '" + getRealOutputType(attribute) + "'");
                }

                boolean found = false;
                //noinspection unchecked
                for (Container o : (Collection<Container>) c) {
                    long l = o.getId().longValue();
                    if (l == id) {
                        step.type = entityRegistry.getMapping(clazz);
                        step.value = o;

                        found = true;
                        break;
                    }
                }

                if (!found) {
                    throw new RuntimeException("Couldn't find container of type '" + getRealOutputType(attribute)
                            + "' with id '" + id + "'");
                }
                break;
            case ONE_TO_ONE:
                // assignment extension is handled separately
                break;
            default:
                // nothing to do for other cases
        }
    }

    private void handleAttribute(Attribute attribute, Object bean, ItemDelta delta, PrismObject prismObject, PrismIdentifierGenerator idGenerator) {
        Method method = (Method) attribute.getJavaMember();

        switch (attribute.getPersistentAttributeType()) {
            case BASIC:
            case EMBEDDED:
                handleBasicOrEmbedded(bean, delta, attribute);
                break;
            case MANY_TO_MANY:
                // not used in our mappings
                throw new SystemException("Don't know how to handle @ManyToMany relationship, should not happen");
            case ONE_TO_ONE:
                // assignment extension is handled separately
                break;
            case MANY_TO_ONE:
                // this can't be in delta (probably)
                throw new SystemException("Don't know how to handle @ManyToOne relationship, should not happen");
            case ONE_TO_MANY:
                // object extension is handled separately, only {@link Container} and references are handled here
                Collection oneToMany = (Collection) invoke(bean, method);
                handleOneToMany(oneToMany, delta, attribute, bean, prismObject, idGenerator);
                break;
            case ELEMENT_COLLECTION:
                Collection elementCollection = (Collection) invoke(bean, method);
                handleElementCollection(elementCollection, delta, attribute, bean, prismObject, idGenerator);
                break;
        }
    }

    private void handleBasicOrEmbedded(Object bean, ItemDelta<?, ?> delta, Attribute attribute) {
        Class outputType = getRealOutputType(attribute);

        PrismValue prismValue;
        if (delta.isAdd()) {
            assert !delta.getValuesToAdd().isEmpty();
            prismValue = getSingleValue(attribute, delta.getValuesToAdd());
        } else if (delta.isReplace()) {
            if (!delta.getValuesToReplace().isEmpty()) {
                prismValue = getSingleValue(attribute, delta.getValuesToReplace());
            } else {
                prismValue = null;
            }
        } else if (delta.isDelete()) {
            // No values to add nor to replace, only deletions. Because we narrowed the delta before, we know that this delete
            // delta is relevant - i.e. after its application, the (single) value of property will be removed.
            prismValue = null;
        } else {
            // This should not occur. The narrowing process eliminates empty deltas.
            LOGGER.warn("Empty delta {} for attribute {} of {} -- skipping it", delta, attribute.getName(), bean.getClass().getName());
            return;
        }

        Object value = prismValue != null ? prismValue.getRealValue() : null;

        //noinspection unchecked
        Object valueMapped = prismEntityMapper.map(value, outputType);

        try {
            PropertyUtils.setSimpleProperty(bean, attribute.getName(), valueMapped);
        } catch (Exception ex) {
            throw new SystemException("Couldn't set simple property for '" + attribute.getName() + "'", ex);
        }
    }

    private PrismValue getSingleValue(Attribute<?, ?> attribute, Collection<? extends PrismValue> valuesToSet) {
        Set<PrismValue> uniqueValues = new HashSet<>(valuesToSet);
        // This uniqueness check might be too strict: the values can be different from the point of default equality,
        // yet the final verdict (when applying the delta to prism structure) can be that they are equal.
        // Therefore we issue only a warning here.
        // TODO We should either use the same "equals" algorithm as used for delta application, or we should apply the delta
        //  first and here only take the result. But this is really a nitpicking now. ;)
        if (uniqueValues.size() > 1) {
            LOGGER.warn("More than one value to be put into single-valued repository item. This operation will probably "
                    + "fail later because of delta execution in prism. If not, please report an error to the developers. "
                    + "Values = {}, attribute = {}", uniqueValues, attribute);
        }
        return uniqueValues.iterator().next();
    }

    private void handleElementCollection(Collection collection, ItemDelta delta, Attribute attribute, Object bean, PrismObject prismObject, PrismIdentifierGenerator idGenerator) {
        handleOneToMany(collection, delta, attribute, bean, prismObject, idGenerator);
    }

    private void handleOneToMany(Collection collection, ItemDelta delta, Attribute attribute, Object bean, PrismObject prismObject, PrismIdentifierGenerator idGenerator) {
        Class outputType = getRealOutputType(attribute);

        Item item = prismObject.findItem(delta.getPath());

        // handle replace
        if (delta.isReplace()) {
            //noinspection unchecked
            Collection<PrismEntityPair<?>> valuesToReplace = processDeltaValues(delta.getValuesToReplace(), outputType, delta, bean);
            replaceValues(collection, valuesToReplace, item, idGenerator);
            return;
        }

        // handle add
        if (delta.isAdd()) {
            //noinspection unchecked
            Collection<PrismEntityPair<?>> valuesToAdd = processDeltaValues(delta.getValuesToAdd(), outputType, delta, bean);
            addValues(collection, valuesToAdd, idGenerator);
        }

        // handle delete
        if (delta.isDelete()) {
            //noinspection unchecked
            Collection<PrismEntityPair<?>> valuesToDelete = processDeltaValues(delta.getValuesToDelete(), outputType, delta, bean);
            valuesToDelete.forEach(pair -> {
                if (pair.getRepository() instanceof EntityState) {
                    ((EntityState) pair.getRepository()).setTransient(false);
                }
            });
            deleteValues(collection, valuesToDelete, item);
        }
    }

    private Collection<PrismEntityPair> processDeltaValues(Collection<? extends PrismValue> values, Class outputType,
            ItemDelta delta, Object bean) {
        if (values == null) {
            return new ArrayList<>();
        }

        Collection<PrismEntityPair> results = new ArrayList<>();
        for (PrismValue value : values) {
            MapperContext context = new MapperContext();
            context.setRepositoryContext(new RepositoryContext(repositoryService, prismContext, relationRegistry,
                    extItemDictionary, baseHelper.getConfiguration()));
            context.setDelta(delta);
            context.setOwner(bean);

            //noinspection unchecked
            Object result = prismEntityMapper.mapPrismValue(value, outputType, context);
            results.add(new PrismEntityPair<>(value, result));
        }

        return results;
    }

    private Class getRealOutputType(Attribute attribute) {
        Class type = attribute.getJavaType();
        if (!Collection.class.isAssignableFrom(type)) {
            return type;
        }

        Method method = (Method) attribute.getJavaMember();
        ParameterizedType parametrized = (ParameterizedType) method.getGenericReturnType();
        Type t = parametrized.getActualTypeArguments()[0];
        if (t instanceof Class) {
            return (Class) t;
        }

        parametrized = (ParameterizedType) t;
        return (Class) parametrized.getRawType();
    }

    private Object invoke(Object object, Method method) {
        try {
            return method.invoke(object);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new SystemException("Couldn't invoke method '" + method.getName() + "' on object '" + object + "'", ex);
        }
    }

    /**
     * add with overwrite
     */
    @SuppressWarnings("unused")
    public <T extends ObjectType> RObject update(PrismObject<T> object, RObject objectToMerge,
            boolean noFetchExtensionValueInsertionForbidden, Session session) {

        return merge(objectToMerge, session); // todo implement
    }

    private <T extends ObjectType> RObject merge(RObject object, Session session) {
        //noinspection unchecked
        return (RObject) session.merge(object);
    }

    private static class TypeValuePair {

        private ManagedType<?> type;
        private Object value;
    }
}
