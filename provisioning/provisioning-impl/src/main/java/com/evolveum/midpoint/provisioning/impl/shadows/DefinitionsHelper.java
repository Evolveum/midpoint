/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.util.MiscUtil.argNonNull;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.util.DefinitionsUtil;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.processor.ShadowCoordinatesQualifiedObjectDelta;
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

    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private ShadowFinder shadowFinder;

    public void applyDefinition(
            ObjectDelta<ShadowType> delta,
            @Nullable ShadowType repoShadow,
            Task task,
            OperationResult result) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ShadowType shadow;
        ResourceShadowCoordinates coordinates;
        if (delta.isAdd()) {
            shadow = delta.getObjectToAdd().asObjectable();
            coordinates = null;
        } else if (delta.isModify()) {
            if (delta instanceof ShadowCoordinatesQualifiedObjectDelta<?> coordinatesDelta) {
                shadow = null; // not needed
                // This one does not have OID, it has to be specially processed
                coordinates = coordinatesDelta.getCoordinates();
            } else {
                String shadowOid = delta.getOid();
                if (shadowOid == null) {
                    shadow = argNonNull(
                            repoShadow,
                            "No OID in object delta %s and no externally-supplied shadow is present as well.", delta);
                } else {
                    // TODO consider fetching only when really necessary
                    shadow = shadowFinder.getShadowBean(shadowOid, result);
                }
                coordinates = null;
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
        ctx.applyCurrentDefinition(delta);
    }

    public void applyDefinition(ShadowType shadow, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ctxFactory
                .createForShadow(shadow, task, result)
                .applyCurrentDefinition(shadow);
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
