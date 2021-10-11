/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author mederly
 */
public class ModelInvocationContext<T extends ObjectType> {

    private static final Trace LOGGER = TraceManager.getTrace(ModelInvocationContext.class);

    @NotNull public final PrismContext prismContext;
    @NotNull public final ModelContext<T> modelContext;
    @Nullable public final WfConfigurationType wfConfiguration;
    @NotNull public final Task task;
    @NotNull public final RepositoryService repositoryService;

    public ModelInvocationContext(@NotNull ModelContext<T> modelContext, @Nullable WfConfigurationType wfConfiguration,
            @NotNull PrismContext prismContext, @NotNull RepositoryService repositoryService, @NotNull Task task) {
        this.prismContext = prismContext;
        this.modelContext = modelContext;
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
        ModelElementContext<?> fc = modelContext.getFocusContext();
        if (fc.getObjectNew() != null && fc.getObjectNew().getOid() != null) {
            return fc.getObjectNew().getOid();
        } else if (fc.getObjectOld() != null && fc.getObjectOld().getOid() != null) {
            return fc.getObjectOld().getOid();
        } else {
            return null;
        }
    }

    public ObjectType getFocusObjectNewOrOld() {
        ModelElementContext<? extends ObjectType> fc = modelContext.getFocusContext();
        PrismObject<? extends ObjectType> prism = fc.getObjectNew() != null ? fc.getObjectNew() : fc.getObjectOld();
        if (prism == null) {
            throw new IllegalStateException("No object (new or old) in model context");
        }
        return prism.asObjectable();
    }

    public PrismObject<? extends FocusType> getRequestor(OperationResult result) {
        if (task.getOwner() == null) {
            LOGGER.warn("No requester in task {} -- continuing, but the situation is suspicious.", task);
            return null;
        }

        // let's get fresh data (not the ones read on user login)
        PrismObject<? extends FocusType> requester;
        try {
            requester = repositoryService.getObject(UserType.class, task.getOwner().getOid(), null, result);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get data about task requester (" + task.getOwner() + "), because it does not exist in repository anymore. Using cached data.", e);
            requester = task.getOwner().clone();
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get data about task requester (" + task.getOwner() + "), due to schema exception. Using cached data.", e);
            requester = task.getOwner().clone();
        }
        return requester;
    }


}
