/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.repo.api.CacheInvalidationDispatcher;
import com.evolveum.midpoint.repo.api.CacheInvalidationEventSpecification;
import com.evolveum.midpoint.repo.api.CacheInvalidationListener;
import com.evolveum.midpoint.repo.api.DeleteObjectResult;
import com.evolveum.midpoint.repo.cache.invalidation.RepositoryCacheInvalidationDetails;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProcessedDocumentationType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Listens for {@link ConnectorDevelopmentType} object deletions and removes the files that were stored
 * for the deleted connector development on the file system - currently the processed documentation
 * files ({@code <midpointHome>/tmp-docs/<uuid>}).
 *
 * The installed connector bundle itself (and the {@code ConnectorType} object it corresponds to) is
 * intentionally left in place, as it may still be in use by resources.
 *
 * Note that the deleted object's content is available only for deletions performed on this node
 * (via {@link DeleteObjectResult}); invalidation events received from other cluster nodes carry no
 * details, so cleanup happens on the node where the object was deleted.
 */
@Component
public class ConnectorDevelopmentCacheInvalidationListener implements CacheInvalidationListener {

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorDevelopmentCacheInvalidationListener.class);

    @Autowired private CacheInvalidationDispatcher cacheInvalidationDispatcher;
    @Autowired private MidpointConfiguration configuration;

    @PostConstruct
    public void register() {
        cacheInvalidationDispatcher.registerListener(this);
    }

    @PreDestroy
    public void unregister() {
        cacheInvalidationDispatcher.unregisterListener(this);
    }

    @Override
    public Collection<CacheInvalidationEventSpecification> getEventSpecifications() {
        return Set.of(CacheInvalidationEventSpecification.of(
                ConnectorDevelopmentType.class, Set.of(ChangeType.DELETE)));
    }

    @Override
    public synchronized <O extends ObjectType> void invalidate(
            @Nullable Class<O> type, @Nullable String oid, @Nullable CacheInvalidationContext context) {
        if (type == null || !ConnectorDevelopmentType.class.equals(type) || oid == null || context == null) {
            return;
        }

        // We can react only to local deletions: only then does the context carry the repository
        // operation result (with the deleted object's content). Events from other cluster nodes
        // (and manual invalidations) have no details.
        if (!(context.getDetails() instanceof RepositoryCacheInvalidationDetails details)
                || !(details.getResult() instanceof DeleteObjectResult deleteResult)) {
            return;
        }

        String objectXml = deleteResult.getObjectTextRepresentation();
        if (objectXml == null) {
            LOGGER.trace("No deleted object content available for connector development {}; skipping file cleanup", oid);
            return;
        }

        try {
            PrismObject<ConnectorDevelopmentType> object = PrismContext.get().parseObject(objectXml);
            cleanupDocumentationFiles(object.asObjectable(), oid);
        } catch (SchemaException | RuntimeException e) {
            // File cleanup is best-effort; never fail the (already committed) delete operation because of it.
            LOGGER.error("Couldn't clean up files of deleted connector development {}: {}", oid, e.getMessage(), e);
        }
    }

    private void cleanupDocumentationFiles(ConnectorDevelopmentType development, String oid) {
        File storageDir = new File(configuration.getMidpointHome(), ProcessedDocumentation.STORAGE_DIR_NAME);
        for (ProcessedDocumentationType documentation : MiscUtil.emptyIfNull(development.getProcessedDocumentation())) {
            File file = new File(storageDir, documentation.getUuid());
            if (file.exists() && file.delete()) {
                LOGGER.debug("Removed documentation file {} of deleted connector development {}", file, oid);
            }
        }
    }
}
