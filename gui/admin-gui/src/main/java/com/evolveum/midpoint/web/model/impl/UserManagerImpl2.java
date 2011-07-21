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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.diff.DiffException;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.web.model.UserManager2;
import com.evolveum.midpoint.web.model.WebModelException;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.UserDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
public class UserManagerImpl2 extends ObjectManagerImpl2<UserType, UserDto> implements UserManager2 {

	private static final long serialVersionUID = -3457278299468312767L;

	@Override
	protected Class<? extends ObjectType> getSupportedObjectClass() {
		return UserType.class;
	}

	@Override
	protected UserDto createObject(UserType objectType) {
		return new UserDto(null);
	}

	@Override
	public Collection<UserDto> list(PagingType paging) {
		return list(paging, ObjectTypes.USER);
	}

	@Override
	public Set<PropertyChange> submit(UserDto changedObject) {
		Validate.notNull(changedObject, "User object must not be null.");
		Set<PropertyChange> set = new HashSet<PropertyChange>();
		UserDto oldUser = get(changedObject.getOid(), Utils.getResolveResourceList());

		try { // Call Web Service Operation
			ObjectModificationType changes = CalculateXmlDiff.calculateChanges(oldUser.getXmlObject(),
					changedObject.getXmlObject());
			if (changes != null && changes.getOid() != null && changes.getPropertyModification().size() > 0) {
				getModel().modifyObject(changes, new OperationResult(UserManager2.OPERATION_USER_SUBMIT));
			}

			if (null != changes) {
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
			return set;
		} catch (ObjectNotFoundException ex) {
			// throw new WebModelException(fault.getFaultInfo().getMessage(),
			// "[Web Service Error] Submit user failed.");
		} catch (SystemException ex) {

		} catch (DiffException ex) {
			// throw new WebModelException(ex.getMessage(),
			// "[Diff Error] Submit user failed.");
		} catch (Exception ex) {

		}

		return set;
	}

	@Override
	public AccountShadowDto addAccount(UserDto userDto, String resourceOid) throws WebModelException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UserDto> search(QueryType search, PagingType paging, OperationResult result)
			throws WebModelException {
		// TODO Auto-generated method stub
		return null;
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
