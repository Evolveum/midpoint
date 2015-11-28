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
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.definition.Any;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.QueryEntity;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualAny;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualCollection;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * @author lazyman
 * @author mederly
 */
public class ClassDefinitionParser {

    private static final Trace LOGGER = TraceManager.getTrace(ClassDefinitionParser.class);

    public JpaRootEntityDefinition parseRootClass(Class jpaClass) {
        JpaEntityContentDefinition content = parseClass(jpaClass);
        JpaRootEntityDefinition rootEntityDefinition = new JpaRootEntityDefinition(jpaClass, content);
        return rootEntityDefinition;
    }

    private JpaEntityContentDefinition parseClass(Class jpaClass) {

        JpaEntityContentDefinition entity = new JpaEntityContentDefinition();
        LOGGER.trace("### {}", entity);

        addVirtualDefinitions(jpaClass, entity);
        Method[] methods = jpaClass.getMethods();

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

            LOGGER.trace("# {}", method);
            JpaItemDefinition jpaItemDefinition = parseMethod(method);
            entity.addDefinition(jpaItemDefinition);
        }
        return entity;
    }

    private JpaItemDefinition parseMethod(Method method) {
        CollectionSpecification collectionSpecification;    // non-null if return type is Set<X>, null if it's X
        Type returnedContentType;                           // X in return type, which is either X or Set<X>
        if (Set.class.isAssignableFrom(method.getReturnType())) {
            // e.g. Set<RObject> or Set<String> or Set<REmbeddedReference<RFocus>>
            Type returnType = method.getGenericReturnType();
            if (!(returnType instanceof ParameterizedType)) {
                throw new IllegalStateException("Method " + method + " returns a non-parameterized collection");
            }
            returnedContentType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
            collectionSpecification = new CollectionSpecification();
        } else {
            returnedContentType = method.getReturnType();
            collectionSpecification = null;
        }

        QName jaxbName = getJaxbName(method);
        String jpaName = getJpaName(method);
        Class jpaClass = getClass(returnedContentType);

        // sanity check
        if (Set.class.isAssignableFrom(jpaClass)) {
            throw new IllegalStateException("Collection within collection is not supported: method=" + method);
        }

        JpaItemDefinition definition;
        Any any = (Any) jpaClass.getAnnotation(Any.class);
        if (any != null) {
            jaxbName = new QName(any.jaxbNameNamespace(), any.jaxbNameLocalPart());
            definition = new JpaAnyDefinition(jaxbName, jpaName, jpaClass);
        } else if (ObjectReference.class.isAssignableFrom(jpaClass)) {
            boolean embedded = method.isAnnotationPresent(Embedded.class);
            // computing referenced entity type from returned content type like RObjectReference<RFocus> or REmbeddedReference<RRole>
            Class referencedJpaClass;
            if (returnedContentType instanceof ParameterizedType) {
                referencedJpaClass = getClass(((ParameterizedType) returnedContentType).getActualTypeArguments()[0]);
            } else {
                referencedJpaClass = RObject.class;
            }
            definition = new JpaReferenceDefinition(jaxbName, jpaName, collectionSpecification, jpaClass, referencedJpaClass, embedded);
        } else if (isEntity(jpaClass)) {
            JpaEntityContentDefinition content = parseClass(jpaClass);
            boolean embedded = method.isAnnotationPresent(Embedded.class) || jpaClass.isAnnotationPresent(Embeddable.class);
            definition = new JpaEntityItemDefinition(jaxbName, jpaName, collectionSpecification, jpaClass, content, embedded);
        } else {
            boolean lob = method.isAnnotationPresent(Lob.class);
            boolean enumerated = method.isAnnotationPresent(Enumerated.class);
            //todo implement also lookup for @Table indexes
            boolean indexed = method.isAnnotationPresent(Index.class);
            definition = new JpaPropertyDefinition(jaxbName, jpaName, collectionSpecification, jpaClass, lob, enumerated, indexed);
        }
        return definition;
    }

    private Class<?> getClass(Type type) {
        if (type instanceof Class) {
            return ((Class) type);
        } else if (type instanceof ParameterizedType) {
            return getClass(((ParameterizedType) type).getRawType());
        } else {
            throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    private void addVirtualDefinitions(Class jpaClass, JpaEntityContentDefinition entityDef) {
        addVirtualDefinitionsForClass(jpaClass, entityDef);

        while ((jpaClass = jpaClass.getSuperclass()) != null) {
            addVirtualDefinitionsForClass(jpaClass, entityDef);
        }
    }

    private void addVirtualDefinitionsForClass(Class jpaClass, JpaEntityContentDefinition entityDef) {
        if (!jpaClass.isAnnotationPresent(QueryEntity.class)) {
            return;
        }

        QueryEntity qEntity = (QueryEntity) jpaClass.getAnnotation(QueryEntity.class);

        for (VirtualAny any : qEntity.anyElements()) {
            VirtualAnyDefinition def = new VirtualAnyDefinition(
                    new QName(any.jaxbNameNamespace(), any.jaxbNameLocalPart()),
                    any.ownerType());
            entityDef.addDefinition(def);
        }

        for (VirtualCollection collection : qEntity.collections()) {
            // only collections of entities expected at this moment
            VirtualCollectionSpecification colSpec = new VirtualCollectionSpecification(collection.additionalParams());
            QName jaxbName = createQName(collection.jaxbName());
            String jpaName = collection.jpaName();
            JpaEntityContentDefinition content = parseClass(collection.jpaType());
            JpaEntityItemDefinition definition = new JpaEntityItemDefinition(jaxbName, jpaName, colSpec, jpaClass, content, false);
            entityDef.addDefinition(definition);
        }
    }

    private QName createQName(JaxbName name) {
        return new QName(name.namespace(), name.localPart());
    }

    private boolean isEntity(Class type) {
        if (RPolyString.class.isAssignableFrom(type)) {
            //it's hibernate entity but from prism point of view it's property
            return false;
        }
        return type.getAnnotation(Entity.class) != null || type.getAnnotation(Embeddable.class) != null;
    }

    private QName getJaxbName(Method method) {
        if (method.isAnnotationPresent(JaxbName.class)) {
            JaxbName jaxbName = method.getAnnotation(JaxbName.class);
            return new QName(jaxbName.namespace(), jaxbName.localPart());
        } else {
            return new QName(SchemaConstantsGenerated.NS_COMMON, getPropertyName(method.getName()));
        }
    }

//    private Class getJaxbClass(Class clazz) {
//        if (RObject.class.isAssignableFrom(clazz)) {
//            ObjectTypes objectType = ClassMapper.getObjectTypeForHQLType(clazz);
//            return objectType.getClassDefinition();
//        }
//
//        if (clazz.getAnnotation(JaxbType.class) != null) {
//            JaxbType type = (JaxbType) clazz.getAnnotation(JaxbType.class);
//            return type.type();
//        }
//
//        return clazz;
//    }

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
