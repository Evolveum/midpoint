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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.helpers.modify.PrismEntityMapper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
public class ObjectDeltaUpdater {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectDeltaUpdater.class);

    @Autowired
    private EntityModificationRegistry entityModificationRegistry;

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

        // todo handle nameCopy/name correctly
        // todo handle extension attributes

        RObject object = session.byId(objectToMerge.getClass()).getReference(oid);
        object.setVersion(objectToMerge.getVersion());
        object.setFullObject(objectToMerge.getFullObject());

        ManagedType mainEntityType = entityModificationRegistry.getJaxbMapping(type);

        for (ItemDelta delta : modifications) {
            ManagedType managedType = mainEntityType;
            Object bean = object;

            ItemPath path = delta.getPath();
            Iterator<ItemPathSegment> segments = path.getSegments().iterator();
            while (segments.hasNext()) {
                ItemPathSegment segment = segments.next();
                if (!(segment instanceof NameItemPathSegment)) {
                    throw new SystemException("Segment '" + segment + "' in '" + path + "' is not a name item");
                }

                NameItemPathSegment nameSegment = (NameItemPathSegment) segment;
                String nameLocalPart = nameSegment.getName().getLocalPart();

                Attribute attribute = entityModificationRegistry.findAttribute(managedType, nameLocalPart);
                if (attribute == null) {
                    attribute = entityModificationRegistry.findAttributeOverride(managedType, nameLocalPart);
                }

                if (attribute == null) {
                    // there's no table/column that needs update
                    break;
                }

                Method method = (Method) attribute.getJavaMember();

                if (segments.hasNext()) {
                    switch (attribute.getPersistentAttributeType()) {
                        case EMBEDDED:
                            managedType = entityModificationRegistry.getMapping(attribute.getJavaType());
                            bean = invoke(bean, method);
                            break;
                        case ONE_TO_MANY:
                            ParameterizedType parametrized = (ParameterizedType) method.getGenericReturnType();
                            Class clazz = (Class) parametrized.getActualTypeArguments()[0];

                            IdItemPathSegment id = (IdItemPathSegment) segments.next();
                            // todo handle types correctly
                            Collection c = (Collection) invoke(bean, method);
                            if (Container.class.isAssignableFrom(clazz)) {
                                boolean found = false;
                                for (Container o : (Collection<Container>) c) {
                                    long l = o.getId().longValue();
                                    if (l == id.getId()) {
                                        managedType = entityModificationRegistry.getMapping(clazz);
                                        bean = o;

                                        found = true;
                                        break;
                                    }
                                }

                                if (!found) {
                                    throw new RuntimeException("Couldn't find container"); // todo error handling
                                }
                            } else {
                                throw new RuntimeException("Can't go over collection"); // todo error handling
                            }
                            break;
                        default:
//                            throw new RuntimeException("Don't know what to do"); // todo error handling
                    }

                    continue;
                }

                handleAttribute(attribute, bean, method, delta);
            }
        }

        session.save(object);

        LOGGER.debug("Object saved");

        return objectToMerge;
    }

    private void handleAttribute(Attribute attribute, Object bean, Method method, ItemDelta delta) {
        switch (attribute.getPersistentAttributeType()) {
            case BASIC:
            case EMBEDDED:
                // todo qnames
                // todo how to handle add/delete/replace
                try {
                    Object realValue = delta.getAnyValue().getRealValue();
                    Class outputType = method.getReturnType();
                    if (realValue != null &&
                            prismEntityMapper.supports(realValue.getClass(), outputType)) {
                        realValue = prismEntityMapper.map(realValue, outputType);
                    }

                    PropertyUtils.setSimpleProperty(bean, attribute.getName(), realValue);
                } catch (Exception ex) {
                    throw new RuntimeException(ex); //todo error handling
                }
                break;
            case MANY_TO_MANY:
                // not used in our mappings
                throw new SystemException("Don't know how to handle @ManyToMany relationship, should not happen");
            case ONE_TO_ONE:
                // todo implement, it's assignment extension
                break;
            case MANY_TO_ONE:
                // this can't be in delta (probably)
                throw new SystemException("Don't know how to handle @ManyToOne relationship, should not happen");
            case ONE_TO_MANY:
                Collection oneToMany = (Collection) invoke(bean, method);
                handleOneToMany(oneToMany, delta);
                break;
            case ELEMENT_COLLECTION:
                Collection elementCollection = (Collection) invoke(bean, method);
                handleElementCollection(elementCollection, delta);
                break;
        }
    }

    private void handleElementCollection(Collection collection, ItemDelta delta) {
        // todo handle add/modify/delete
        // todo handle types correctly

        collection.addAll((List) delta.getValuesToAdd().stream().map(i -> ((PrismPropertyValue) i).getRealValue()).collect(Collectors.toList()));
    }

    private void handleOneToMany(Collection collection, ItemDelta delta) {
        for (Object obj : collection) {
            if (obj instanceof Container) {

            } else {
                // e.g. RObjectReference
            }
            // todo implement
        }
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
}
