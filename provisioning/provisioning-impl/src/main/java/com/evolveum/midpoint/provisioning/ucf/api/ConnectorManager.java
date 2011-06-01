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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import java.util.Set;

/**
 * Interface to Unified Connector functionality.
 * 
 * This is kind of connector facade. It is an API provided by
 * the "Unified Connector Framework" to the midPoint provisioning
 * component. There is no associated SPI yet. That may come in the
 * future when this interface stabilizes a bit.
 * 
 * As it is an API for midPoint, it is using midPoint-specific classes,
 * such as Schema.
 * 
 * Calls to this interface always try to reach the resource and get the
 * actual state on resource. The connectors are not supposed to cache any
 * information. Therefore the methods do not follow get/set java convention
 * as the data are not regular javabean properties.

 * 
 * @author Radovan Semancik
 *
 */
public interface ConnectorManager {

	/**
	 * Creates new instance of the connector.
	 * 
	 * The factory does NOT cache or pool the connector instances. Call to this
	 * method will always create new connector instance.
	 * 
	 * May return null if the resource definition cannot be handled by this factory
	 * instance. E.g. it does not have configuration or the configuration is meant for
	 * a different factory.
	 * 
	 * TODO throw misconfiguration exception if the configuration is obviously meant for
	 * this connector but it cannot be applied.
	 * 
	 * @param resource
	 * @return
	 */
	public ConnectorInstance createConnectorInstance(ResourceType resource);
	
	public Set<ConnectorType> listConnectors();
	
	public ConnectorType getConnector(String oid);
}