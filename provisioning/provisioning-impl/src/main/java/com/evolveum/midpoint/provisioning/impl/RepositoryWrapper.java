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

import java.util.List;

import javax.jws.WebParam;

import org.apache.commons.lang.NotImplementedException;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.FaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.IllegalArgumentFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;

/**
 * TEMPORARY HACK: wrapper to adapt old repository interface to the new one
 * 
 * This is a class that adapts existing (WSDL) repository interface to a new interface.
 * The new interface is not yet applied to repository, but it eventually will be. And
 * we do not want to change the new provisioning code again, so let's create the
 * new code in a way that suits new interface. 
 * 
 * WORK IN PROGRESS
 * 
 * @author Radovan Semancik
 * 
 */
public class RepositoryWrapper implements RepositoryService {

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

	public ObjectListType searchObjects(QueryType query, PagingType paging) {
		// TODO Auto-generated method stub
		throw new NotImplementedException();
	}

	@Override
	public String addObject(ObjectType object, OperationResult parentResult) throws ObjectAlreadyExistsException,
			SchemaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectListType listObjects(Class objectType, PagingType paging, OperationResult parentResult) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectListType searchObjects(QueryType query, PagingType paging, OperationResult parentResult)
			throws SchemaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void modifyObject(ObjectModificationType objectChange, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteObject(String oid, OperationResult parentResult) throws ObjectNotFoundException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PropertyAvailableValuesListType getPropertyAvailableValues(String oid, PropertyReferenceListType properties,
			OperationResult parentResult) throws ObjectNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserType listAccountShadowOwner(String accountOid) throws ObjectNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ResourceObjectShadowType> listResourceObjectShadows(String resourceOid, Class resourceObjectShadowType)
			throws ObjectNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
