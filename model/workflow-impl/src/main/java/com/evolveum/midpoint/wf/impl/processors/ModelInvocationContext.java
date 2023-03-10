/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;

/**
 * Captures the information from the invocation of the approvals (workflow) hook.
 */
public class ModelInvocationContext<T extends ObjectType> {

    private static final Trace LOGGER = TraceManager.getTrace(ModelInvocationContext.class);

    @NotNull public final LensContext<T> modelContext;
    @Nullable public final WfConfigurationType wfConfiguration;
    @NotNull public final Task task;
    @NotNull public final RepositoryService repositoryService;

    public ModelInvocationContext(
            @NotNull ModelContext<T> modelContext,
            @Nullable WfConfigurationType wfConfiguration,
            @NotNull RepositoryService repositoryService,
            @NotNull Task task) {
        this.modelContext = (LensContext<T>) modelContext;
        this.wfConfiguration = wfConfiguration;
        this.task = task;
        this.repositoryService = repositoryService;
    }

    /**
     * Retrieves focus object name from the model context.
     */
    public String getFocusObjectName() {
        ObjectType object = getFocusObjectNewOrOld();
        return object.getName() != null ? object.getName().getOrig() : null;
    }

    public String getFocusObjectOid() {
        LensFocusContext<?> fc = modelContext.getFocusContext();
        if (fc.getObjectNew() != null && fc.getObjectNew().getOid() != null) {
            return fc.getObjectNew().getOid();
        } else if (fc.getObjectOld() != null && fc.getObjectOld().getOid() != null) {
            return fc.getObjectOld().getOid();
        } else {
            return null;
        }
    }

    public ObjectType getFocusObjectNewOrOld() {
        LensFocusContext<? extends ObjectType> fc = modelContext.getFocusContext();
        PrismObject<? extends ObjectType> prism = fc.getObjectNew() != null ? fc.getObjectNew() : fc.getObjectOld();
        if (prism == null) {
            throw new IllegalStateException("No object (new or old) in model context");
        }
        return prism.asObjectable();
    }

    public PrismObject<? extends FocusType> getRequestor(OperationResult result) {
        if (task.getOwnerRef() == null) {
            LOGGER.warn("No requester in task {} -- continuing, but the situation is suspicious.", task);
            return null;
        }

        // let's get fresh data (not the ones read on task creation, that may be - e.g. if used from GUI - the state of
        // logged in midPoint principal, which is determined at logon)
        try {
            return repositoryService.getObject(UserType.class, task.getOwnerRef().getOid(), null, result);
        } catch (ObjectNotFoundException e) {
            PrismObject<? extends FocusType> taskOwner = task.getOwner(result);
            LoggingUtils.logException(LOGGER, "Couldn't get data about task requester (" + taskOwner + "), "
                    + "because it does not exist in repository anymore. Using cached data.", e);
            return CloneUtil.clone(taskOwner); // may be still null if it was not cached
        } catch (SchemaException e) {
            PrismObject<? extends FocusType> taskOwner = task.getOwner(result);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get data about task requester (" + taskOwner + "), "
                    + "due to schema exception. Using cached data.", e);
            return CloneUtil.clone(taskOwner); // may be still null if it was not cached
        }
    }
}
