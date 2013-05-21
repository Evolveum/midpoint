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

package com.evolveum.midpoint.common.configuration.api;

import org.apache.commons.configuration.Configuration;

public interface RuntimeConfiguration {

	/**
	 * Return symbolic name of the component in configuration subsytem.
	 *  Samples:
	 *  <li>
	 *  	<ul>repository -> midpoint.repository</ul>
	 *  	<ul>provisioning  -> midpoint.provisioning</ul>
	 *  	<ul>model -> midpoint.model</ul>
	 *  </li>
	 * @return	String name of component
	 */

	public String getComponentId();

	/**
	 * Returns current component configuration in commons configuration structure 
	 * {@link http://commons.apache.org/configuration/apidocs/org/apache/commons/configuration/Configuration.html}
	 * 
	 * <p>
	 * Example  of structure for repository:
	 * </p>
	 * <p>
	 * {@code
	 * 
	 *		Configuration config =  new BaseConfiguration(); 
	 *		config.setProperty("host", "localhost");
	 * 		config.setProperty("port" , 12345);
	 * 		return config;
	 * }
	 * </p>
	 *  
	 * Note: current configuration can be obtained only on fully initialized objects. If called on not initialized objects, then it can end with undefined behavior 
	 *  
	 * @return	Commons configuration
	 */
	public Configuration getCurrentConfiguration();
}
