/**
11 * Copyright (c) 2018 Evolveum
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

import org.apache.catalina.startup.Tomcat;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
public class MidPointTomcatEmbeddedServletContainerFactory extends TomcatEmbeddedServletContainerFactory {
	
	private static final Trace LOGGER = TraceManager.getTrace(MidPointTomcatEmbeddedServletContainerFactory.class);

	@Override
	protected TomcatEmbeddedServletContainer getTomcatEmbeddedServletContainer(Tomcat tomcat) {

		// TODO: doc
		
		RootRootContext rootRootContext = new RootRootContext();
		tomcat.getHost().addChild(rootRootContext);
		
		return super.getTomcatEmbeddedServletContainer(tomcat);
	}
	
	

}
