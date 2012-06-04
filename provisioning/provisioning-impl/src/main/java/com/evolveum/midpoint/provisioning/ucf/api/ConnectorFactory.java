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

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import java.util.Set;

/**
 * Interface to Unified Connector functionality.
 * 
 * This is considered the "main" interface of the Unified Connector
 * Framework (UCF) API. It can be used to instantiate connectors which in
 * turn can be used to invoke operations on resources.
 * 
 * The UCF interface is considered to be an abstraction internal to the
 * midPoint project and not really reusable. It is using objects from the
 * midPoint data model. But the longer-term ambition is to use this
 * interface as an prototype of ICF replacement, as ICF is burdened by
 * numerous problems.
 * 
 * Calls to this interface always try to reach the resource and get the
 * actual state on resource. The connectors are not supposed to cache any
 * information. Therefore the methods do not follow get/set java convention
 * as the data are not regular javabean properties.

 * 
 * @author Radovan Semancik
 *
 */
public interface ConnectorFactory {

	String OPERATION_LIST_CONNECTOR = ConnectorFactory.class+".listConnectors";

	/**
	 * Creates new unconfigured instance of the connector.
	 * 
	 * This factory is NOT required to cache or pool the connector instances.
	 * Call to this method may create new connector instance each time it is
	 * called unless an underlying framework is pooling connector instances.
	 * 
	 * May return null if the resource definition cannot be handled by this factory
	 * instance. E.g. it does not have configuration or the configuration is meant for
	 * a different factory.
	 * TODO: Better error handling
	 * 
	 * @param resource resource definition
	 * @return configured and initialized connector instance
	 * @throws ObjectNotFoundException is the specified connector was not found
	 * @throws SchemaException 
	 */
	public ConnectorInstance createConnectorInstance(ConnectorType connectorType, String namespace) throws ObjectNotFoundException, SchemaException;
	
	/**
	 * Returns a list of all known connectors.
	 * 
	 * The returned list contains all connectors known to the system, whether
	 * they are used or not, whethere they are configured or not. It should
	 * be used to list the "capabilities" of the system.
	 * 
	 * Returned connector objects are "virtual". They may not be stored in the
	 * persistent repository and they may disappear if the connector disappears
	 * from the system. The returned connector objects are immutable.
	 * 
	 * @param host definition of a connector host or null for local connector list
	 * @return list of all known connectors.
	 */
	public Set<ConnectorType> listConnectors(ConnectorHostType host, OperationResult parentRestul) throws CommunicationException;

}