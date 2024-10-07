/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.delta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.evolveum.midpoint.prism.ItemModifyResult;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;

import com.evolveum.midpoint.prism.delta.ContainerDelta;

import org.apache.commons.beanutils.PropertyUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Handles general (the most common) case of item delta application.
 * Tries to find appropriate R-class member and update its value,
 * causing respective DB update to be scheduled by Hibernate.
 */
class GeneralUpdate extends BaseUpdate {

    private static final Trace LOGGER = TraceManager.getTrace(GeneralUpdate.class);

    private final PrismObject<? extends ObjectType> prismObject;

    private final ItemPath path;
    private final Iterator<?> segmentsIterator;

    private Object currentBean;
    private ManagedType<?> currentBeanType;
    private ItemName currentItemName;

    <T extends ObjectType> GeneralUpdate(RObject object, ItemDelta<?, ?> delta, PrismObject<T> prismObject, ManagedType<T> mainEntityType, UpdateContext ctx) {
        super(object, delta, ctx);
        this.path = delta.getPath();
        this.segmentsIterator = path.getSegments().iterator();
        this.prismObject = prismObject;
        this.currentBean = object;
        this.currentBeanType = mainEntityType;
    }

    void handleItemDelta() throws SchemaException {
        try {
            while (segmentsIterator.hasNext()) {
                Object currentPathSegment = segmentsIterator.next();
                if (!ItemPath.isName(currentPathSegment)) {
                    // TODO Why is this? Shouldn't we throw an exception instead?
                    LOGGER.trace("Segment {} in path {} is not name item, finishing entity update for delta", currentPathSegment, path);
                    break;
                }
                currentItemName = ItemPath.toName(currentPathSegment);

                LOGGER.trace("handleItemDelta: current item name = {}, currentBean = {}, currentBeanType = {}",
                        currentItemName, currentBean, currentBeanType.getJavaType());

                if (currentBean instanceof RAssignment) {
                    if (handleAssignment(currentItemName)) {
                        break;
                    }
                }

                Attribute attribute = findAttributeForCurrentState();
                if (attribute == null) {
                    // there's no table/column that needs update
                    break;
                }

                if (segmentsIterator.hasNext()) {
                    stepThroughAttribute(attribute);
                } else {
                    updateAttribute(attribute);
                    updateNameCopyAttribute(attribute);
                }
            }
        } catch (ContainerMissingException e) {
            LOGGER.debug("Delta Item Path: {} to non-existing container" , path, e);
        }
    }

    private void updateNameCopyAttribute(Attribute attribute) {
        if ("name".equals(attribute.getName()) &&
                RObject.class.isAssignableFrom(attribute.getDeclaringType().getJavaType())) {
            Attribute nameCopyAttribute = findAttributeForName("nameCopy");
            assert nameCopyAttribute != null;
            updateAttribute(nameCopyAttribute);
        }
    }

    /**
     * @return true if the delta is completely handled
     */
    private boolean handleAssignment(ItemName name) throws SchemaException {
        RAssignment assignment = (RAssignment) currentBean;
        if (QNameUtil.match(name, AssignmentType.F_EXTENSION)) {
            if (segmentsIterator.hasNext()) {
                AssignmentExtensionUpdate extensionUpdate = new AssignmentExtensionUpdate(object, assignment, delta, ctx);
                extensionUpdate.handleItemDelta();
            } else {
                AssignmentExtensionUpdate.handleWholeContainerDelta(object, assignment, delta, ctx);
            }
            return true;
        } else if (QNameUtil.match(name, AssignmentType.F_METADATA)) {
            if (!segmentsIterator.hasNext()) {
                MetadataUpdate metadataUpdate = new MetadataUpdate(object, assignment, delta, ctx);
                metadataUpdate.handleWholeContainerDelta();
                return true;
            }
        }
        return false;
    }

    protected Attribute findAttributeForCurrentState() {
        Attribute attribute = findAttributeForName(currentItemName.getLocalPart());
        if (attribute != null) {
            return attribute;
        } else if (segmentsIterator.hasNext()) {
            return findAttributePathOverrideIfExists();
        } else {
            return null;
        }
    }

    private Attribute findAttributeForName(String name) {
        Attribute attribute = beans.entityRegistry.findAttribute(currentBeanType, name);
        if (attribute != null) {
            return attribute;
        } else {
            return beans.entityRegistry.findAttributeOverride(currentBeanType, name);
        }
    }

    // try to search path overrides like metadata/* or assignment/metadata/* or assignment/construction/resourceRef
    @Nullable
    protected Attribute findAttributePathOverrideIfExists() {
        ItemPath subPath = currentItemName;
        while (segmentsIterator.hasNext()) {
            if (beans.entityRegistry.hasAttributePathOverride(currentBeanType, subPath)) {
                Object nextSegment = segmentsIterator.next();
                if (ItemPath.isName(nextSegment)) {
                    subPath = subPath.append(nextSegment);
                } else {
                    // TODO is this ok?
                    return null;
                }
            } else {
                subPath = subPath.allUpToLastName();
                break;
            }
        }

        return beans.entityRegistry.findAttributePathOverride(currentBeanType, subPath);
    }

    private void stepThroughAttribute(Attribute attribute) throws ContainerMissingException {
        Method method = (Method) attribute.getJavaMember();

        switch (attribute.getPersistentAttributeType()) {
            case EMBEDDED:
                stepIntoSingleValue(attribute, method);
                break;
            case ONE_TO_MANY:
                // object extension is handled separately, only {@link Container} and references are handled here
                Long id = ItemPath.toId(segmentsIterator.next());
                stepIntoContainerValue(attribute, method, id);
                break;
            case MANY_TO_ONE:
                break;
            case ONE_TO_ONE: // assignment extension is handled separately
            case BASIC:
            case MANY_TO_MANY:
            case ELEMENT_COLLECTION:
                break;
            default:
                // nothing to do for other cases
        }
    }

    private void stepIntoSingleValue(Attribute attribute, Method method) {
        Object child = invoke(method);
        if (child != null) {
            currentBean = child;
        } else {
            currentBean = createAndSetNewAttributeValue(attribute);
        }
        //noinspection unchecked
        currentBeanType = beans.entityRegistry.getMapping(attribute.getJavaType());
    }

    // embedded entity doesn't exist, so we have to create it first in order to be populated later
    @NotNull
    private Object createAndSetNewAttributeValue(Attribute attribute) {
        Class<?> childType = getAttributeValueType(attribute);
        try {
            Object child = childType.getDeclaredConstructor().newInstance();
            PropertyUtils.setSimpleProperty(currentBean, attribute.getName(), child);
            return child;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new SystemException("Couldn't create new instance of '" + childType.getName()
                    + "', attribute '" + attribute.getName() + "'", ex);
        }
    }

    private void stepIntoContainerValue(Attribute attribute, Method method, Long id) throws ContainerMissingException {
        Class clazz = getAttributeValueType(attribute);
        Collection c = (Collection) invoke(method);
        if (!Container.class.isAssignableFrom(clazz)) {
            throw new SystemException("Don't know how to go through collection of '" + getAttributeValueType(attribute) + "'");
        }

        //noinspection unchecked
        for (Container o : (Collection<Container>) c) {
            long l = o.getId().longValue();
            if (l == id) {
                currentBean = o;
                //noinspection unchecked
                currentBeanType = beans.entityRegistry.getMapping(clazz);
                return;
            }
        }

        throw new ContainerMissingException("Couldn't find container of type '" + getAttributeValueType(attribute)
                + "' with id '" + id + "'");
    }

    private void updateAttribute(Attribute attribute) {
        LOGGER.trace("Updating attribute {}", attribute.getName());

        switch (attribute.getPersistentAttributeType()) {
            case BASIC:
            case EMBEDDED:
                updateBasicOrEmbedded(attribute);
                break;
            case MANY_TO_MANY:
                // not used in our mappings
                throw new SystemException("Don't know how to handle @ManyToMany relationship, should not happen");
            case ONE_TO_ONE:
                // assignment extension was handled separately
                break;
            case MANY_TO_ONE:
                // this can't be in delta (probably)
                throw new SystemException("Don't know how to handle @ManyToOne relationship, should not happen");
            case ONE_TO_MANY:
            case ELEMENT_COLLECTION:
                // object extension is handled separately, only {@link Container} and references are handled here
                updateCollection(attribute);
                break;
        }
    }

    private void updateCollection(Attribute attribute) {
        Method method = (Method) attribute.getJavaMember();
        Collection collection = (Collection) invoke(method);
        Class attributeValueType = getAttributeValueType(attribute);

        if (Container.class.isAssignableFrom(attributeValueType)) {
            //noinspection unchecked
            new ContainerCollectionUpdate(collection, currentBean, prismObject, (ContainerDelta) delta, attributeValueType, ctx)
                    .execute();
        } else {
            //noinspection unchecked
            new CollectionUpdate(collection, currentBean, prismObject, delta, attributeValueType, ctx)
                    .execute();
        }
    }

    private void updateBasicOrEmbedded(Attribute<?, ?> attribute) {
        String attributeName = attribute.getName();
        PrismValue prismValue;

        Collection<ItemModifyResult<? extends PrismValue>> results = (Collection) delta.applyResults();
        ItemModifyResult<? extends PrismValue> addModifyResult = results.stream()
                .filter(r -> r.isAdded() || r.isModified())
                .findFirst()
                .orElse(null);
        ItemModifyResult<? extends PrismValue> deleteResult = results.stream()
                .filter(r -> r.isDeleted())
                .findFirst()
                .orElse(null);

        if (addModifyResult != null) {
            prismValue = addModifyResult.finalValue;
        } else if (deleteResult != null) {
            // No values to add nor to replace, only deletions. Because we narrowed the delta before, we know that this delete
            // delta is relevant - i.e. after its application, the (single) value of property will be removed.
            prismValue = null;
        } else {
            // This should not occur. The narrowing process eliminates empty deltas.
            LOGGER.warn("Empty delta {} for attribute {} of {} -- skipping it", delta, attributeName, currentBean.getClass().getName());
            return;
        }

        Object value = prismValue != null ? prismValue.getRealValue() : null;
        Class attributeValueType = getAttributeValueType(attribute);

        //noinspection unchecked
        Object valueMapped = beans.prismEntityMapper.map(value, attributeValueType);

        try {
            LOGGER.trace("Setting simple property {} to {}", attributeName, valueMapped);
            PropertyUtils.setSimpleProperty(currentBean, attributeName, valueMapped);
        } catch (Exception ex) {
            throw new SystemException("Couldn't set simple property for '" + attributeName + "'", ex);
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


    private Class getAttributeValueType(Attribute attribute) {
        Class type = attribute.getJavaType();
        if (!Collection.class.isAssignableFrom(type)) {
            return type;
        }

        Method method = (Method) attribute.getJavaMember();
        ParameterizedType parametrized = (ParameterizedType) method.getGenericReturnType();
        Type t = parametrized.getActualTypeArguments()[0];
        if (t instanceof Class) {
            return (Class) t;
        } else {
            return (Class) ((ParameterizedType) t).getRawType();
        }
    }

    private Object invoke(Method method) {
        try {
            return method.invoke(currentBean);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new SystemException("Couldn't invoke method '" + method.getName() + "' on object '" + currentBean + "'", ex);
        }
    }

    private static class ContainerMissingException extends Exception {

        public ContainerMissingException(String message) {
            super(message);
        }
    }
}
