/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import com.evolveum.midpoint.provisioning.util.DefinitionsUtil;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.processor.ShadowCoordinatesQualifiedObjectDelta;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Implements various methods of `applyDefinition` kind.
 */
@Experimental
@Component
class DefinitionsHelper {

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired protected ShadowManager shadowManager;
    @Autowired private ProvisioningContextFactory ctxFactory;

    public void applyDefinition(ObjectDelta<ShadowType> delta, @Nullable ShadowType repoShadow,
            Task task, OperationResult result) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ShadowType> shadow = null;
        ResourceShadowCoordinates coordinates = null;
        if (delta.isAdd()) {
            shadow = delta.getObjectToAdd();
        } else if (delta.isModify()) {
            if (delta instanceof ShadowCoordinatesQualifiedObjectDelta) {
                // This one does not have OID, it has to be specially processed
                coordinates = ((ShadowCoordinatesQualifiedObjectDelta<?>) delta).getCoordinates();
            } else {
                String shadowOid = delta.getOid();
                if (shadowOid == null) {
                    if (repoShadow == null) {
                        throw new IllegalArgumentException("No OID in object delta " + delta
                                + " and no externally-supplied shadow is present as well.");
                    }
                    shadow = repoShadow.asPrismObject();
                } else {
                    // TODO consider fetching only when really necessary
                    shadow = repositoryService.getObject(delta.getObjectTypeClass(), shadowOid, null, result);
                }
            }
        } else {
            // Delete delta, nothing to do at all
            return;
        }
        ProvisioningContext ctx;
        if (shadow == null) {
            stateCheck(coordinates != null, "No shadow nor coordinates");
            ctx = ctxFactory.createForShadowCoordinates(coordinates, task, result);
        } else {
            ctx = ctxFactory.createForShadow(shadow, task, result);
        }
        ctx.applyAttributesDefinition(delta);
    }

    public void applyDefinition(ShadowType shadow, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ProvisioningContext ctx = ctxFactory.createForShadow(shadow, task, result);
        ctx.applyAttributesDefinition(shadow);
    }

    public void applyDefinition(ObjectQuery query, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ProvisioningContext ctx = ctxFactory.createForShadowCoordinates(
                ObjectQueryUtil.getShadowCoordinates(query),
                task,
                result);
        DefinitionsUtil.applyDefinition(ctx, query);
    }
}
