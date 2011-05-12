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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.provisioning.service;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.exceptions.InitialisationException;
import com.evolveum.midpoint.provisioning.pool.ResourceAccessPoolFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceAccessConfigurationType;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

/**
 * Default impleemntation currently is aways using ICF.
 * 
 * @author elek
 */
public class DefaultResourceFactory implements ResourceFactory {

	private static final Trace logger = TraceManager.getTrace(DefaultResourceFactory.class);
	private ResourceAccessPoolFactory factory = new ResourceAccessPoolFactory();
	private GenericKeyedObjectPool pool = null;

	public DefaultResourceFactory() {

		// pool = new ResourceAccessPoolFactory(factory, 200,
		// GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK, 50000, 20);
		pool = new GenericKeyedObjectPool(factory, -1);
	}

	@Override
	public <T extends ResourceAccessInterface<?>> void checkin(T resourceAccessInterface) {
		try {
			pool.returnObject(resourceAccessInterface.getClass(), resourceAccessInterface);
		} catch (Exception ex) {
			logger.error("Pool Checkin Exception", ex);
		}
	}

	@Override
	public <T extends ResourceAccessInterface<C>, C extends ResourceConnector<?>> T checkout(Class<T> c,
			C resource, ResourceAccessConfigurationType configuration) throws InitialisationException {
		try {
			T rai = (T) pool.borrowObject(c);
			if (rai.configure(configuration)) {
				return rai.initialise(c, resource);
			}
		} catch (Exception ex) {
			logger.error("Pool Checkout Exception", ex);
			throw new InitialisationException("ResourceAccessInterface for '" + resource.getOid()
					+ "' can not be initialised, reason: " + ex.getMessage(), ex);
		}

		throw new InitialisationException("ResourceAccessInterface for '" + resource.getOid()
				+ "' can not be initialised, unknown reason.");
	}
}
