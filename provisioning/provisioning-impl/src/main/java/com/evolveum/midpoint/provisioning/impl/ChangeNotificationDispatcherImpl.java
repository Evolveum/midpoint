/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class ChangeNotificationDispatcherImpl implements ChangeNotificationDispatcher {
	
	private List<ResourceObjectChangeListener> listeners = new ArrayList<ResourceObjectChangeListener>();
	
	private static final Trace LOGGER = TraceManager.getTrace(ChangeNotificationDispatcherImpl.class);
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeNotificationManager#registerNotificationListener(com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener)
	 */
	@Override
	public synchronized void registerNotificationListener(ResourceObjectChangeListener listener) {
		if (listeners.contains(listener)) {
			LOGGER.warn(
					"Resource object change listener '{}' is already registered. Subsequent registration is ignored",
					listener);
		} else {
			listeners.add(listener);
		}

	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeNotificationManager#unregisterNotificationListener(com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener)
	 */
	@Override
	public synchronized void unregisterNotificationListener(ResourceObjectChangeListener listener) {
		listeners.remove(listener);
	}


	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#notifyChange(com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowChangeDescriptionType, com.evolveum.midpoint.common.result.OperationResult)
	 */
	@Override
	public void notifyChange(ResourceObjectShadowChangeDescription change, Task task, OperationResult parentResult) {
		Validate.notNull(change, "Change description of resource object shadow must not be null.");
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("SYNCHRONIZATION change notification\n{} ", change.dump());
		}
		
		change.checkConsistence();
		
		if ((null != listeners) && (!listeners.isEmpty())) {
			for (ResourceObjectChangeListener listener : listeners) {
				//LOGGER.trace("Listener: {}", listener.getClass().getSimpleName());
				try {
					listener.notifyChange(change, task, parentResult);
				} catch (RuntimeException e) {
					LOGGER.error("Exception {} thrown by object change listener {}: {}", new Object[]{
							e.getClass(), listener.getName(), e.getMessage(), e });
				}
			}
		} else {
			LOGGER.warn("Change notification received but listener list is empty, there is nobody to get the message");
		}
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#getName()
	 */
	@Override
	public String getName() {
		return "object change notification dispatcher";
	}

}
