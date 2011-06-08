/**
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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.provisioning.impl;

import javax.jws.WebParam;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.FaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.IllegalArgumentFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;

/**
 * TEMPORARY HACK: wrapper to adapt old repository interface to the new one
 * 
 * WORK IN PROGRESS
 * 
 * @author Radovan Semancik
 * 
 */
public class RepositoryWrapper {

	RepositoryPortType repository;

	public RepositoryWrapper(RepositoryPortType repository) {
		super();
		this.repository = repository;
	}

	public RepositoryPortType getRepository() {
		return repository;
	}

	public void setRepository(RepositoryPortType repository) {
		this.repository = repository;
	}

	public ObjectType getObject(String oid,PropertyReferenceListType resolve, OperationResult parentResult) throws ObjectNotFoundException {
		// TODO: use result
		try {
			ObjectContainerType container = repository.getObject(oid, resolve);
			return container.getObject();
		} catch (FaultMessage fault) {
			FaultType faultInfo = fault.getFaultInfo();
			if (faultInfo instanceof IllegalArgumentFaultType) {
				throw new IllegalArgumentException(faultInfo.getMessage());
			}
			if (faultInfo instanceof ObjectNotFoundFaultType) {
				throw new ObjectNotFoundException(faultInfo.getMessage());
			}
			// Any other type is SysteFault or something unexpected.
			// Just wrap in RuntimeExcpetion
			throw new RuntimeException(faultInfo.getClass().getSimpleName()+": "+faultInfo.getMessage());
		}
	}

	public String addObject(ObjectType object) {
		ObjectContainerType container = new ObjectContainerType();
		container.setObject(object);
		try {
			return repository.addObject(container);
		} catch (FaultMessage fault) {
			FaultType faultInfo = fault.getFaultInfo();
			// TODO Auto-generated catch block
			throw new RuntimeException(faultInfo.getClass().getSimpleName()+": "+faultInfo.getMessage());
		}
	}
	
	

}
