/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
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
		panelsForHandlers = new LinkedHashMap<>();			// order is important!
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
		return new HandlerPanelFactory();		// TODO
	}

	public Panel createPanelForTask(String id, IModel<? extends HandlerDto> handlerDtoModel, PageTaskEdit parentPage) {
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

	private Panel instantiate(Class<? extends Panel> clazz, String id, IModel<? extends HandlerDto> handlerDtoModel, PageTaskEdit parentPage) {
		try {
			try {
				Constructor<?> constructor = clazz.getConstructor(String.class, IModel.class, PageTaskEdit.class);
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
