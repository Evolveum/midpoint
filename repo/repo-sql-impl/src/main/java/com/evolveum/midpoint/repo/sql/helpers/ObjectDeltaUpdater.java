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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.*;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.helpers.modify.PrismEntityMapper;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.commons.beanutils.PropertyUtils;
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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Viliam Repan (lazyman).
 */
@Component
public class ObjectDeltaUpdater {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectDeltaUpdater.class);

    @Autowired
    private EntityModificationRegistry entityModificationRegistry;
    @Autowired
    private PrismContext prismContext;

    private PrismEntityMapper prismEntityMapper = new PrismEntityMapper();

    /**
     * modify
     */
    public <T extends ObjectType> RObject<T> update(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
                                                    RObject<T> objectToMerge, Session session, OperationResult result) {

        LOGGER.debug("Starting to build entity changes based on delta via reference");

        if (1 == 1) {
            return merge(objectToMerge, session);
        }

        // todo how to generate identifiers correctly now? to repo entities and to full xml, ids in full XML are generated on different place than we later create new containers...how to match them

        // todo set proper owner/ownerOid for containers/references

        // todo implement transformation from prism to entity (PrismEntityMapper)

        // todo validate lookup tables and certification campaigns

        // todo mark newly added containers/references as transient

        // todo don't store non indexed extension attributes

        RObject object = session.byId(objectToMerge.getClass()).getReference(oid);
        object.setVersion(objectToMerge.getVersion());
        object.setFullObject(objectToMerge.getFullObject());

        ManagedType mainEntityType = entityModificationRegistry.getJaxbMapping(type);

        for (ItemDelta delta : modifications) {
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

                Attribute attribute = findAttribute(attributeStep, nameLocalPart);
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
                    // we also need to handle "nameCopy" column
                    Attribute nameCopyAttribute = findAttribute(attributeStep, "nameCopy");
                    handleAttribute(nameCopyAttribute, attributeStep.bean, delta);
                }
            }
        }

        LOGGER.debug("Saving object");

        session.save(object);

        LOGGER.debug("Object saved");

        return objectToMerge;
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

        // todo if extension is empty, null it probably
    }

    private void processAnyExtensionDeltaValues(Collection<PrismValue> values,
                                                RObject object,
                                                RObjectExtensionType objectOwnerType,
                                                RAssignmentExtension assignmentExtension,
                                                RAssignmentExtensionType assignmentExtensionType,
                                                BiConsumer<Collection<? extends RAnyValue>, Collection<RAnyValue>> processObjectValues,
                                                BiFunction<Short, Short, Short> processObjectValuesCount) {

        RAnyConverter converter = new RAnyConverter(prismContext);

        if (values == null || values.isEmpty()) {
            return;
        }

        try {
            Collection<RAnyValue> extValues = new ArrayList<>();
            for (PrismValue value : values) {
                RAnyValue extValue = converter.convertToRValue(value, object == null);
                if (extValue == null) {
                    continue;
                }

                extValues.add(extValue);
            }

            RAnyValue first = extValues.iterator().next();
            Class type = first.getClass();

            if (ROExtValue.class.isAssignableFrom(type)) {
                extValues.stream().forEach(item -> {
                    ROExtValue val = (ROExtValue) item;
                    val.setOwner(object);
                    val.setOwnerType(objectOwnerType);
                });

                processObjectExtensionValues(object, type,
                        (existing) -> processObjectValues.accept(existing, extValues),
                        (existing) -> processObjectValuesCount.apply(existing, (short) extValues.size()));
            } else {
                extValues.stream().forEach(item -> {
                    RAExtValue val = (RAExtValue) item;
                    val.setAnyContainer(assignmentExtension);
                    val.setExtensionType(assignmentExtensionType);
                });

                processAssignmentExtensionValues(assignmentExtension, type,
                        (existing) -> processObjectValues.accept(existing, extValues),
                        (existing) -> processObjectValuesCount.apply(existing, (short) extValues.size()));
            }
        } catch (SchemaException ex) {
            throw new SystemException("Couldn't process extension attributes", ex);
        }
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
                        existing.clear();
                        markNewOnesTransientAndAddToExisting(existing, fromDelta);
                    },
                    (existingCount, fromDeltaCount) -> fromDeltaCount.shortValue());
            return;
        }

        // handle delete
        processAnyExtensionDeltaValues(delta.getValuesToDelete(), object, objectOwnerType, assignmentExtension, assignmentExtensionType,
                (existing, fromDelta) -> existing.removeAll(fromDelta),
                (existingCount, fromDeltaCount) -> (short) (existingCount.shortValue() - fromDeltaCount.shortValue()));

        // handle add
        processAnyExtensionDeltaValues(delta.getValuesToAdd(), object, objectOwnerType, assignmentExtension, assignmentExtensionType,
                (existing, fromDelta) -> markNewOnesTransientAndAddToExisting(existing, fromDelta),
                (existingCount, fromDeltaCount) -> (short) (existingCount.shortValue() + fromDeltaCount.shortValue()));
    }

    private void processAssignmentExtensionValues(RAssignmentExtension extension, Class<? extends RAExtValue> type,
                                                  Consumer<Collection<? extends RAExtValue>> processObjectValues,
                                                  Function<Short, Short> processObjectValuesCount) {

        if (type.equals(RAExtDate.class)) {
            processObjectValues.accept(extension.getDates());
            Short count = processObjectValuesCount.apply(extension.getDatesCount());
            extension.setDatesCount(count);
        } else if (type.equals(RAExtLong.class)) {
            processObjectValues.accept(extension.getLongs());
            Short count = processObjectValuesCount.apply(extension.getLongsCount());
            extension.setLongsCount(count);
        } else if (type.equals(RAExtReference.class)) {
            processObjectValues.accept(extension.getReferences());
            Short count = processObjectValuesCount.apply(extension.getReferencesCount());
            extension.setReferencesCount(count);
        } else if (type.equals(RAExtString.class)) {
            processObjectValues.accept(extension.getStrings());
            Short count = processObjectValuesCount.apply(extension.getStringsCount());
            extension.setStringsCount(count);
        } else if (type.equals(RAExtPolyString.class)) {
            processObjectValues.accept(extension.getPolys());
            Short count = processObjectValuesCount.apply(extension.getPolysCount());
            extension.setPolysCount(count);
        } else if (type.equals(RAExtBoolean.class)) {
            processObjectValues.accept(extension.getBooleans());
            Short count = processObjectValuesCount.apply(extension.getBooleansCount());
            extension.setBooleansCount(count);
        }
    }

    private void processObjectExtensionValues(RObject object, Class<? extends ROExtValue> type,
                                              Consumer<Collection<ROExtValue>> processObjectValues,
                                              Function<Short, Short> processObjectValuesCount) {

        if (type.equals(ROExtDate.class)) {
            processObjectValues.accept(object.getDates());
            Short count = processObjectValuesCount.apply(object.getDatesCount());
            object.setDatesCount(count);
        } else if (type.equals(ROExtLong.class)) {
            processObjectValues.accept(object.getLongs());
            Short count = processObjectValuesCount.apply(object.getLongsCount());
            object.setLongsCount(count);
        } else if (type.equals(ROExtReference.class)) {
            processObjectValues.accept(object.getReferences());
            Short count = processObjectValuesCount.apply(object.getReferencesCount());
            object.setReferencesCount(count);
        } else if (type.equals(ROExtString.class)) {
            processObjectValues.accept(object.getStrings());
            Short count = processObjectValuesCount.apply(object.getStringsCount());
            object.setStringsCount(count);
        } else if (type.equals(ROExtPolyString.class)) {
            processObjectValues.accept(object.getPolys());
            Short count = processObjectValuesCount.apply(object.getPolysCount());
            object.setPolysCount(count);
        } else if (type.equals(ROExtBoolean.class)) {
            processObjectValues.accept(object.getBooleans());
            Short count = processObjectValuesCount.apply(object.getBooleansCount());
            object.setBooleansCount(count);
        }
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

    private Attribute findAttribute(AttributeStep attributeStep, String nameLocalPart) {
        Attribute attribute = entityModificationRegistry.findAttribute(attributeStep.managedType, nameLocalPart);
        if (attribute != null) {
            return attribute;
        }

        return entityModificationRegistry.findAttributeOverride(attributeStep.managedType, nameLocalPart);
    }

    private AttributeStep stepThroughAttribute(Attribute attribute, AttributeStep step, Iterator<ItemPathSegment> segments) {
        Method method = (Method) attribute.getJavaMember();

        switch (attribute.getPersistentAttributeType()) {
            case EMBEDDED:
                step.managedType = entityModificationRegistry.getMapping(attribute.getJavaType());
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
                        step.managedType = entityModificationRegistry.getMapping(clazz);
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
                handleOneToMany(oneToMany, delta, attribute);
                break;
            case ELEMENT_COLLECTION:
                Collection elementCollection = (Collection) invoke(bean, method);
                handleElementCollection(elementCollection, delta, attribute);
                break;
        }
    }

    private void handleBasicOrEmbedded(Object bean, ItemDelta delta, Attribute attribute) {
        Class outputType = getRealOutputType(attribute);

        Object value;
        if (delta.isDelete()) {
            value = null;
        } else {
            value = delta.getAnyValue().getRealValue();
        }

        value = prismEntityMapper.map(value, outputType);

        try {
            PropertyUtils.setSimpleProperty(bean, attribute.getName(), value);
        } catch (Exception ex) {
            throw new SystemException("Couldn't set simple property for '" + attribute.getName() + "'", ex);
        }
    }

    private void handleElementCollection(Collection collection, ItemDelta delta, Attribute attribute) {
        handleOneToMany(collection, delta, attribute);
    }

    private void handleOneToMany(Collection collection, ItemDelta delta, Attribute attribute) {
        Class outputType = getRealOutputType(attribute);

        // handle replace
        Collection valuesToReplace = processDeltaValues(delta.getValuesToReplace(), outputType);
        if (!valuesToReplace.isEmpty()) {
            collection.clear();
            markNewOnesTransientAndAddToExisting(collection, valuesToReplace);

            return;
        }

        // handle delete
        Collection valuesToDelete = processDeltaValues(delta.getValuesToDelete(), outputType);
        Set<Long> containerIdsToDelete = new HashSet<>();
        for (Object obj : valuesToDelete) {
            if (obj instanceof Container) {
                Container container = (Container) obj;

                long id = container.getId().longValue();
                containerIdsToDelete.add(id);
            }
        }

        if (!valuesToDelete.isEmpty()) {
            Collection toDelete = new ArrayList();
            for (Object obj : collection) {
                if (obj instanceof Container) {
                    Container container = (Container) obj;

                    long id = container.getId().longValue();
                    if (containerIdsToDelete.contains(id)) {
                        toDelete.add(container);
                    }
                } else {
                    // e.g. RObjectReference
                    if (valuesToDelete.contains(obj)) {
                        toDelete.add(obj);
                    }
                }
            }
            collection.removeAll(toDelete);
        }

        // handle add
        Collection valuesToAdd = processDeltaValues(delta.getValuesToAdd(), outputType);
        markNewOnesTransientAndAddToExisting(collection, valuesToAdd);
    }

    private void markNewOnesTransientAndAddToExisting(Collection existing, Collection newOnes) {
        for (Object item : newOnes) {
            if (item instanceof EntityState) {
                EntityState es = (EntityState) item;
                es.setTransient(true);
            }
            existing.add(item);
        }
    }

    private Collection processDeltaValues(Collection<? extends PrismValue> values, Class outputType) {
        if (values == null) {
            return new ArrayList();
        }

        Collection results = new ArrayList();
        for (PrismValue value : values) {
            Object result = prismEntityMapper.mapPrismValue(value, outputType);
            results.add(result);
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
