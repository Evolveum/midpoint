/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.manual;

import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.ModelCrudService;
import com.evolveum.midpoint.model.impl.sync.tasks.imp.ImportFromResourceLauncher;
import com.evolveum.midpoint.provisioning.ucf.api.TicketingService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

@Component
public class TicketingServiceImpl implements TicketingService {

    @Autowired private ModelCrudService modelCrudService;
    @Autowired private ModelService modelService;
    @Autowired private ImportFromResourceLauncher importFromResourceLauncher;

    @Override
    public @NotNull String addCase(
            @NotNull CaseType aCase,
            @NotNull TicketingResourceSpec ticketingResourceSpec,
            @NotNull Task task,
            @NotNull OperationResult result) {
        // We set the case state to "open" so it won't be processed by midPoint built-in case engine
        // TODO this is quite an ugly hack; we should replace it by a different mechanism
        aCase.setState(SchemaConstants.CASE_STATE_OPEN);
        try {
            var ticketShadow = createEmptyTicketShadow(ticketingResourceSpec, task, result);
            aCase.getLinkRef().add(ticketShadow.getRefWithEmbeddedObject());
            return modelCrudService.addObject(aCase.asPrismObject(), null, task, result);
        } catch (Throwable t) {
            throw SystemException.unexpected(t, "Couldn't create a ticket for " + aCase);
        }
    }

    private AbstractShadow createEmptyTicketShadow(
            @NonNull TicketingResourceSpec ticketingResourceSpec, @NonNull Task task, @NonNull OperationResult result)
            throws CommonException {
        var ticketingResourcePrismObject =
                modelService.getObject(ResourceType.class, ticketingResourceSpec.resourceOid(), null, task, result);
        var ticketingResource = Resource.of(ticketingResourcePrismObject);
        return ticketingResource
                .shadow(ticketingResourceSpec.ticketingObjectType())
                .asAbstractShadow();
    }

    @Override
    public @NonNull CaseType getCase(@NonNull String caseOid, @NonNull Task task, @NonNull OperationResult result) {
        try {
            // Obtaining shadow OID (there should be exactly one)
            var aCase = modelService.getObject(CaseType.class, caseOid, null, task, result).asObjectable();
            var ticketRef = MiscUtil.extractSingletonRequired(
                    aCase.getLinkRef(),
                    () -> new IllegalStateException("Multiple projections for " + aCase),
                    () -> new IllegalStateException("No projections for " + aCase));

            // Reading the shadow from the ticketing resource + synchronizing with the case object
            importFromResourceLauncher.importSingleShadow(ticketRef.getOid(), task, result);

            // Re-reading the case object after synchronization, to be returned
            return modelService.getObject(CaseType.class, caseOid, null, task, result).asObjectable();

        } catch (Throwable t) {
            throw SystemException.unexpected(t, "Couldn't get a ticket for " + caseOid);
        }
    }
}
