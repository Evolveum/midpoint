/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class ProjectionMappingLoader implements MappingLoader<ShadowType> {

    private static final Trace LOGGER = TraceManager.getTrace(ProjectionMappingLoader.class);

    private final LensProjectionContext projectionContext;
    private final ContextLoader contextLoader;
    /** Ugly hack. */
    private final LoadedStateProvider loadedStateProvider;

    public ProjectionMappingLoader(
            LensProjectionContext projectionContext,
            ContextLoader contextLoader,
            LoadedStateProvider loadedStateProvider) {
        this.projectionContext = projectionContext;
        this.contextLoader = contextLoader;
        this.loadedStateProvider = loadedStateProvider;
    }

    @Override
    public boolean isLoaded() throws SchemaException, ConfigurationException {
        return loadedStateProvider.isLoaded();
    }

    @Override
    public PrismObject<ShadowType> load(String loadReason, Task task, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        contextLoader.loadFullShadow(projectionContext, loadReason, task, result);
        if (projectionContext.isBroken()) {
            LOGGER.debug("Attempt to load full object for {} failed, projection context is broken", projectionContext.getHumanReadableName());
            throw new ObjectNotFoundException("Projection loading failed, projection broken"); // TODO is this correct exception?
        }
        if (projectionContext.isGone()) {
            LOGGER.debug("Projection {} is gone", projectionContext.getHumanReadableName());
            throw new ObjectNotFoundException("Projection loading failed, projection gone"); // TODO is this correct exception?
        }
        return projectionContext.getObjectCurrent();
    }
}
