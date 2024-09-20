/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init;

import java.util.*;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.Cache;
import com.evolveum.midpoint.repo.api.CacheRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

public class SchemaCache implements Cache {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaCache.class);

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

        if (!repositoryService.supports(SchemaType.class)) {
            LOGGER.debug("Skip processing schema object from database, because SchemaType isn't supported.");
            return;
        }

        Map<String, Element> dbSchemaExtensions = new HashMap<>();

        ResultHandler<SchemaType> handler = (object, parentResult) -> {
            SchemaType schemaType = object.asObjectable();
            String description = schemaType.getName() + " (" + schemaType.getOid() + ")";
            if (SchemaConstants.LIFECYCLE_PROPOSED.equals(schemaType.getLifecycleState())) {
                LOGGER.debug("Skip processing schema object from database, because SchemaType " + description + " is in proposed lifecycle state.");
                return true;
            }

            SchemaDefinitionType def = schemaType.getDefinition();
            Element schemaElement = def.getSchema();
            dbSchemaExtensions.put("dynamic schema from db: " + description, schemaElement);
            return true;
        };

        OperationResult result = new OperationResult("initExtensionFromDB");
        try {
            repositoryService.searchObjectsIterative(SchemaType.class, null, handler, null, true, result);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't search schema objects", e);
            return;
        }

        if (!dbSchemaExtensions.isEmpty() || prismContext.getSchemaRegistry().existDynamicSchemaExtensions()) {
            try {
                prismContext.getSchemaRegistry().registerDynamicSchemaExtensions(dbSchemaExtensions);
                prismContext.reload();
            } catch (SchemaException e) {
                LOGGER.error("Couldn't reload schema", e);
            }
        }
    }

    private static final Collection<Class<?>> INVALIDATION_RELATED_CLASSES = Arrays.asList(
            SchemaType.class
    );

    @Override
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        if (type == null || INVALIDATION_RELATED_CLASSES.contains(type)) {
            init();
        }
    }

    @Override
    public @NotNull Collection<SingleCacheStateInformationType> getStateInformation() {
        return Collections.singleton(new SingleCacheStateInformationType());
    }

    @Override
    public void dumpContent() {
    }
}
