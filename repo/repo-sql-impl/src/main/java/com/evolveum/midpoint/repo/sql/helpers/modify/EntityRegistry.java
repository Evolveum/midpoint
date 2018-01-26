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

package com.evolveum.midpoint.repo.sql.helpers.modify;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbPath;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.Metamodel;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Viliam Repan (lazyman)
 */
@Service
public class EntityRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(EntityRegistry.class);

    @Autowired
    private SessionFactory sessionFactory;

    private Metamodel metamodel;

    private Map<Class, ManagedType> jaxbMappings = new HashMap<>();

    private Map<ManagedType, Map<String, Attribute>> attributeNameOverrides = new HashMap<>();

    @PostConstruct
    public void init() {
        LOGGER.debug("Starting initialization");

        metamodel = sessionFactory.getMetamodel();

        for (EntityType entity : metamodel.getEntities()) {
            Class javaType = entity.getJavaType();
            Ignore ignore = (Ignore) javaType.getAnnotation(Ignore.class);
            if (ignore != null) {
                continue;
            }

            Class jaxb;
            if (RObject.class.isAssignableFrom(javaType)) {
                jaxb = RObjectType.getType(javaType).getJaxbClass();
            } else {
                JaxbType jaxbType = (JaxbType) javaType.getAnnotation(JaxbType.class);
                if (jaxbType == null) {
                    throw new IllegalStateException("Unknown jaxb type for " + javaType.getName());
                }
                jaxb = jaxbType.type();
            }

            jaxbMappings.put(jaxb, entity);

            // create override map
            Map<String, Attribute> overrides = new HashMap<>();

            for (Attribute attribute : (Set<Attribute>) entity.getAttributes()) {
                Class jType = attribute.getJavaType();
                JaxbPath path = (JaxbPath) jType.getAnnotation(JaxbPath.class);
                if (path == null) {
                    path = ((Method) attribute.getJavaMember()).getAnnotation(JaxbPath.class);
                }

                if (path == null) {
                    continue;
                }

                for (JaxbName name : path.itemPath()) {
                    overrides.put(name.localPart(), attribute);
                }
            }

            if (!overrides.isEmpty()) {
                attributeNameOverrides.put(entity, overrides);
            }
        }

        LOGGER.debug("Initialization finished");
    }

    public ManagedType getJaxbMapping(Class jaxbType) {
        return jaxbMappings.get(jaxbType);
    }

    public ManagedType getMapping(Class entityType) {
        return metamodel.managedType(entityType);
    }

    public Attribute findAttribute(ManagedType type, String name) {
        try {
            return type.getAttribute(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public Attribute findAttributeOverride(ManagedType type, String nameOverride) {
        Map<String, Attribute> overrides = attributeNameOverrides.get(type);
        if (overrides == null) {
            return null;
        }

        return overrides.get(nameOverride);
    }
}
