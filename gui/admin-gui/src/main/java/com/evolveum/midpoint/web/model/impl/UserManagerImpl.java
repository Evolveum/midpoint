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
package com.evolveum.midpoint.web.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.UserManager;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;
import com.evolveum.midpoint.web.model.dto.GuiResourceDto;
import com.evolveum.midpoint.web.model.dto.GuiUserDto;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.web.model.dto.UserDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType.Attributes;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
public class UserManagerImpl extends ObjectManagerImpl<UserType, GuiUserDto> implements UserManager {

	private static final Trace LOGGER = TraceManager.getTrace(UserManagerImpl.class);
	private static final long serialVersionUID = -3457278299468312767L;
	@Autowired(required = true)
	private transient ObjectTypeCatalog objectTypeCatalog;
	@Autowired(required = true)
	private transient Protector protector;

	@Override
	protected Class<? extends ObjectType> getSupportedObjectClass() {
		return UserType.class;
	}

	@Override
	protected GuiUserDto createObject(UserType objectType) {
		return new GuiUserDto(objectType);
	}

	@Override
	public Collection<GuiUserDto> list(PagingType paging) {
		return list(paging, ObjectTypes.USER);
	}

	@Override
	public Set<PropertyChange> submit(GuiUserDto changedObject) {
		Validate.notNull(changedObject, "User object must not be null.");

		Set<PropertyChange> set = null;
		UserDto oldUser = get(changedObject.getOid(), Utils.getResolveResourceList());

		OperationResult result = new OperationResult(UserManager.SUBMIT);
		try { // Call Web Service Operation
			changedObject.encryptCredentials(protector);

			ObjectModificationType changes = CalculateXmlDiff.calculateChanges(oldUser.getXmlObject(),
					changedObject.getXmlObject());
			if (changes != null && changes.getOid() != null && changes.getPropertyModification().size() > 0) {
				getModel().modifyObject(UserType.class, changes, result);
			}

			if (null != changes) {
				set = new HashSet<PropertyChange>();
				// TODO: finish this
				List<PropertyModificationType> modifications = changes.getPropertyModification();
				for (PropertyModificationType modification : modifications) {
					Set<Object> values = new HashSet<Object>();
					if (modification.getValue() != null) {
						values.addAll(modification.getValue().getAny());
					}
					set.add(new PropertyChange(createQName(modification.getPath()),
							getChangeType(modification.getModificationType()), values));
				}
			}
			result.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't submit user {}", ex,
					new Object[] { changedObject.getName() });
			result.recordFatalError("Couldn't submit user '" + changedObject.getName() + "'.", ex);
		}

		result.computeStatus("Couldn't submit user '" + changedObject.getName() + "'.");
		printResults(LOGGER, result);

		return set;
	}

	@Override
	public String add(GuiUserDto object) {
		Validate.notNull(object);
		try {
			object.encryptCredentials(protector);
		} catch (EncryptionException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't encrypt credentials for user {}", ex,
					object.getName());
		}
		return super.add(object);
	}

	@Override
	public AccountShadowDto addAccount(UserDto userDto, String resourceOid) {
		AccountShadowDto accountShadowDto = new AccountShadowDto();
		AccountShadowType accountShadowType = new AccountShadowType();
		accountShadowType.setAttributes(new Attributes());

		ResourceManager manager = (ResourceManager) objectTypeCatalog.getObjectManager(ResourceType.class,
				GuiResourceDto.class);
		ResourceDto resourceDto = null;
		try {
			resourceDto = manager.get(resourceOid, new PropertyReferenceListType());
		} catch (Exception ex) {
			FacesUtils.addErrorMessage("User add account failed, reason: " + ex.getMessage(), ex);
			return null;
		}
		accountShadowType.setResource((ResourceType) resourceDto.getXmlObject());
		accountShadowDto.setXmlObject(accountShadowType);

		// TODO: account is set to user not here, but in method where we are
		// going to persist it from GUI, because actual account is retrieved
		// from form generator userDto.getAccount().add(accountShadowDto);

		return accountShadowDto;
	}

	@Override
	public List<UserDto> search(QueryType search, PagingType paging) {
		Validate.notNull(search, "Query must not be null.");

		OperationResult result = new OperationResult(SEARCH);
		List<UserDto> users = new ArrayList<UserDto>();
		try {
			List<UserType> list = getModel().searchObjects(UserType.class, search, paging, result);
			for (UserType user : list) {
				UserDto userDto = createObject(user);
				users.add(userDto);
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't search users", ex);
			FacesUtils.addErrorMessage("Couldn't search users.", ex);
		}

		return users;
	}

	private QName createQName(Element element) {
		String namespace = element.getNamespaceURI();
		if (namespace == null) {
			namespace = element.getBaseURI();
		}
		return new QName(namespace, element.getLocalName(), element.getPrefix());
	}

	private PropertyChange.ChangeType getChangeType(PropertyModificationTypeType type) {
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
				throw new IllegalArgumentException("Unknown change type '" + type + "'.");
		}
	}
}
