/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.web.boot;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;

/**
 * Fake root context. This context does not really do anything.
 * But it is "mapped" to the root URL (/ ... or rather "" in Tomcat parlance).
 * This fake context is necessary. If there is no context at all then
 * CoyoteAdapter will not execute any Valves and returns 404 immediately. 
 * So without this the TomcatRootValve will not work.
 * 
 * @author semancik
 */
public class RootRootContext extends StandardContext {
	
	public RootRootContext() {
		super();
		setPath("/ "); // this means "/"
		setDisplayName("RootRoot");
	}
	
	// HAck
	@Override
	public void resourcesStart() throws LifecycleException {
		super.resourcesStart();
		setConfigured(true);
	}

}
