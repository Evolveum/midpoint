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
package com.evolveum.midpoint.model.test.util.equal;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * 
 * @author lazyman
 * 
 */
public class ResourceTypeComparator extends Equals<ResourceType> {

	private static final Trace LOGGER = TraceManager.getTrace(ResourceTypeComparator.class);

	@Override
	public boolean areEqual(ResourceType o1, ResourceType o2) {
		if (!new ExtensibleObjectTypeComparator().areEqual(o1, o2)) {
			return false;
		}
		
		if (o1 == null && o2 == null) {
			return true;
		}
		LOGGER.warn("ResourceObjectShadowTypeComparator is not comparing all class members (not implemented yet).");

		// TODO Auto-generated method stub
		o1.getConfiguration();
		o1.getConnector();
		o1.getConnectorRef();
		o1.getSchema();
		o1.getSchemaHandling();
		o1.getScripts();
		o1.getSynchronization();

		return areStringEqual(o1.getNamespace(), o2.getNamespace());
	}
}
