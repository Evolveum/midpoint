/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query2.definition;

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

    public Definition parse(Class clazz) {
        if (clazz.isInterface()) {// || !clazz.isAnnotationPresent(Entity.class)) {
            return null;
        }

        //we doesn't need to check java.lang or other packages than our sql repo
        if (!clazz.getPackage().getName().startsWith("com.evolveum.midpoint.repo.sql.data")) {
            return null;
        }

        QName jaxbName = getJaxbName(clazz);
        Class jaxbType = getJaxbType(clazz);
        EntityDefinition entityDef = new EntityDefinition(jaxbName, jaxbType, clazz.getSimpleName(), clazz);
        updateEntityDefinition(entityDef, clazz);

        Method[] methods = clazz.getMethods();
        LOGGER.info("### {}", new Object[]{clazz.getName()});

        for (Method method : methods) {
            String methodName = method.getName();
            if (Modifier.isStatic(method.getModifiers()) || "getClass".equals(methodName) ||
                    !methodName.startsWith("is") && !methodName.startsWith("get")) {
                //it's not getter for property
                continue;
            }

            LOGGER.trace("# {}", new Object[]{methodName});

            Definition definition = createDefinitionFromMethod(method);
            entityDef.getDefinitions().add(definition);
        }

        Collections.sort(entityDef.getDefinitions(), new DefinitionComparator());

        return entityDef;
    }

    private QName getJaxbName(Class clazz) {
        if (RObject.class.isAssignableFrom(clazz)) {
            ObjectTypes objectType = ClassMapper.getObjectTypeForHQLType(clazz);
            return objectType.getQName();
        }

        //todo implement
        return null;
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

    private void updateEntityDefinition(EntityDefinition definition, Class clazz) {
        //todo implement
    }

    private Definition createDefinitionFromMethod(Method method) {
        QName jaxbName = getJaxbName(method);
        Class jaxbType = getJaxbType(method);

        String jpaName = getJpaName(method.getName());
        Class jpaType = method.getReturnType();

        Definition definition;
        if (ObjectReference.class.isAssignableFrom(jpaType)) {
            ReferenceDefinition refDef = new ReferenceDefinition(jaxbName, jaxbType, jpaName, jpaType);
            updateReferenceDefinition(refDef, method);
            definition = refDef;
        } else if (RAnyContainer.class.isAssignableFrom(jpaType)) {
            definition = new AnyDefinition(jaxbName, jaxbType, jpaName, jpaType);
        } else if (Set.class.isAssignableFrom(jpaType)) {
            CollectionDefinition collDef = new CollectionDefinition(jaxbName, jaxbType, jpaName, jpaType);
            updateCollectionDefinition(collDef, method);
            definition = collDef;
        } else if (isReturningEntity(method)) {
            EntityDefinition entityDef = new EntityDefinition(jaxbName, jaxbType, jpaName, jpaType);
            //todo implement recursion
//            updateEntityDefinition(entityDef);
            definition = entityDef;
        } else {
            PropertyDefinition propDef = new PropertyDefinition(jaxbName, jaxbType, jpaName, jpaType);
            updatePropertyDefinition(propDef, method);
            definition = propDef;
        }

        return definition;
    }

    private boolean isReturningEntity(Method method) {
        Class type = method.getReturnType();
        if (RPolyString.class.isAssignableFrom(type)) {
            //it's hibernate entity but from prism point of view it's property
            return false;
        }

        return type.getAnnotation(Entity.class) != null || type.getAnnotation(Embeddable.class) != null;
    }

    private void updateCollectionDefinition(CollectionDefinition definition, Method method) {
        ParameterizedType type = (ParameterizedType) method.getGenericReturnType();
        Class clazz = (Class) type.getActualTypeArguments()[0];

        ClassDefinitionParser parser = new ClassDefinitionParser();
        Definition collDef = parser.parse(clazz);

        definition.setDefinition(collDef);
    }

    private void updateReferenceDefinition(ReferenceDefinition definition, Method method) {
        if (method.isAnnotationPresent(Embedded.class)) {
            definition.setEmbedded(true);
        }
    }

    private void updatePropertyDefinition(PropertyDefinition definition, Method method) {
        if (method.isAnnotationPresent(Lob.class)) {
            definition.setLob(true);
        }

        if (method.isAnnotationPresent(Enumerated.class)) {
            definition.setEnumerated(true);
        }

        //todo implement also lookup for @Table indexes
        if (method.isAnnotationPresent(Index.class)) {
            definition.setIndexed(true);
        }
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

    private String getJpaName(String methodName) {
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
