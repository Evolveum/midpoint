/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.helpers;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RObjectTextInfo;
import com.evolveum.midpoint.repo.sql.data.common.any.*;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.helpers.modify.EntityRegistry;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.helpers.modify.PrismEntityMapper;
import com.evolveum.midpoint.repo.sql.helpers.modify.PrismEntityPair;
import com.evolveum.midpoint.repo.sql.util.PrismIdentifierGenerator;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FullTextSearchConfigurationUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FullTextSearchConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.evolveum.midpoint.repo.sql.helpers.modify.DeltaUpdaterUtils.*;

/**
 * @author Viliam Repan (lazyman).
 */
@Component
public class ObjectDeltaUpdater {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectDeltaUpdater.class);

    @Autowired
    private PrismContext prismContext;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private EntityRegistry entityRegistry;
    @Autowired
    private PrismEntityMapper prismEntityMapper;

    /**
     * modify
     */
    public <T extends ObjectType> RObject<T> modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
                                                          PrismObject<T> prismObject, Session session) throws SchemaException {

        LOGGER.debug("Starting to build entity changes based on delta via reference");

        // normalize reference.relation qnames like it's done here ObjectTypeUtil.normalizeAllRelations(prismObject);

        // how to generate identifiers correctly now? to repo entities and to full xml, ids in full XML are generated
        // on different place than we later create new containers...how to match them

        // set proper owner/ownerOid/ownerType for containers/references/result and others

        // todo implement transformation from prism to entity (PrismEntityMapper), probably ROperationResult missing

        // validate lookup tables and certification campaigns

        // mark newly added containers/references as transient

        // validate metadata/*, assignment/metadata/*, assignment/construction/resourceRef changes

        // preprocess modifications
        PrismObject changed = prismObject.clone();
        ItemDelta.applyTo(modifications, changed);
        Collection<? extends ItemDelta> processedModifications = prismObject.diffModifications(changed, true, true);

        // process only real modifications
        Class<? extends RObject> objectClass = RObjectType.getByJaxbType(type).getClazz();
        RObject<T> object = session.byId(objectClass).getReference(oid);

        ManagedType mainEntityType = entityRegistry.getJaxbMapping(type);

        for (ItemDelta delta : processedModifications) {
            ItemPath path = delta.getPath();

            if (isObjectExtensionDelta(path) || isShadowAttributesDelta(path)) {
                handleObjectExtensionOrAttributesDelta(object, delta);
                continue;
            }

            AttributeStep attributeStep = new AttributeStep();
            attributeStep.managedType = mainEntityType;
            attributeStep.bean = object;

            Iterator<ItemPathSegment> segments = path.getSegments().iterator();
            while (segments.hasNext()) {
                ItemPathSegment segment = segments.next();
                if (!(segment instanceof NameItemPathSegment)) {
                    throw new SystemException("Segment '" + segment + "' in '" + path + "' is not a name item");
                }

                NameItemPathSegment nameSegment = (NameItemPathSegment) segment;
                String nameLocalPart = nameSegment.getName().getLocalPart();

                if (isAssignmentExtensionDelta(attributeStep, nameSegment)) {
                    handleAssignmentExtensionDelta((RAssignment) attributeStep.bean, delta);
                }

                Attribute attribute = findAttribute(attributeStep, nameLocalPart, path, segments, nameSegment);
                if (attribute == null) {
                    // there's no table/column that needs update
                    break;
                }

                if (segments.hasNext()) {
                    attributeStep = stepThroughAttribute(attribute, attributeStep, segments);

                    continue;
                }

                handleAttribute(attribute, attributeStep.bean, delta);

                if ("name".equals(attribute.getName()) && RObject.class.isAssignableFrom(attribute.getDeclaringType().getJavaType())) {
                    // we also need to handle "nameCopy" column, we doesn't need path/segments/nameSegment for this call
                    Attribute nameCopyAttribute = findAttribute(attributeStep, "nameCopy", null, null, null);
                    handleAttribute(nameCopyAttribute, attributeStep.bean, delta);
                }
            }
        }

        handleObjectCommonAttributes(type, processedModifications, prismObject, object);

        LOGGER.debug("Entity changes applied");

        return object;
    }

    private <T extends ObjectType> void handleObjectCommonAttributes(Class<T> type, Collection<? extends ItemDelta> modifications,
                                                                     PrismObject<T> prismObject, RObject object) throws SchemaException {
        // update version
        String strVersion = prismObject.getVersion();
        int version = StringUtils.isNotEmpty(strVersion) && strVersion.matches("[0-9]*") ? Integer.parseInt(strVersion) + 1 : 1;
        object.setVersion(version);

        // apply modifications, ids' for new containers already filled in delta values
        ItemDelta.applyTo(modifications, prismObject);

        handleObjectTextInfoChanges(type, modifications, prismObject, object);

        // generate ids for containers that weren't handled in previous step (not processed by repository)
        PrismIdentifierGenerator generator = new PrismIdentifierGenerator();
        generator.generate(prismObject, PrismIdentifierGenerator.Operation.MODIFY);

        // normalize all relations
        ObjectTypeUtil.normalizeAllRelations(prismObject);

        // full object column will be updated later
    }

    private <T extends ObjectType> boolean isObjectTextInfoRecomputationNeeded(Class<T> type, Collection<? extends ItemDelta> modifications) {
        FullTextSearchConfigurationType config = repositoryService.getFullTextSearchConfiguration();
        if (!FullTextSearchConfigurationUtil.isEnabled(config)) {
            return false;
        }

        Set<ItemPath> paths = FullTextSearchConfigurationUtil.getFullTextSearchItemPaths(config, type);

        for (ItemDelta modification : modifications) {
            ItemPath modPath = modification.getPath();
            ItemPath namesOnly = modPath.namedSegmentsOnly();

            for (ItemPath path : paths) {
                if (path.startsWith(namesOnly)) {
                    return true;
                }
            }
        }

        return false;
    }

    private <T extends ObjectType> void handleObjectTextInfoChanges(Class<T> type, Collection<? extends ItemDelta> modifications,
                                                                    PrismObject prismObject, RObject object) {
        // update object text info if necessary
        if (!isObjectTextInfoRecomputationNeeded(type, modifications)) {
            return;
        }

        Set<RObjectTextInfo> infos = RObjectTextInfo.createItemsSet((ObjectType) prismObject.asObjectable(), object,
                new RepositoryContext(repositoryService, prismContext));

        if (infos == null || infos.isEmpty()) {
            object.getTextInfoItems().clear();
        } else {
            // todo improve this replace
            object.getTextInfoItems().clear();
            object.getTextInfoItems().addAll(infos);
        }
    }

    private boolean isObjectExtensionDelta(ItemPath path) {
        return path.startsWithName(ObjectType.F_EXTENSION);
    }

    private boolean isShadowAttributesDelta(ItemPath path) {
        return path.startsWithName(ShadowType.F_ATTRIBUTES);
    }

    private boolean isAssignmentExtensionDelta(AttributeStep attributeStep, NameItemPathSegment nameSegment) {
        if (!(attributeStep.bean instanceof RAssignment)) {
            return false;
        }

        if (!AssignmentType.F_EXTENSION.equals(nameSegment.getName())) {
            return false;
        }

        return true;
    }

    private void handleAssignmentExtensionDelta(RAssignment assignment, ItemDelta delta) {
        RAssignmentExtension extension = assignment.getExtension();
        if (extension == null) {
            extension = new RAssignmentExtension();
            extension.setOwner(assignment);
            extension.setTransient(true);

            assignment.setExtension(extension);
        }

        processAnyExtensionDeltaValues(delta, null, null, extension, RAssignmentExtensionType.EXTENSION);
    }

    private void processAnyExtensionDeltaValues(Collection<PrismValue> values,
                                                RObject object,
                                                RObjectExtensionType objectOwnerType,
                                                RAssignmentExtension assignmentExtension,
                                                RAssignmentExtensionType assignmentExtensionType,
                                                BiConsumer<Collection<? extends RAnyValue>, Collection<PrismEntityPair<RAnyValue>>> processObjectValues) {

        RAnyConverter converter = new RAnyConverter(prismContext);

        if (values == null || values.isEmpty()) {
            return;
        }

        try {
            Collection<PrismEntityPair<RAnyValue>> extValues = new ArrayList<>();
            for (PrismValue value : values) {
                RAnyValue extValue = converter.convertToRValue(value, object == null);
                if (extValue == null) {
                    continue;
                }

                extValues.add(new PrismEntityPair(value, extValue));
            }

            if (extValues.isEmpty()) {
                // no changes in indexed values
                return;
                // todo can't return if new "values" collection is empty, if it was REPLACE with "nothing" we have to remove proper attributes
            }

            Class type = null;
            if (!extValues.isEmpty()) {
                RAnyValue first = extValues.iterator().next().getRepository();
                type = first.getClass();
            }

            if (object != null) {
                extValues.stream().forEach(item -> {
                    ROExtValue val = (ROExtValue) item.getRepository();
                    val.setOwner(object);
                    val.setOwnerType(objectOwnerType);
                });

                processObjectExtensionValues(object, type,
                        (existing) -> processObjectValues.accept(existing, extValues));
            } else {
                extValues.stream().forEach(item -> {
                    RAExtValue val = (RAExtValue) item.getRepository();
                    val.setAnyContainer(assignmentExtension);
                    val.setExtensionType(assignmentExtensionType);
                });

                processAssignmentExtensionValues(assignmentExtension, type,
                        (existing) -> processObjectValues.accept(existing, extValues));
            }
        } catch (SchemaException ex) {
            throw new SystemException("Couldn't process extension attributes", ex);
        }
    }

    private Collection<RAnyValue> filterRAnyValues(Collection<? extends RAnyValue> existing, ItemDefinition def,
                                                   RObjectExtensionType objectOwnerType, RAssignmentExtensionType assignmentExtensionType) {

        Collection<RAnyValue> filtered = new ArrayList<>();
        for (RAnyValue value : existing) {
            if (!value.getName().equals(RUtil.qnameToString(def.getName()))
                    || !value.getType().equals(RUtil.qnameToString(def.getTypeName()))) {
                continue;
            }

            if (value instanceof ROExtValue) {
                ROExtValue oValue = (ROExtValue) value;
                if (!objectOwnerType.equals(oValue.getOwnerType())) {
                    continue;
                }
            } else if (value instanceof RAExtValue) {
                RAExtValue aValue = (RAExtValue) value;
                if (!assignmentExtensionType.equals(aValue.getExtensionType())) {
                    continue;
                }
            }

            filtered.add(value);
        }

        return filtered;
    }

    private void processAnyExtensionDeltaValues(ItemDelta delta,
                                                RObject object,
                                                RObjectExtensionType objectOwnerType,
                                                RAssignmentExtension assignmentExtension,
                                                RAssignmentExtensionType assignmentExtensionType) {
        // handle replace
        if (delta.getValuesToReplace() != null && !delta.getValuesToReplace().isEmpty()) {
            processAnyExtensionDeltaValues(delta.getValuesToReplace(), object, objectOwnerType, assignmentExtension, assignmentExtensionType,
                    (existing, fromDelta) -> {
                        ItemDefinition def = delta.getDefinition();
                        Collection<RAnyValue> filtered = filterRAnyValues(existing, def, objectOwnerType, assignmentExtensionType);

                        if (fromDelta.isEmpty()) {
                            // if there are not new values, we just remove existing ones
                            existing.removeAll(filtered);
                            return;
                        }

                        Collection<RAnyValue> toDelete = new ArrayList<>();
                        Collection<PrismEntityPair<?>> toAdd = new ArrayList<>();

                        Set<Object> justValuesToReplace = new HashSet<>();
                        for (PrismEntityPair<RAnyValue> pair : fromDelta) {
                            justValuesToReplace.add(pair.getRepository().getValue());
                        }

                        for (RAnyValue value : filtered) {
                            if (justValuesToReplace.contains(value.getValue())) {
                                // do not replace with the same one - don't touch
                                justValuesToReplace.remove(value.getValue());
                            } else {
                                toDelete.add(value);
                            }
                        }

                        for (PrismEntityPair<RAnyValue> pair : fromDelta) {
                            if (justValuesToReplace.contains(pair.getRepository().getValue())) {
                                toAdd.add(pair);
                            }
                        }

                        existing.removeAll(toDelete);
                        markNewOnesTransientAndAddToExisting(existing, toAdd);
                    });
            return;
        }

        // handle delete
        processAnyExtensionDeltaValues(delta.getValuesToDelete(), object, objectOwnerType, assignmentExtension, assignmentExtensionType,
                (existing, fromDelta) -> existing.removeAll(fromDelta));

        // handle add
        processAnyExtensionDeltaValues(delta.getValuesToAdd(), object, objectOwnerType, assignmentExtension, assignmentExtensionType,
                (existing, fromDelta) -> markNewOnesTransientAndAddToExisting(existing, (Collection) fromDelta));
    }

    private void processAssignmentExtensionValues(RAssignmentExtension extension, Class<? extends RAExtValue> type,
                                                  Consumer<Collection<? extends RAExtValue>> processObjectValues) {

        if (type.equals(RAExtDate.class)) {
            processObjectValues.accept(extension.getDates());
            Short count = getCount(extension.getDates());
            extension.setDatesCount(count);
        } else if (type.equals(RAExtLong.class)) {
            processObjectValues.accept(extension.getLongs());
            Short count = getCount(extension.getLongs());
            extension.setLongsCount(count);
        } else if (type.equals(RAExtReference.class)) {
            processObjectValues.accept(extension.getReferences());
            Short count = getCount(extension.getReferences());
            extension.setReferencesCount(count);
        } else if (type.equals(RAExtString.class)) {
            processObjectValues.accept(extension.getStrings());
            Short count = getCount(extension.getStrings());
            extension.setStringsCount(count);
        } else if (type.equals(RAExtPolyString.class)) {
            processObjectValues.accept(extension.getPolys());
            Short count = getCount(extension.getPolys());
            extension.setPolysCount(count);
        } else if (type.equals(RAExtBoolean.class)) {
            processObjectValues.accept(extension.getBooleans());
            Short count = getCount(extension.getBooleans());
            extension.setBooleansCount(count);
        }
    }

    private void processObjectExtensionValues(RObject object, Class<? extends ROExtValue> type,
                                              Consumer<Collection<ROExtValue>> processObjectValues) {

        if (type.equals(ROExtDate.class)) {
            processObjectValues.accept(object.getDates());
            Short count = getCount(object.getDates());
            object.setDatesCount(count);
        } else if (type.equals(ROExtLong.class)) {
            processObjectValues.accept(object.getLongs());
            Short count = getCount(object.getLongs());
            object.setLongsCount(count);
        } else if (type.equals(ROExtReference.class)) {
            processObjectValues.accept(object.getReferences());
            Short count = getCount(object.getReferences());
            object.setReferencesCount(count);
        } else if (type.equals(ROExtString.class)) {
            processObjectValues.accept(object.getStrings());
            Short count = getCount(object.getStrings());
            object.setStringsCount(count);
        } else if (type.equals(ROExtPolyString.class)) {
            processObjectValues.accept(object.getPolys());
            Short count = getCount(object.getPolys());
            object.setPolysCount(count);
        } else if (type.equals(ROExtBoolean.class)) {
            processObjectValues.accept(object.getBooleans());
            Short count = getCount(object.getBooleans());
            object.setBooleansCount(count);
        }
    }

    private Short getCount(Collection collection) {
        if (collection == null) {
            return 0;
        }

        return Integer.valueOf(collection.size()).shortValue();
    }

    private void handleObjectExtensionOrAttributesDelta(RObject object, ItemDelta delta) {
        RObjectExtensionType ownerType = null;
        if (isObjectExtensionDelta(delta.getPath())) {
            ownerType = RObjectExtensionType.EXTENSION;
        } else if (isShadowAttributesDelta(delta.getPath())) {
            ownerType = RObjectExtensionType.ATTRIBUTES;
        }

        processAnyExtensionDeltaValues(delta, object, ownerType, null, null);
    }

    private Attribute findAttribute(AttributeStep attributeStep, String nameLocalPart, ItemPath path,
                                    Iterator<ItemPathSegment> segments, NameItemPathSegment nameSegment) {
        Attribute attribute = entityRegistry.findAttribute(attributeStep.managedType, nameLocalPart);
        if (attribute != null) {
            return attribute;
        }

        attribute = entityRegistry.findAttributeOverride(attributeStep.managedType, nameLocalPart);
        if (attribute != null) {
            return attribute;
        }

        if (!segments.hasNext()) {
            return null;
        }

        // try to search path overrides like metadata/* or assignment/metadata/* or assignment/construction/resourceRef
        ItemPathSegment segment;
        ItemPath subPath = new ItemPath(nameSegment);
        while (segments.hasNext()) {
            if (!entityRegistry.hasAttributePathOverride(attributeStep.managedType, subPath)) {
                subPath = subPath.allUpToLastNamed();
                break;
            }

            segment = segments.next();
            if (!(segment instanceof NameItemPathSegment)) {
                throw new SystemException("Segment '" + segment + "' in '" + path + "' is not a name item");
            }

            nameSegment = (NameItemPathSegment) segment;

            subPath = subPath.append(nameSegment.getName());
        }

        return entityRegistry.findAttributePathOverride(attributeStep.managedType, subPath);
    }

    private AttributeStep stepThroughAttribute(Attribute attribute, AttributeStep step, Iterator<ItemPathSegment> segments) {
        Method method = (Method) attribute.getJavaMember();

        switch (attribute.getPersistentAttributeType()) {
            case EMBEDDED:
                step.managedType = entityRegistry.getMapping(attribute.getJavaType());
                Object child = invoke(step.bean, method);
                if (child == null) {
                    // embedded entity doesn't exist we have to create it first, so it can be populated later
                    Class childType = getRealOutputType(attribute);
                    try {
                        child = childType.newInstance();
                        PropertyUtils.setSimpleProperty(step.bean, attribute.getName(), child);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                        throw new SystemException("Couldn't create new instance of '" + childType.getName()
                                + "', attribute '" + attribute.getName() + "'", ex);
                    }
                }
                step.bean = child;
                break;
            case ONE_TO_MANY:
                // object extension is handled separately, only {@link Container} and references are handled here
                Class clazz = getRealOutputType(attribute);

                IdItemPathSegment id = (IdItemPathSegment) segments.next();

                Collection c = (Collection) invoke(step.bean, method);
                if (!Container.class.isAssignableFrom(clazz)) {
                    throw new SystemException("Don't know how to go through collection of '" + getRealOutputType(attribute) + "'");
                }

                boolean found = false;
                for (Container o : (Collection<Container>) c) {
                    long l = o.getId().longValue();
                    if (l == id.getId()) {
                        step.managedType = entityRegistry.getMapping(clazz);
                        step.bean = o;

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

        return step;
    }

    private void handleAttribute(Attribute attribute, Object bean, ItemDelta delta) {
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
                handleOneToMany(oneToMany, delta, attribute, bean);
                break;
            case ELEMENT_COLLECTION:
                Collection elementCollection = (Collection) invoke(bean, method);
                handleElementCollection(elementCollection, delta, attribute, bean);
                break;
        }
    }

    private void handleBasicOrEmbedded(Object bean, ItemDelta delta, Attribute attribute) {
        Class outputType = getRealOutputType(attribute);

        PrismValue anyPrismValue = delta.getAnyValue();

        Object value;
        if (delta.isDelete()
                || (delta.isReplace() && (anyPrismValue == null || anyPrismValue.isEmpty()))) {
            value = null;
        } else {
            value = anyPrismValue.getRealValue();
        }

        value = prismEntityMapper.map(value, outputType);

        try {
            PropertyUtils.setSimpleProperty(bean, attribute.getName(), value);
        } catch (Exception ex) {
            throw new SystemException("Couldn't set simple property for '" + attribute.getName() + "'", ex);
        }
    }

    private void handleElementCollection(Collection collection, ItemDelta delta, Attribute attribute, Object bean) {
        handleOneToMany(collection, delta, attribute, bean);
    }

    private void handleOneToMany(Collection collection, ItemDelta delta, Attribute attribute, Object bean) {
        Class outputType = getRealOutputType(attribute);

        // handle replace
        if (delta.isReplace()) {
            Collection<PrismEntityPair<?>> valuesToReplace = processDeltaValues(delta.getValuesToReplace(), outputType, delta, bean);
            replaceValues(collection, valuesToReplace);
            return;
        }

        // handle add
        if (delta.isAdd()) {
            Collection<PrismEntityPair<?>> valuesToAdd = processDeltaValues(delta.getValuesToAdd(), outputType, delta, bean);
            addValues(collection, valuesToAdd);
        }

        // handle delete
        if (delta.isDelete()) {
            Collection<PrismEntityPair<?>> valuesToDelete = processDeltaValues(delta.getValuesToDelete(), outputType, delta, bean);
            deleteValues(collection, valuesToDelete);
        }
    }

    private Collection<PrismEntityPair> processDeltaValues(Collection<? extends PrismValue> values, Class outputType,
                                                           ItemDelta delta, Object bean) {
        if (values == null) {
            return new ArrayList();
        }

        Collection<PrismEntityPair> results = new ArrayList();
        for (PrismValue value : values) {
            MapperContext context = new MapperContext();
            context.setRepositoryContext(new RepositoryContext(repositoryService, prismContext));
            context.setDelta(delta);
            context.setOwner(bean);

            Object result = prismEntityMapper.mapPrismValue(value, outputType, context);
            results.add(new PrismEntityPair(value, result));
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
    public <T extends ObjectType> RObject<T> update(PrismObject<T> object, RObject<T> objectToMerge, Session session,
                                                    OperationResult result) {

        return merge(objectToMerge, session); // todo implement
    }

    private <T extends ObjectType> RObject<T> merge(RObject<T> object, Session session) {
        return (RObject) session.merge(object);
    }

    private static class AttributeStep {

        private ManagedType managedType;
        private Object bean;
    }
}
