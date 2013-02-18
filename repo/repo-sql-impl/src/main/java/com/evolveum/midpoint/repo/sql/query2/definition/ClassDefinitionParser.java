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
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query2.restriction.AnyPropertyRestriction;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Index;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.xml.namespace.QName;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * @author lazyman
 */
public class ClassDefinitionParser {

    private static final Trace LOGGER = TraceManager.getTrace(ClassDefinitionParser.class);

    public Definition parse(Class clazz) {
        if (clazz.isInterface() || !clazz.isAnnotationPresent(Entity.class)) {
            return null;
        }

        QName jaxbName = getJaxbName(clazz);
        QName jaxbType = getJaxbType(clazz);
        EntityDefinition entityDef = new EntityDefinition(jaxbName, jaxbType, clazz.getName(), clazz);
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

            LOGGER.info("# {}", new Object[]{methodName});

            Definition definition = createMethodDefinition(method);
            entityDef.getDefinitions().add(definition);
        }

        return entityDef;
    }

    private QName getJaxbName(Class clazz) {
        //todo implement
//        ObjectTypes objectType = ClassMapper.getObjectTypeForHQLType(clazz);
        return null;
    }

    private QName getJaxbType(Class clazz) {
        //todo implement
        return null;
    }

    private void updateEntityDefinition(EntityDefinition definition, Class clazz) {
        //todo implement
    }

    private Definition createMethodDefinition(Method method) {
        QName jaxbName = getJaxbName(method);
        QName jaxbType = getJaxbType(method);

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
        } else {
            PropertyDefinition propDef = new PropertyDefinition(jaxbName, jaxbType, jpaName, jpaType);
            updatePropertyDefinition(propDef, method);
            definition = propDef;
        }

        return definition;
    }

    private void updateCollectionDefinition(CollectionDefinition definition, Method method) {
        //todo implement
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

    private QName getJaxbType(Method method) {
        //todo implement, maybe unnecessary, will be probably unused
        return null;
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
}
