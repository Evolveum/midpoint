/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.operations;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class OperationsHelper {

    private static final Trace LOGGER = TraceManager.getTrace(OperationsHelper.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;

    @NotNull
    public <T extends ObjectType> PrismObject<T> getRepoObject(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        try {
            return cacheRepositoryService.getObject(type, oid, options, result);
        } catch (ObjectNotFoundException e) {
            GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
            if (!GetOperationOptions.isAllowNotFound(rootOptions)) {
                ProvisioningUtil.recordFatalErrorWhileRethrowing(
                        LOGGER, result, "Can't get object with oid " + oid + ". Reason " + e.getMessage(), e);
            } else {
                // TODO check if this is really needed (lower layers shouldn't produce FATAL_ERROR if allow not found is true)
                result.muteLastSubresultError();
                result.computeStatus();
            }
            throw e;
        } catch (SchemaException ex) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(
                    LOGGER, result, "Can't get object with oid " + oid + ". Reason " + ex.getMessage(), ex);
            throw ex;
        }
    }
}
