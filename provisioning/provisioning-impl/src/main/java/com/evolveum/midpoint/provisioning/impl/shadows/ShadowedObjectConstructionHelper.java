/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Helps with shadowed object construction.
 *
 * Currently it delegates these activities to dedicated classes: {@link ShadowAcquisition}, {@link ShadowedObjectConstruction}.
 */
@Experimental
@Component
class ShadowedObjectConstructionHelper {

    @Autowired private CommonBeans commonBeans;

    /**
     * TODO improve the description
     * TODO devise better name ... like constructAdoptedObject? dunno...
     *
     * Make sure that the repo shadow is complete, e.g. that all the mandatory fields
     * are filled (e.g name, resourceRef, ...) Also transforms the shadow with
     * respect to simulated capabilities. Also shadowRefs are added to associations.
     */
    @NotNull ShadowType constructShadowedObject(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType repoShadow,
            @NotNull ShadowType resourceObject,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

        return ShadowedObjectConstruction
                .create(ctx, repoShadow, resourceObject, commonBeans)
                .construct(result);
    }
}
