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

package com.evolveum.midpoint.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.provisioning.synchronization.SynchronizationProcessManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CycleListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CycleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.model.cycle_management_1.CycleManagementPortType;

/**
 * 
 * @author sleepwalker
 */
@Service
public class CycleManagementService implements CycleManagementPortType {

	public static final String SYNCHRONIZATIONCYCLE = "SynchronizationCycle";
	@Autowired(required = true)
	SynchronizationProcessManager synchronizationProcessManager;
	ObjectFactory objectFactory = new ObjectFactory();

	@Override
	public com.evolveum.midpoint.xml.ns._public.common.common_1.CycleListType listCycles(
			com.evolveum.midpoint.xml.ns._public.common.common_1.EmptyType empty)
			throws com.evolveum.midpoint.xml.ns._public.model.cycle_management_1.FaultMessage {
		CycleListType cycles = objectFactory.createCycleListType();
		CycleType cycle = objectFactory.createCycleType();
		cycle.setDisplayName("Synchronization Cycle");
		cycle.setName(SYNCHRONIZATIONCYCLE);
		cycles.getCycle().add(cycle);
		// TODO: set missing information, when it is provided by
		// SynchronizationProcessManager
		return cycles;
	}

	@Override
	public com.evolveum.midpoint.xml.ns._public.common.common_1.EmptyType startCycle(java.lang.String name)
			throws com.evolveum.midpoint.xml.ns._public.model.cycle_management_1.FaultMessage {
		if (SYNCHRONIZATIONCYCLE.equals(name)) {
			synchronizationProcessManager.init();
			return objectFactory.createEmptyType();
		} else {
			throw new IllegalArgumentException("Unknown cycle name " + name);
		}
	}

	@Override
	public com.evolveum.midpoint.xml.ns._public.common.common_1.EmptyType stopCycle(java.lang.String name)
			throws com.evolveum.midpoint.xml.ns._public.model.cycle_management_1.FaultMessage {
		if (SYNCHRONIZATIONCYCLE.equals(name)) {
			synchronizationProcessManager.shutdown();
			return objectFactory.createEmptyType();
		} else {
			throw new IllegalArgumentException("Unknown cycle name " + name);
		}
	}

	@Override
	public void init(javax.xml.ws.Holder<com.evolveum.midpoint.xml.ns._public.common.common_1.EmptyType> empty)
			throws com.evolveum.midpoint.xml.ns._public.model.cycle_management_1.FaultMessage {
		synchronizationProcessManager.init();
	}

	@Override
	public void shutdown(
			javax.xml.ws.Holder<com.evolveum.midpoint.xml.ns._public.common.common_1.EmptyType> empty)
			throws com.evolveum.midpoint.xml.ns._public.model.cycle_management_1.FaultMessage {
		synchronizationProcessManager.shutdown();
	}
}
