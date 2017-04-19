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

import com.evolveum.midpoint.prism.path.IdentifierPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ParentPathSegment;
import com.evolveum.midpoint.repo.sql.data.Marker;
import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.definition.*;
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 * @author mederly
 */
public class ClassDefinitionParser {

    private static final Trace LOGGER = TraceManager.getTrace(ClassDefinitionParser.class);

    public JpaEntityDefinition parseRootClass(Class jpaClass) {
        return parseClass(jpaClass);
    }

    private JpaEntityDefinition parseClass(Class jpaClass) {

        Class jaxbClass = getJaxbClassForEntity(jpaClass);
        JpaEntityDefinition entity = new JpaEntityDefinition(jpaClass, jaxbClass);
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

            JpaLinkDefinition linkDefinition;
            OwnerGetter ownerGetter = method.getAnnotation(OwnerGetter.class);
            if (ownerGetter != null) {
                String jpaName = getJpaName(method);
                JpaDataNodeDefinition nodeDefinition = new JpaEntityPointerDefinition(ownerGetter.ownerClass());
                // Owner is considered as not embedded, so we generate left outer join to access it
                // (instead of implicit inner join that would be used if we would do x.owner.y = '...')
                linkDefinition = new JpaLinkDefinition(new ParentPathSegment(), jpaName, null, false, nodeDefinition);
            } else {
                linkDefinition = parseMethod(method);
            }
            entity.addDefinition(linkDefinition);
        }
        return entity;
    }

    private JpaLinkDefinition parseMethod(Method method) {
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

        ItemPath itemPath = getJaxbName(method);
        String jpaName = getJpaName(method);
        Class jpaClass = getClass(returnedContentType);

        // sanity check
        if (Set.class.isAssignableFrom(jpaClass)) {
            throw new IllegalStateException("Collection within collection is not supported: method=" + method);
        }

        JpaLinkDefinition<? extends JpaDataNodeDefinition> linkDefinition;
        Any any = method.getAnnotation(Any.class);
        if (any != null) {
            JpaAnyContainerDefinition targetDefinition = new JpaAnyContainerDefinition(jpaClass);
            QName jaxbNameForAny = new QName(any.jaxbNameNamespace(), any.jaxbNameLocalPart());
            linkDefinition = new JpaLinkDefinition<>(jaxbNameForAny, jpaName, collectionSpecification, false, targetDefinition);
        } else if (ObjectReference.class.isAssignableFrom(jpaClass)) {
            boolean embedded = method.isAnnotationPresent(Embedded.class);
            // computing referenced entity type from returned content type like RObjectReference<RFocus> or REmbeddedReference<RRole>
            Class referencedJpaClass;
            if (returnedContentType instanceof ParameterizedType) {
                referencedJpaClass = getClass(((ParameterizedType) returnedContentType).getActualTypeArguments()[0]);
            } else {
                referencedJpaClass = RObject.class;
            }
            JpaReferenceDefinition targetDefinition = new JpaReferenceDefinition(jpaClass, referencedJpaClass);
            linkDefinition = new JpaLinkDefinition<>(itemPath, jpaName, collectionSpecification, embedded, targetDefinition);
        } else if (isEntity(jpaClass)) {
            JpaEntityDefinition content = parseClass(jpaClass);
            boolean embedded = method.isAnnotationPresent(Embedded.class) || jpaClass.isAnnotationPresent(Embeddable.class);
            linkDefinition = new JpaLinkDefinition<JpaDataNodeDefinition>(itemPath, jpaName, collectionSpecification, embedded,
                    content);
        } else {
            boolean lob = method.isAnnotationPresent(Lob.class);
            boolean enumerated = method.isAnnotationPresent(Enumerated.class);
            //todo implement also lookup for @Table indexes
            boolean indexed = method.isAnnotationPresent(Index.class);
            boolean count = method.isAnnotationPresent(Count.class);
            Class jaxbClass = getJaxbClass(method, jpaClass);

            if (method.isAnnotationPresent(IdQueryProperty.class)) {
                if (collectionSpecification != null) {
                    throw new IllegalStateException("ID property is not allowed to be multivalued; for method " + method);
                }
                itemPath = new ItemPath(new IdentifierPathSegment());
            } else if (method.isAnnotationPresent(OwnerIdGetter.class)) {
                if (collectionSpecification != null) {
                    throw new IllegalStateException("Owner ID property is not allowed to be multivalued; for method " + method);
                }
                itemPath = new ItemPath(new ParentPathSegment(), new IdentifierPathSegment());
            }

            JpaPropertyDefinition propertyDefinition = new JpaPropertyDefinition(jpaClass, jaxbClass, lob, enumerated, indexed, count);
            // Note that properties are considered to be embedded
            linkDefinition = new JpaLinkDefinition<JpaDataNodeDefinition>(itemPath, jpaName, collectionSpecification, true,
                    propertyDefinition);
        }
        return linkDefinition;
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

    private void addVirtualDefinitions(Class jpaClass, JpaEntityDefinition entityDef) {
        addVirtualDefinitionsForClass(jpaClass, entityDef);

        while ((jpaClass = jpaClass.getSuperclass()) != null) {
            addVirtualDefinitionsForClass(jpaClass, entityDef);
        }
    }

    private void addVirtualDefinitionsForClass(Class jpaClass, JpaEntityDefinition entityDef) {
        if (!jpaClass.isAnnotationPresent(QueryEntity.class)) {
            return;
        }

        QueryEntity qEntity = (QueryEntity) jpaClass.getAnnotation(QueryEntity.class);

        for (VirtualAny any : qEntity.anyElements()) {
            QName jaxbName = new QName(any.jaxbNameNamespace(), any.jaxbNameLocalPart());
            VirtualAnyContainerDefinition def = new VirtualAnyContainerDefinition(any.ownerType());
            JpaLinkDefinition linkDefinition = new JpaLinkDefinition(jaxbName, null, null, false, def);
            entityDef.addDefinition(linkDefinition);
        }

        for (VirtualCollection collection : qEntity.collections()) {
            // only collections of entities expected at this moment
            VirtualCollectionSpecification colSpec = new VirtualCollectionSpecification(collection.additionalParams());
            QName jaxbName = createQName(collection.jaxbName());
            String jpaName = collection.jpaName();
            JpaEntityDefinition content = parseClass(collection.collectionType());
            JpaLinkDefinition linkDefinition = new JpaLinkDefinition(jaxbName, jpaName, colSpec, false, content);
            entityDef.addDefinition(linkDefinition);
        }

        for (VirtualEntity entity : qEntity.entities()) {
            QName jaxbName = createQName(entity.jaxbName());
            String jpaName = normalizeJpaName(entity.jpaName());
            if (jpaName != null) {
                throw new IllegalStateException("Only self-pointing virtual entities are supported for now; this one is not: " + jaxbName + " in " + entityDef);
            }
            JpaDataNodeDefinition target = new JpaEntityPointerDefinition(entityDef);         // pointer to avoid loops
            JpaLinkDefinition linkDefinition = new JpaLinkDefinition(jaxbName, jpaName, null, false, target);
            entityDef.addDefinition(linkDefinition);
        }
    }

    private QName createQName(JaxbName name) {
        return new QName(name.namespace(), name.localPart());
    }

    private String normalizeJpaName(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;        // "" -> null
        } else {
            return name;
        }
    }

    private boolean isEntity(Class type) {
        if (RPolyString.class.isAssignableFrom(type)) {
            //it's hibernate entity but from prism point of view it's property
            return false;
        }
        return type.getAnnotation(Entity.class) != null || type.getAnnotation(Embeddable.class) != null;
    }

    private ItemPath getJaxbName(Method method) {
        if (method.isAnnotationPresent(JaxbName.class)) {
            JaxbName jaxbName = method.getAnnotation(JaxbName.class);
            return new ItemPath(new QName(jaxbName.namespace(), jaxbName.localPart()));
        } else if (method.isAnnotationPresent(JaxbPath.class)) {
            JaxbPath jaxbPath = method.getAnnotation(JaxbPath.class);
            List<QName> names = new ArrayList<>(jaxbPath.itemPath().length);
            for (JaxbName jaxbName : jaxbPath.itemPath()) {
                names.add(new QName(jaxbName.namespace(), jaxbName.localPart()));
            }
            return new ItemPath(names.toArray(new QName[0]));
        } else {
            String propertyName = getPropertyName(method.getName());
            if (method.isAnnotationPresent(Count.class)) {
                propertyName = StringUtils.removeEnd(propertyName, "Count");
            }
            return new ItemPath(new QName(SchemaConstantsGenerated.NS_COMMON, propertyName));
        }
    }

    // second parameter is just to optimize
    private Class getJaxbClass(Method method, Class returnedClass) {
        JaxbType annotation = (JaxbType) method.getAnnotation(JaxbType.class);
        if (annotation != null) {
            return annotation.type();
        }
        Class classFromEntity = getJaxbClassForEntity(returnedClass);
        if (classFromEntity != null) {
            return classFromEntity;
        }
        Package returnedClassPkg = returnedClass.getPackage();
        Package dataObjectsPkg = Marker.class.getPackage();
        if (returnedClassPkg != null && returnedClassPkg.getName().startsWith(dataObjectsPkg.getName())) {
            return null;
        } else {
            return returnedClass;       // probably the JAXB value
        }
    }

    private Class getJaxbClassForEntity(Class clazz) {
        if (RObject.class.isAssignableFrom(clazz)) {
            ObjectTypes objectType = ClassMapper.getObjectTypeForHQLType(clazz);
            return objectType.getClassDefinition();
        }
        JaxbType annotation = (JaxbType) clazz.getAnnotation(JaxbType.class);
        if (annotation != null) {
            return annotation.type();
        }
        return null;
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
