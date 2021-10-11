/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine.actions;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.wf.api.request.*;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Component
public class ActionFactory {

    private Map<Class<? extends Request>, Class<? extends Action>> requestToActionMap = new HashMap<>();

    {
        requestToActionMap.put(CompleteWorkItemsRequest.class, CompleteWorkItemsAction.class);
        requestToActionMap.put(DelegateWorkItemsRequest.class, DelegateWorkItemsAction.class);
        requestToActionMap.put(ClaimWorkItemsRequest.class, ClaimWorkItemsAction.class);
        requestToActionMap.put(ReleaseWorkItemsRequest.class, ReleaseWorkItemsAction.class);
        requestToActionMap.put(CancelCaseRequest.class, CancelCaseAction.class);
        requestToActionMap.put(OpenCaseRequest.class, OpenCaseAction.class);
    }

    public Action create(Request request, EngineInvocationContext ctx) {
        Class<? extends Action> actionClass = requestToActionMap.get(request.getClass());
        if (actionClass != null) {
            try {
                return actionClass
                        .getConstructor(EngineInvocationContext.class, request.getClass())
                        .newInstance(ctx, request);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new SystemException("Couldn't invoke constructor on action class " + actionClass.getName() + ": " + e.getMessage(), e);
            }
        } else {
            throw new IllegalArgumentException("No action for request: " + request);
        }
    }
}
