/**
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or CDDLv1.0.txt file in the source
 * code distribution. See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.common.configuration.api;

/**
 * @author mamut
 */
import org.apache.commons.configuration.Configuration;

public interface SystemConfiguration {
	/**
	 * get configuration for symbolic name of the component from configuration
	 * subsytem.
	 * 
	 * 
	 * @param componetID
	 *            name of the component Samples of names: <li>
	 *            <ul>
	 *            repository -> midpoint.repository
	 *            </ul>
	 *            <ul>
	 *            provisioning -> midpoint.provisioning
	 *            </ul>
	 *            <ul>
	 *            model -> midpoint.model
	 *            </ul>
	 *            </li>
	 * 
	 * @return Configuration object {@link http
	 *         ://commons.apache.org/configuration
	 *         /apidocs/org/apache/commons/configuration/Configuration.html}
	 *         Sample how to get config value:
	 *         {@code config.getInt("port", 1234);}
	 */
	public Configuration getConfiguration(String componetID);
}
