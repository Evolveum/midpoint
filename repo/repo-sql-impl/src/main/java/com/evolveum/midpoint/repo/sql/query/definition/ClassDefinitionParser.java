/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RAnyContainer;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import javax.xml.namespace.QName;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.Comparator;
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

        EntityDefinition entityDefinition = new EntityDefinition(jaxbName, jaxbType, type.getSimpleName(), type);
        updateEntityDefinition(entityDefinition);

        return entityDefinition;
    }

    private void updateEntityDefinition(EntityDefinition entity) {
        Method[] methods = entity.getJpaType().getMethods();
        LOGGER.trace("### {}", new Object[]{entity.getJpaName()});

        entity.setEmbedded(entity.getJpaType().getAnnotation(Embeddable.class) != null);

        for (Method method : methods) {
            String methodName = method.getName();
            if (Modifier.isStatic(method.getModifiers()) || "getClass".equals(methodName) ||
                    !methodName.startsWith("is") && !methodName.startsWith("get")) {
                //it's not getter for property
                continue;
            }

            LOGGER.trace("# {}", new Object[]{methodName});

            QName jaxbName = getJaxbName(method);
            Class jaxbType = getJaxbType(method);
            String jpaName = getJpaName(method);
            Definition definition = createDefinition(jaxbName, jaxbType, jpaName, method);
            entity.getDefinitions().add(definition);
        }

        Collections.sort(entity.getDefinitions(), new DefinitionComparator());
    }

    private Definition createDefinition(QName jaxbName, Class jaxbType, String jpaName, AnnotatedElement object) {
        Class jpaType = (object instanceof Class) ? (Class) object : ((Method) object).getReturnType();

        Definition definition;
        if (ObjectReference.class.isAssignableFrom(jpaType)) {
            ReferenceDefinition refDef = new ReferenceDefinition(jaxbName, jaxbType, jpaName, jpaType);
            definition = updateReferenceDefinition(refDef, object);
        } else if (RAnyContainer.class.isAssignableFrom(jpaType)) {
            definition = new AnyDefinition(jaxbName, jaxbType, jpaName, jpaType);
        } else if (Set.class.isAssignableFrom(jpaType)) {
            CollectionDefinition collDef = new CollectionDefinition(jaxbName, jaxbType, jpaName, jpaType);
            updateCollectionDefinition(collDef, object);
            definition = collDef;
        } else if (isEntity(object)) {
            EntityDefinition entityDef = new EntityDefinition(jaxbName, jaxbType, jpaName, jpaType);
            if ("com.evolveum.midpoint.repo.sql.data.common.embedded".equals(jpaType.getPackage().getName())) {
                updateEntityDefinition(entityDef);
            }
            definition = entityDef;
        } else {
            PropertyDefinition propDef = new PropertyDefinition(jaxbName, jaxbType, jpaName, jpaType);
            definition = updatePropertyDefinition(propDef, object);
        }

        return definition;
    }

    private CollectionDefinition updateCollectionDefinition(CollectionDefinition definition, AnnotatedElement object) {
        Definition collDef;
        if (object instanceof Method) {
            Method method = (Method) object;
            ParameterizedType type = (ParameterizedType) method.getGenericReturnType();
            Class clazz = (Class) type.getActualTypeArguments()[0];

            QName jaxbName = getJaxbName(method);
            Class jaxbType = getJaxbType(clazz);
            String jpaName = getJpaName(method);
            collDef = createDefinition(jaxbName, jaxbType, jpaName, clazz);

            if (collDef instanceof EntityDefinition) {
                updateEntityDefinition((EntityDefinition) collDef);
            }
        } else {
            // todo what to do here, when we're looking at collection in collection (if there is something
            // like collection in entity which is in collection)
            throw new UnsupportedOperationException("Not implemented yet.");
        }

        definition.setDefinition(collDef);

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

    private static class DefinitionComparator implements Comparator<Definition> {

        @Override
        public int compare(Definition o1, Definition o2) {
            if (o1.getClass().equals(o2.getClass())) {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getJaxbName().getLocalPart(),
                        o2.getJaxbName().getLocalPart());
            }

            return getType(o1) - getType(o2);
        }

        private int getType(Definition def) {
            if (def == null) {
                return 0;
            }

            if (def instanceof PropertyDefinition) {
                return 1;
            } else if (def instanceof ReferenceDefinition) {
                return 2;
            } else if (def instanceof CollectionDefinition) {
                return 3;
            } else if (def instanceof AnyDefinition) {
                return 4;
            } else if (def instanceof EntityDefinition) {
                return 5;
            }

            return 0;
        }
    }
}
