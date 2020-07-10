/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.delta;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.helpers.modify.PrismEntityPair;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.repo.sql.util.PrismIdentifierGenerator;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.beanutils.PropertyUtils;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static com.evolveum.midpoint.repo.sql.helpers.modify.DeltaUpdaterUtils.*;

/**
 * Handles general (the most common) case of item delta application.
 * Tries to find appropriate R-class member and update its value,
 * causing respective DB update to be scheduled by Hibernate.
 */
class GeneralUpdate extends BaseUpdate {

    private static final Trace LOGGER = TraceManager.getTrace(GeneralUpdate.class);
    private final PrismObject<? extends ObjectType> prismObject;
    private final ManagedType<? extends ObjectType> mainEntityType;

    <T extends ObjectType> GeneralUpdate(RObject object, ItemDelta<?, ?> delta, PrismObject<T> prismObject, ManagedType<T> mainEntityType, UpdateContext ctx) {
        super(object, delta, ctx);
        this.prismObject = prismObject;
        this.mainEntityType = mainEntityType;
    }

    void handleItemDelta() throws SchemaException {
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
                RAssignment assignment = (RAssignment) currentValueTypePair.value;
                if (QNameUtil.match(name, AssignmentType.F_EXTENSION)) {
                    if (segments.hasNext()) {
                        AssignmentExtensionUpdate extensionUpdate = new AssignmentExtensionUpdate(object, assignment, delta, ctx);
                        extensionUpdate.handleItemDelta();
                    } else {
                        AssignmentExtensionUpdate.handleWholeContainerDelta(object, assignment, delta, ctx);
                    }
                    break;
                } else if (QNameUtil.match(name, AssignmentType.F_METADATA)) {
                    if (!segments.hasNext()) {
                        MetadataUpdate metadataUpdate = new MetadataUpdate(object, assignment, delta, ctx);
                        metadataUpdate.handleWholeContainerDelta();
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

    private Attribute findAttribute(TypeValuePair typeValuePair, String nameLocalPart, Iterator<?> segments, ItemName name) {
        Attribute attribute = beans.entityRegistry.findAttribute(typeValuePair.type, nameLocalPart);
        if (attribute != null) {
            return attribute;
        }

        Attribute<?, ?> attributeOverride = beans.entityRegistry.findAttributeOverride(typeValuePair.type, nameLocalPart);
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
            if (!beans.entityRegistry.hasAttributePathOverride(typeValuePair.type, subPath)) {
                subPath = subPath.allUpToLastName();
                break;
            }

            segment = segments.next();
            if (!ItemPath.isName(segment)) {
                return null;
            }
            subPath = subPath.append(segment);
        }

        return beans.entityRegistry.findAttributePathOverride(typeValuePair.type, subPath);
    }

    private void stepThroughAttribute(Attribute attribute, TypeValuePair step, Iterator<?> segments) {
        Method method = (Method) attribute.getJavaMember();

        switch (attribute.getPersistentAttributeType()) {
            case EMBEDDED:
                step.type = beans.entityRegistry.getMapping(attribute.getJavaType());
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
                        step.type = beans.entityRegistry.getMapping(clazz);
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
        Object valueMapped = beans.prismEntityMapper.map(value, outputType);

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

        } else {

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
    }

    private Collection<PrismEntityPair> processDeltaValues(Collection<? extends PrismValue> values, Class outputType,
            ItemDelta delta, Object bean) {
        if (values == null) {
            return new ArrayList<>();
        }

        Collection<PrismEntityPair> results = new ArrayList<>();
        for (PrismValue value : values) {
            MapperContext context = new MapperContext();
            context.setRepositoryContext(beans.createRepositoryContext());
            context.setDelta(delta);
            context.setOwner(bean);

            //noinspection unchecked
            Object result = beans.prismEntityMapper.mapPrismValue(value, outputType, context);
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
}
