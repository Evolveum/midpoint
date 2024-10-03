/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.modify;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbPath;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Viliam Repan (lazyman)
 */
@Service
public class EntityRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(EntityRegistry.class);

    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private PrismContext prismContext;

    private Metamodel metamodel;

    // we know that some <?> represent the same type in one entry, but there is no way to capture that information

    private Map<Class<?>, ManagedType<?>> jaxbMappings = new HashMap<>();

    private Map<ManagedType<?>, Map<String, Attribute<?, ?>>> attributeNameOverrides = new HashMap<>();

    private Map<ManagedType<?>, Map<UniformItemPath, Attribute<?, ?>>> attributeNamePathOverrides = new HashMap<>();

    @PostConstruct
    public void init() {
        LOGGER.debug("Starting initialization");

        metamodel = entityManagerFactory.getMetamodel();

        for (EntityType<?> entity : metamodel.getEntities()) {
            Class<?> javaType = entity.getJavaType();
            Ignore ignore = javaType.getAnnotation(Ignore.class);
            if (ignore != null) {
                continue;
            }

            Class<?> jaxb;
            if (RObject.class.isAssignableFrom(javaType)) {
                //noinspection unchecked,rawtypes
                jaxb = RObjectType.getType((Class<? extends RObject>) javaType).getJaxbClass();
            } else {
                JaxbType jaxbType = javaType.getAnnotation(JaxbType.class);
                if (jaxbType == null) {
                    throw new IllegalStateException("Unknown jaxb type for " + javaType.getName());
                }
                jaxb = jaxbType.type();
            }

            jaxbMappings.put(jaxb, entity);

            // create override map
            Map<String, Attribute<?, ?>> overrides = new HashMap<>();
            Map<UniformItemPath, Attribute<?, ?>> pathOverrides = new HashMap<>();

            for (Attribute<?, ?> attribute : entity.getAttributes()) {
                Class<?> jType = attribute.getJavaType();
                JaxbPath[] paths = jType.getAnnotationsByType(JaxbPath.class);
                var member = attribute.getJavaMember();
                if (paths == null || paths.length == 0) {
                    if (member instanceof Method) {
                        paths = ((Method) member).getAnnotationsByType(JaxbPath.class);
                    } else if (member instanceof Field) {
                        paths = ((Field) member).getAnnotationsByType(JaxbPath.class);
                    }

                }

                if (paths == null || paths.length == 0) {
                    JaxbName name = null;
                    if (member instanceof Method) {
                        name = ((Method) member).getAnnotation(JaxbName.class);
                    } else if (member instanceof Field) {
                        name = ((Field) member).getAnnotation(JaxbName.class);
                    }
                    if (name != null) {
                        overrides.put(name.localPart(), attribute);
                    }
                    continue;
                }

                for (JaxbPath path : paths) {
                    JaxbName[] names = path.itemPath();
                    if (names.length == 1) {
                        overrides.put(names[0].localPart(), attribute);
                    } else {
                        UniformItemPath customPath = prismContext.emptyPath();
                        for (JaxbName name : path.itemPath()) {
                            customPath = customPath.append(new QName(name.namespace(), name.localPart()));
                        }
                        pathOverrides.put(customPath, attribute);
                    }
                }
            }

            if (!overrides.isEmpty()) {
                attributeNameOverrides.put(entity, overrides);
            }
            if (!pathOverrides.isEmpty()) {
                attributeNamePathOverrides.put(entity, pathOverrides);
            }
        }

        LOGGER.debug("Initialization finished");
    }

    public <T> ManagedType<T> getJaxbMapping(Class<T> jaxbType) {
        //noinspection unchecked
        return (ManagedType<T>) jaxbMappings.get(jaxbType);
    }

    public <T> ManagedType<T> getMapping(Class<T> entityType) {
        return metamodel.managedType(entityType);
    }

    public <T> Attribute<T, ?> findAttribute(ManagedType<T> type, String name) {
        try {
            //noinspection unchecked
            return (Attribute<T, ?>) type.getAttribute(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public <T> Attribute<T, ?> findAttributeOverride(ManagedType<T> type, String nameOverride) {
        Map<String, Attribute<?, ?>> overrides = attributeNameOverrides.get(type);
        if (overrides == null) {
            return null;
        }

        //noinspection unchecked
        return (Attribute<T, ?>) overrides.get(nameOverride);
    }

    public boolean hasAttributePathOverride(ManagedType<?> type, ItemPath pathOverride) {
        Map<UniformItemPath, Attribute<?, ?>> overrides = attributeNamePathOverrides.get(type);
        if (overrides == null) {
            return false;
        }

        ItemPath namedOnly = pathOverride.namedSegmentsOnly();

        for (UniformItemPath path : overrides.keySet()) {
            if (path.isSuperPathOrEquivalent(namedOnly)) {
                return true;
            }
        }

        return false;
    }

    public <T> Attribute<T,?> findAttributePathOverride(ManagedType<T> type, ItemPath pathOverride) {
        //noinspection unchecked,rawtypes
        Map<UniformItemPath, Attribute<T, ?>> overrides = (Map) attributeNamePathOverrides.get(type);
        if (overrides == null) {
            return null;
        }

        return overrides.get(prismContext.toUniformPath(pathOverride));
    }
}
