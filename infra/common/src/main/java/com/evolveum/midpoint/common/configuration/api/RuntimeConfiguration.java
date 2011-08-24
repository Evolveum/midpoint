/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
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
