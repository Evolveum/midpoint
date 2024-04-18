/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init;

import java.io.IOException;
import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Freezable;

import com.evolveum.midpoint.prism.impl.schema.SchemaParsingUtil;
import com.evolveum.midpoint.repo.api.CacheRegistry;

import com.evolveum.midpoint.util.DOMUtil;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.prism.impl.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.repo.api.Cache;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

public class SchemaCache implements Cache {

    private PrismContext prismContext;
    private RepositoryService repositoryService;

    @Autowired private CacheRegistry cacheRegistry;

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public void setRepositoryService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @PostConstruct
    public void register() {
        cacheRegistry.registerCache(this);
    }

    @PreDestroy
    public void unregister() {
        cacheRegistry.unregisterCache(this);
    }


    public void init() {

        Map<QName, ComplexTypeDefinition> dbExtensions = new HashMap<>();

        //TODO cleanup
        ResultHandler<SchemaType> handler = (object, parentResult) -> {
            try {
                SchemaType schemaType = object.asObjectable();
                QName extensionForType = schemaType.getType();

                SchemaDefinitionType def = schemaType.getDefinition();
                Element schemaElement = def.getSchema();

                PrismSchemaImpl extensionSchema = new PrismSchemaImpl(DOMUtil.getSchemaTargetNamespace(schemaElement));
                SchemaParsingUtil.parse(extensionSchema, schemaElement, true, "schema for " + extensionForType, false);
                ComplexTypeDefinition finalDef = detectExtensionSchemas(extensionSchema, dbExtensions);
                if (finalDef != null) {
                    dbExtensions.put(extensionForType, finalDef);
                }
            } catch (SchemaException e) {
                throw new RuntimeException(e);
            }
            return true;
        };

        OperationResult result = new OperationResult("initExtensionFromDB");
        try {
            repositoryService.searchObjectsIterative(SchemaType.class, null, handler, null, true, result);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        //TODO reload schema registry
        ((SchemaRegistryImpl) prismContext.getSchemaRegistry()).registerDbSchemaExtensions(dbExtensions);
        try {
             prismContext.reload();
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    private ComplexTypeDefinition detectExtensionSchemas(PrismSchema schema, Map<QName, ComplexTypeDefinition> extensionSchemas) {
        for (ComplexTypeDefinition def : schema.getDefinitions(ComplexTypeDefinition.class)) {
            QName typeBeingExtended = def.getExtensionForType(); // e.g. c:UserType
            if (typeBeingExtended != null) {
//                LOGGER.trace("Processing {} as an extension for {}", def, typeBeingExtended);
                if (extensionSchemas.containsKey(typeBeingExtended)) {
                    ComplexTypeDefinition existingExtension = extensionSchemas.get(typeBeingExtended);
                    existingExtension.merge(def);
                    return existingExtension;
                } else {
                    return def.clone();
                }
            }
        }
        return null;
    }

    private static final Collection<Class<?>> INVALIDATION_RELATED_CLASSES = Arrays.asList(
            SchemaType.class
    );

    @Override
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        if (type == null || INVALIDATION_RELATED_CLASSES.contains(type)) {
            init();
//                prismContext.initialize();
        }
    }

    @Override
    public @NotNull Collection<SingleCacheStateInformationType> getStateInformation() {
        return Collections.singleton(new SingleCacheStateInformationType());

//                .size();
    }

    @Override
    public void dumpContent() {
//        if (LOGGER_CONTENT.isInfoEnabled()) {
//            archetypePolicyCache.forEach((k, v) -> LOGGER_CONTENT.info("Cached archetype policy: {}: {}", k, v));
//        }
    }
}
