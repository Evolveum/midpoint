/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.provisioning.api;

/**
 * Dispatcher of change notifications.
 * 
 * Instances that implement this interface relay notification from the source of the change notification to the
 * destinations. The destinations are chosen dynamically, using a publish-subscribe mechanism.
 * 
 * This interface also includes ResourceObjectChangeListener. By invoking the notifyChange(..) operation of this
 * interface the change will be relayed to all registered listeners.
 * 
 * @author Katka Valalikova
 * @author Radovan Semancik
 *
 */
public interface ChangeNotificationDispatcher extends ResourceObjectChangeListener, ResourceOperationListener, ResourceEventListener {
	
	public  void registerNotificationListener(ResourceObjectChangeListener listener);
	public  void registerNotificationListener(ResourceOperationListener listener);
	public  void registerNotificationListener(ResourceEventListener listener);
		
	public  void unregisterNotificationListener(ResourceObjectChangeListener listener);
	public  void unregisterNotificationListener(ResourceOperationListener listener);
	public  void unregisterNotificationListener(ResourceEventListener listener);
	
	
	
}
