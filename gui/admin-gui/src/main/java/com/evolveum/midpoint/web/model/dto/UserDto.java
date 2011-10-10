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

package com.evolveum.midpoint.web.model.dto;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.bean.AssignmentBean;
import com.evolveum.midpoint.web.controller.util.ContainsAssignment;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author semancik
 */
public class UserDto extends ExtensibleObjectDto<UserType> implements ContainsAssignment {

	private static final long serialVersionUID = 2178456879571587946L;
	private List<AccountShadowDto> accountDtos;
	private List<AssignmentBean> assignments;

	public UserDto() {
	}

	public UserDto(UserType user) {
		super(user);
		if (user != null) {
			createAssignments(user);
		}
	}

	public String getFullName() {
		return getXmlObject().getFullName();
	}

	public void setFullName(String value) {
		getXmlObject().setFullName(value);
	}

	public String getGivenName() {
		return getXmlObject().getGivenName();
	}

	public void setGivenName(String value) {
		getXmlObject().setGivenName(value);
	}

	public String getFamilyName() {
		return getXmlObject().getFamilyName();
	}

	public void setFamilyName(String value) {
		getXmlObject().setFamilyName(value);
	}

	public void setEmail(String email) {
		List<String> list = getXmlObject().getEMailAddress();
		list.clear();
		list.add(email);
	}

	public String getEmail() {
		List<String> list = getXmlObject().getEMailAddress();
		if (list.size() == 0) {
			return null;
		}
		return list.get(0);
	}

	public String getHonorificPrefix() {
		return getXmlObject().getHonorificPrefix();
	}

	public void setHonorificPrefix(String value) {
		getXmlObject().setHonorificPrefix(value);
	}

	public String getHonorificSuffix() {
		return getXmlObject().getHonorificSuffix();
	}

	public void setHonorificSuffix(String value) {
		getXmlObject().setHonorificSuffix(value);
	}

	public List<AccountShadowDto> getAccount() {

		// List<AccountShadowType> accounts = getXmlObject().getAccount();
		if (accountDtos == null) {
			accountDtos = new ArrayList<AccountShadowDto>();
			for (AccountShadowType account : getXmlObject().getAccount()) {
				accountDtos.add(new AccountShadowDto(account));
			}
		}

		return accountDtos;
	}

	public List<ObjectReferenceDto> getAccountRef() {
		List<ObjectReferenceType> accountRefs = getXmlObject().getAccountRef();
		List<ObjectReferenceDto> accountRefDtos = new ArrayList<ObjectReferenceDto>();

		for (ObjectReferenceType ref : accountRefs) {
			accountRefDtos.add(new ObjectReferenceDto(ref));
		}

		return accountRefDtos;
	}

	public String getEmployeeNumber() {
		return getXmlObject().getEmployeeNumber();
	}

	public void setEmployeeNumber(String value) {
		getXmlObject().setEmployeeNumber(value);
	}

	public String getLocality() {
		return getXmlObject().getLocality();
	}

	public void setLocality(String value) {
		getXmlObject().setLocality(value);
	}

	@Override
	public void setXmlObject(UserType xmlObject) {
		super.setXmlObject(xmlObject);
		if (xmlObject != null) {
			createAssignments(xmlObject);
		}
	}

	@Override
	public List<AssignmentBean> getAssignments() {
		if (assignments == null) {
			assignments = new ArrayList<AssignmentBean>();
		}

		return assignments;
	}

	@Override
	public void normalizeAssignments() {
		List<AssignmentType> assignmentTypes = getXmlObject().getAssignment();
		assignmentTypes.clear();
		for (AssignmentBean bean : getAssignments()) {
			assignmentTypes.add(bean.getAssignment());
		}
	}

	private void createAssignments(UserType user) {
		getAssignments().clear();
		int id = 0;
		for (AssignmentType assignment : user.getAssignment()) {
			getAssignments().add(new AssignmentBean(id, assignment));
			id++;
		}
	}
}
