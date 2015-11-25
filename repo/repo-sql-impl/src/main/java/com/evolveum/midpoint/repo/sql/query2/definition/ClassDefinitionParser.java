/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.repo.sql.query2.definition;

import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.RAssignmentExtension;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.query.definition.QueryEntity;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualAny;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualCollection;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Index;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.Transient;
import javax.xml.namespace.QName;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Set;

/**
 * @author lazyman
 */
public class ClassDefinitionParser {

    private static final Trace LOGGER = TraceManager.getTrace(ClassDefinitionParser.class);

    public <T extends RObject> EntityDefinition parseObjectTypeClass(Class<T> type) {
        ObjectTypes objectType = ClassMapper.getObjectTypeForHQLType(type);
        QName jaxbName = objectType.getQName();
        Class jaxbType = objectType.getClassDefinition();
        return parseObjectTypeClass(jaxbName, jaxbType, type);
    }

    public EntityDefinition parseObjectTypeClass(QName jaxbName, Class jaxbType, Class jpaType) {
        EntityDefinition entityDefinition = new EntityDefinition(jaxbName, jaxbType, jpaType.getSimpleName(), jpaType, null);
        updateEntityDefinition(entityDefinition);
        return entityDefinition;
    }

    private void updateEntityDefinition(EntityDefinition entity) {
        LOGGER.trace("### {}", entity);

        addVirtualDefinitions(entity);
        Method[] methods = entity.getJpaType().getMethods();

        entity.setEmbedded(entity.getJpaType().getAnnotation(Embeddable.class) != null);

        for (Method method : methods) {
            String methodName = method.getName();
            if (Modifier.isStatic(method.getModifiers()) || "getClass".equals(methodName) ||
                    (!methodName.startsWith("is") && !methodName.startsWith("get")) ||
                    method.getAnnotation(NotQueryable.class) != null) {
                //it's not getter for queryable property
                continue;
            }

            if (method.isAnnotationPresent(Transient.class)) {
                continue;
            }

            LOGGER.trace("# {}", methodName);

            QName jaxbName = getJaxbName(method);
            Class jaxbType = getJaxbType(method);
            String jpaName = getJpaName(method);
            Definition definition = createDefinition(jaxbName, jaxbType, jpaName, method, null);
            entity.addDefinition(definition);
        }
    }

    private void addVirtualDefinitions(EntityDefinition entityDef) {
        Class jpaType = entityDef.getJpaType();
        addVirtualDefinitionsForClass(entityDef, jpaType);

        while ((jpaType = jpaType.getSuperclass()) != null) {
            addVirtualDefinitionsForClass(entityDef, jpaType);
        }
    }

    private void addVirtualDefinitionsForClass(EntityDefinition entityDef, Class jpaType) {
        if (!jpaType.isAnnotationPresent(QueryEntity.class)) {
            return;
        }

        QueryEntity qEntity = (QueryEntity) jpaType.getAnnotation(QueryEntity.class);
//        for (VirtualProperty property : qEntity.properties()) {
//            QName jaxbName = createQName(property.jaxbName());
//            VirtualPropertyDefinition def = new VirtualPropertyDefinition(jaxbName, property.jaxbType(),
//                    property.jpaName(), property.jpaType());
//            def.setAdditionalParams(property.additionalParams());
//            entityDef.addDefinition(def);
//        }
//
//        for (VirtualReference reference : qEntity.references()) {
//
//        }

        for (VirtualAny any : qEntity.anyElements()) {
            VirtualAnyDefinition def = new VirtualAnyDefinition(
                    new QName(any.jaxbNameNamespace(), any.jaxbNameLocalPart()),
                    any.ownerType());
            entityDef.addDefinition(def);
        }

        for (VirtualCollection collection : qEntity.collections()) {
            VirtualCollectionSpecification colSpec = new VirtualCollectionSpecification();
            colSpec.setAdditionalParams(collection.additionalParams());

            QName jaxbName = createQName(collection.jaxbName());
            Definition definition = createDefinition(jaxbName, collection.jaxbType(), collection.jpaName(), collection.collectionType(), colSpec);
            entityDef.addDefinition(definition);
        }
    }

    private QName createQName(JaxbName name) {
        return new QName(name.namespace(), name.localPart());
    }

    private Definition createDefinition(QName jaxbName, Class jaxbType, String jpaName, AnnotatedElement object, CollectionSpecification collectionSpecification) {
        Class jpaType = (object instanceof Class) ? (Class) object : ((Method) object).getReturnType();

        if (Set.class.isAssignableFrom(jpaType)) {
            if (collectionSpecification != null) {
                throw new IllegalStateException("Collection within collection is not supported: jaxbName=" +
                        jaxbName + ", jaxbType=" + jaxbType + ", jpaName=" + jpaName + ", object=" + object);
            }
            collectionSpecification = new CollectionSpecification();
            if (object instanceof Method) {
                Method method = (Method) object;
                ParameterizedType type = (ParameterizedType) method.getGenericReturnType();
                Class clazz = (Class) type.getActualTypeArguments()[0];
                QName realJaxbName = getJaxbName(method);
                Class realJaxbType = getJaxbType(clazz);
                String realJpaName = getJpaName(method);
                return createDefinition(realJaxbName, realJaxbType, realJpaName, clazz, collectionSpecification);
            } else {
                Class clazz = (Class) object;
                Class realJaxbType = getJaxbType(clazz);
                return createDefinition(jaxbName, realJaxbType, jpaName, clazz, collectionSpecification);
            }
        }

        Definition definition;
        if (ObjectReference.class.isAssignableFrom(jpaType)) {
            ReferenceDefinition refDef = new ReferenceDefinition(jaxbName, jaxbType, jpaName, jpaType, collectionSpecification);
            definition = updateReferenceDefinition(refDef, object);
        } else if (RAssignmentExtension.class.isAssignableFrom(jpaType)) {
            definition = new AnyDefinition(jaxbName, jaxbType, jpaName, jpaType);
        } else if (isEntity(object)) {
            EntityDefinition entityDef = new EntityDefinition(jaxbName, jaxbType, jpaName, jpaType, collectionSpecification);
            updateEntityDefinition(entityDef);
            definition = entityDef;
        } else {
            PropertyDefinition propDef = new PropertyDefinition(jaxbName, jaxbType, jpaName, jpaType, collectionSpecification);
            definition = updatePropertyDefinition(propDef, object);
        }

        return definition;
    }

    private boolean isEntity(AnnotatedElement object) {
        Class type = (object instanceof Class) ? (Class) object : ((Method) object).getReturnType();
        if (RPolyString.class.isAssignableFrom(type)) {
            //it's hibernate entity but from prism point of view it's property
            return false;
        }

        return type.getAnnotation(Entity.class) != null || type.getAnnotation(Embeddable.class) != null;
    }

    private PropertyDefinition updatePropertyDefinition(PropertyDefinition definition, AnnotatedElement object) {
        if (object.isAnnotationPresent(Lob.class)) {
            definition.setLob(true);
        }

        if (object.isAnnotationPresent(Enumerated.class)) {
            definition.setEnumerated(true);
        }

        //todo implement also lookup for @Table indexes
        if (object.isAnnotationPresent(Index.class)) {
            definition.setIndexed(true);
        }

        return definition;
    }

    private ReferenceDefinition updateReferenceDefinition(ReferenceDefinition definition, AnnotatedElement object) {
        if (object.isAnnotationPresent(Embedded.class)) {
            definition.setEmbedded(true);
        }

        return definition;
    }

    private QName getJaxbName(Method method) {
        QName name = new QName(SchemaConstantsGenerated.NS_COMMON, getPropertyName(method.getName()));
        if (method.isAnnotationPresent(JaxbName.class)) {
            JaxbName jaxbName = method.getAnnotation(JaxbName.class);
            name = new QName(jaxbName.namespace(), jaxbName.localPart());
        }

        return name;
    }

    private Class getJaxbType(Method method) {
        return getJaxbType(method.getReturnType());
    }

    private Class getJaxbType(Class clazz) {
        if (RObject.class.isAssignableFrom(clazz)) {
            ObjectTypes objectType = ClassMapper.getObjectTypeForHQLType(clazz);
            return objectType.getClassDefinition();
        }

        if (clazz.getAnnotation(JaxbType.class) != null) {
            JaxbType type = (JaxbType) clazz.getAnnotation(JaxbType.class);
            return type.type();
        }

        return clazz;
    }

    private String getJpaName(Method method) {
        String methodName = method.getName();
        return getPropertyName(methodName);
    }

    private String getPropertyName(String methodName) {
        int startIndex = 3; //method name starts with "get"
        if (methodName.startsWith("is")) {
            startIndex = 2;
        }

        char first = Character.toLowerCase(methodName.charAt(startIndex));
        return Character.toString(first) + StringUtils.substring(methodName, startIndex + 1, methodName.length());
    }
}
