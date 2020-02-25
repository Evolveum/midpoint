/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.*;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author mederly
 */
public class HandlerPanelFactory {

    private static final Trace LOGGER = TraceManager.getTrace(HandlerPanelFactory.class);

    public static Map<Class<? extends HandlerDto>, Class<? extends Panel>> panelsForHandlers;

    static {
        panelsForHandlers = new LinkedHashMap<>();            // order is important!
        panelsForHandlers.put(LiveSyncHandlerDto.class, LiveSyncHandlerPanel.class);
        panelsForHandlers.put(ResourceRelatedHandlerDto.class, ResourceRelatedHandlerPanel.class);
        panelsForHandlers.put(ScannerHandlerDto.class, ScannerHandlerPanel.class);
        panelsForHandlers.put(ScriptExecutionHandlerDto.class, ScriptExecutionHandlerPanel.class);
        panelsForHandlers.put(DeleteHandlerDto.class, DeleteHandlerPanel.class);
        panelsForHandlers.put(RecomputeHandlerDto.class, QueryBasedHandlerPanel.class);
        panelsForHandlers.put(ExecuteChangesHandlerDto.class, ExecuteChangesHandlerPanel.class);
        panelsForHandlers.put(GenericHandlerDto.class, GenericHandlerPanel.class);
        panelsForHandlers.put(ReportCreateHandlerDto.class, ReportCreateHandlerPanel.class);
        panelsForHandlers.put(HandlerDto.class, DefaultHandlerPanel.class);

    }
    public static HandlerPanelFactory instance() {
        return new HandlerPanelFactory();        // TODO
    }

    public Panel createPanelForTask(String id, IModel<? extends HandlerDto> handlerDtoModel, PageBase parentPage) {
        HandlerDto handlerDto = handlerDtoModel.getObject();
        for (Map.Entry<Class<? extends HandlerDto>, Class<? extends Panel>> entry : panelsForHandlers.entrySet()) {
            //System.out.println("Checking " + entry.getKey());
            if (entry.getKey().isAssignableFrom(handlerDto.getClass())) {
                LOGGER.trace("Using {} for {}", entry.getValue(), entry.getKey());
                return instantiate(entry.getValue(), id, handlerDtoModel, parentPage);
            }
        }
        throw new IllegalStateException("No panel for " + handlerDto.getClass());
    }

    private Panel instantiate(Class<? extends Panel> clazz, String id, IModel<? extends HandlerDto> handlerDtoModel, PageBase parentPage) {
        try {
            try {
                Constructor<?> constructor = clazz.getConstructor(String.class, IModel.class, PageBase.class);
                return (Panel) constructor.newInstance(id, handlerDtoModel, parentPage);
            } catch (NoSuchMethodException e) {
                try {
                    Constructor<?> constructor = clazz.getConstructor(String.class, IModel.class);
                    return (Panel) constructor.newInstance(id, handlerDtoModel);
                } catch (NoSuchMethodException e1) {
                    throw new SystemException("Couldn't instantiate " + clazz + ": " + e1.getMessage(), e1);
                }
            }
        } catch (InstantiationException|IllegalAccessException|InvocationTargetException|RuntimeException e) {
            throw new SystemException("Couldn't instantiate " + clazz + ": " + e.getMessage(), e);
        }
    }
}
