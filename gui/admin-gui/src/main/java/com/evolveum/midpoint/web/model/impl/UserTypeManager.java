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

package com.evolveum.midpoint.web.model.impl;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.util.Utils;
import com.evolveum.midpoint.util.diff.CalculateXmlDiff;
import com.evolveum.midpoint.util.diff.DiffException;
import com.evolveum.midpoint.web.dto.GuiResourceDto;
import com.evolveum.midpoint.web.model.AccountShadowDto;
import com.evolveum.midpoint.web.model.ObjectStage;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.PagingDto;
import com.evolveum.midpoint.web.model.PropertyAvailableValues;
import com.evolveum.midpoint.web.model.PropertyChange;
import com.evolveum.midpoint.web.model.ResourceDto;
import com.evolveum.midpoint.web.model.UserDto;
import com.evolveum.midpoint.web.model.UserManager;
import com.evolveum.midpoint.web.model.WebModelException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType.Attributes;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;

/**
 * 
 * @author katuska
 */
public class UserTypeManager implements UserManager, Serializable {

	private static final long serialVersionUID = -3457278299468312767L;
	private static final Trace TRACE = TraceManager
			.getTrace(UserTypeManager.class);
	private Class constructUserType;

	@Autowired(required = true)
	private ObjectTypeCatalog objectTypeCatalog;
	@Autowired(required = true)
	private transient ModelPortType model;

	public UserTypeManager(Class constructUserType) {
		this.constructUserType = constructUserType;
	}

	@Override
	public void delete(String oid) throws WebModelException {
		TRACE.info("oid = {}", new Object[] { oid });
		Validate.notNull(oid);

		try { // Call Web Service Operation
			model.deleteObject(oid);
		} catch (FaultMessage ex) {
			TRACE.error("Delete user failed for oid = {}", oid);
			TRACE.error("Exception was: ", ex);
			throw new WebModelException(ex.getFaultInfo().getMessage(),
					"[Web Service Error] Delete user failed for oid " + oid);
		}

	}

	@Override
	public String add(UserDto newObject) throws WebModelException {
		Validate.notNull(newObject);

		try { // Call Web Service Operation
			ObjectContainerType userContainer = new ObjectContainerType();
			userContainer
					.setObject((UserType) newObject.getStage().getObject());
			java.lang.String result = model.addObject(userContainer);
			return result;
		} catch (FaultMessage fault) {
			throw new WebModelException(fault.getFaultInfo().getMessage(),
					"Web Service Error");
		}

	}

	@Override
	public Set<PropertyChange> submit(UserDto changedObject)
			throws WebModelException {
		Validate.notNull(changedObject);

		UserDto oldUser = get(changedObject.getOid(),
				Utils.getResolveResourceList());

		try { // Call Web Service Operation
			ObjectModificationType changes = CalculateXmlDiff.calculateChanges(
					oldUser.getXmlObject(), changedObject.getXmlObject());
			if (changes != null && changes.getOid() != null
					&& changes.getPropertyModification().size() > 0) {
				model.modifyObject(changes);
			}

			Set<PropertyChange> set = null;
			if (null != changes) {
				// TODO: finish this
				set = new HashSet<PropertyChange>();
				List<PropertyModificationType> modifications = changes
						.getPropertyModification();
				for (PropertyModificationType modification : modifications) {
					Set<Object> values = new HashSet<Object>();
					if (modification.getValue() != null) {
						values.addAll(modification.getValue().getAny());
					}
					set.add(new PropertyChange(createQName(modification
							.getPath()), getChangeType(modification
							.getModificationType()), values));
				}
			} 
			return set;
		} catch (FaultMessage fault) {
			throw new WebModelException(fault.getFaultInfo().getMessage(),
					"[Web Service Error] Submit user failed.");
		} catch (DiffException ex) {
			throw new WebModelException(ex.getMessage(),
					"[Diff Error] Submit user failed.");
		}
	}

	private QName createQName(Element element) {
		String namespace = element.getNamespaceURI();
		if (namespace == null) {
			namespace = element.getBaseURI();
		}
		return new QName(namespace, element.getLocalName(), element.getPrefix());
	}

	private PropertyChange.ChangeType getChangeType(
			PropertyModificationTypeType type) {
		if (type == null) {
			return null;
		}
		switch (type) {
		case add:
			return PropertyChange.ChangeType.ADD;
		case delete:
			return PropertyChange.ChangeType.DELETE;
		case replace:
			return PropertyChange.ChangeType.REPLACE;
		default:
			throw new IllegalArgumentException("Unknown change type '" + type
					+ "'.");
		}
	}

	@Override
	public List<PropertyAvailableValues> getPropertyAvailableValues(String oid,
			List<String> properties) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Collection<UserDto> list() throws WebModelException {
		try { // Call Web Service Operation
				// TODO: more reasonable handling of paging info
			PagingType paging = new PagingType();
			ObjectListType result = model.listObjects(
					Utils.getObjectType("UserType"), paging);

			List<ObjectType> users = result.getObject();
			List<UserDto> guiUsers = new ArrayList<UserDto>();
			for (ObjectType userType : users) {
				ObjectStage stage = new ObjectStage();
				stage.setObject((UserType) userType);
				UserDto userDto = (UserDto) constructUserType.newInstance();
				userDto.setStage(stage);
				guiUsers.add(userDto);
			}

			return guiUsers;
		} catch (FaultMessage ex) {
			throw new WebModelException(ex.getFaultInfo().getMessage(),
					"[Web Service Error] list user failed");
		} catch (InstantiationException ex) {

			throw new WebModelException(ex.getMessage(), "Instatiation failed.");
		} catch (IllegalAccessException ex) {

			throw new WebModelException(ex.getMessage(),
					"Class or its nullary constructor is not accessible.");
		}

	}

	@Override
	public UserDto get(String oid, PropertyReferenceListType resolve)
			throws WebModelException {
		TRACE.info("oid = {}", new Object[] { oid });
		Validate.notNull(oid);

		try { // Call Web Service Operation
			ObjectContainerType result = model.getObject(oid, resolve);
			// ObjectStage stage = new ObjectStage();
			// stage.setObject((UserType) result.getObject());

			UserDto userDto = (UserDto) constructUserType.newInstance();
			userDto.setXmlObject((UserType) result.getObject());
			// userDto.setStage(stage);
			return userDto;
		} catch (FaultMessage ex) {
			TRACE.error("User lookup for oid = {}", oid);
			TRACE.error("Exception was: ", ex);
			throw new WebModelException(ex.getFaultInfo().getMessage(),
					"Failed to get user with oid " + oid, ex);
		} catch (IllegalAccessException ex) {
			TRACE.error(
					"Class or its nullary constructor is not accessible: {}",
					ex);
			throw new WebModelException(
					"Class or its nullary constructor is not accessible",
					"Internal Error", ex);
		} catch (InstantiationException ex) {
			TRACE.error("Instantiation failed: {}", ex);
			throw new WebModelException("Instantiation failed",
					"Internal Error", ex);
		} catch (RuntimeException ex) {
			// We want to catch also runtime exceptions here. These are severe
			// internal errors (bugs) or system errors (out of memory). But
			// we want at least to let user know that something bad happened
			// here
			TRACE.error("Runtime exception: {}", ex);
			throw new WebModelException(ex.getMessage(), "Internal Error", ex);
		}

	}

	@Override
	public AccountShadowDto addAccount(UserDto userDto, String resourceOid)
			throws WebModelException {
		AccountShadowDto accountShadowDto = new AccountShadowDto();
		AccountShadowType accountShadowType = new AccountShadowType();
		accountShadowType.setAttributes(new Attributes());
		// TODO: workaround, till we switch to staging
		// ResourceTypeManager rtm = new
		// ResourceTypeManager(GuiResourceDto.class);
		ResourceTypeManager rtm = (ResourceTypeManager) objectTypeCatalog
				.getObjectManager(ResourceDto.class, GuiResourceDto.class);
		ResourceDto resourceDto;
		try {
			resourceDto = rtm.get(resourceOid, new PropertyReferenceListType());
		} catch (Exception ex) {
			throw new WebModelException(ex.getMessage(),
					"User - add account failed.");
		}
		accountShadowType
				.setResource((ResourceType) resourceDto.getXmlObject());

		accountShadowDto.setXmlObject(accountShadowType);
		// TODO: account is set to user not here, but in method where we are
		// going to persist it from GUI,
		// because actual account is retrivede from form generator
		// userDto.getAccount().add(accountShadowDto);

		return accountShadowDto;
	}

	@Override
	public UserDto create() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Collection<UserDto> list(PagingDto pagingDto)
			throws WebModelException {
		try { // Call Web Service Operation
				// TODO: more reasonable handling of paging info
			PagingType paging = new PagingType();

			PropertyReferenceType propertyReferenceType = Utils
					.fillPropertyReference(pagingDto.getOrderBy());
			paging.setOrderBy(propertyReferenceType);
			paging.setOffset(BigInteger.valueOf(pagingDto.getOffset()));
			paging.setMaxSize(BigInteger.valueOf(pagingDto.getMaxSize()));
			paging.setOrderDirection(paging.getOrderDirection());
			ObjectListType result = model.listObjects(
					Utils.getObjectType("UserType"), paging);

			List<ObjectType> users = result.getObject();
			List<UserDto> guiUsers = new ArrayList<UserDto>();
			for (ObjectType userType : users) {
				ObjectStage stage = new ObjectStage();
				stage.setObject((UserType) userType);
				UserDto userDto = (UserDto) constructUserType.newInstance();
				userDto.setStage(stage);
				guiUsers.add(userDto);
			}

			return guiUsers;
		} catch (FaultMessage ex) {

			throw new WebModelException(ex.getMessage(),
					"[Web Service Error] list user failed");
		} catch (InstantiationException ex) {

			throw new WebModelException(ex.getMessage(), "Instatiation failed.");
		} catch (IllegalAccessException ex) {

			throw new WebModelException(ex.getMessage(),
					"Class or its nullary constructor is not accessible.");
		}

	}
}
