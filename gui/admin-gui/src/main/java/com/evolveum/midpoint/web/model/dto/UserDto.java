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

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.web.bean.AssignmentBean;
import com.evolveum.midpoint.web.controller.util.ContainsAssignment;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author semancik
 */
public class UserDto extends ObjectDto<UserType> implements ContainsAssignment {

    private static final long serialVersionUID = 2178456879571587946L;
    private List<AccountShadowDto> accountDtos;
    private List<AssignmentBean> assignments;
    @Deprecated
    private ModelService model;

    public UserDto() {
    }

    @Deprecated
    public UserDto(UserType user, ModelService model) {
        super(user);
        this.model = model;
        if (user != null) {
            createAssignments(user);
        }
    }

    public String getFullName() {
        PolyStringType poly = getXmlObject().getFullName();
        return poly != null ? poly.getOrig() : null;
    }

    public void setFullName(String value) {
        PolyStringType poly = getXmlObject().getFullName();
        if (poly == null) {
            poly = new PolyStringType();
            getXmlObject().setFullName(poly);
        }
        poly.setOrig(value);
    }

    public String getGivenName() {
        PolyStringType poly = getXmlObject().getGivenName();
        return poly != null ? poly.getOrig() : null;
    }

    public void setGivenName(String value) {
        PolyStringType poly = getXmlObject().getGivenName();
        if (poly == null) {
            poly = new PolyStringType();
            getXmlObject().setGivenName(poly);
        }
        poly.setOrig(value);
    }

    public String getFamilyName() {
        PolyStringType poly = getXmlObject().getFamilyName();
        return poly != null ? poly.getOrig() : null;
    }

    public void setFamilyName(String value) {
        PolyStringType poly = getXmlObject().getFamilyName();
        if (poly == null) {
            poly = new PolyStringType();
            getXmlObject().setFamilyName(poly);
        }
        poly.setOrig(value);
    }

    public void setEmail(String email) {
        getXmlObject().setEmailAddress(email);
    }

    public String getEmail() {
        return getXmlObject().getEmailAddress();
    }

    public String getHonorificPrefix() {
        PolyStringType poly = getXmlObject().getHonorificPrefix();
        return poly != null ? poly.getOrig() : null;
    }

    public void setHonorificPrefix(String value) {
        PolyStringType poly = getXmlObject().getHonorificPrefix();
        if (poly == null) {
            poly = new PolyStringType();
            getXmlObject().setHonorificPrefix(poly);
        }
        poly.setOrig(value);
    }

    public String getHonorificSuffix() {
        PolyStringType poly = getXmlObject().getHonorificSuffix();
        return poly != null ? poly.getOrig() : null;
    }

    public void setHonorificSuffix(String value) {
        PolyStringType poly = getXmlObject().getHonorificSuffix();
        if (poly == null) {
            poly = new PolyStringType();
            getXmlObject().setHonorificSuffix(poly);
        }
        poly.setOrig(value);
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
        PolyStringType poly = getXmlObject().getLocality();
        return poly != null ? poly.getOrig() : null;
    }

    public void setLocality(String value) {
        PolyStringType poly = getXmlObject().getLocality();
        if (poly == null) {
            poly = new PolyStringType();
            getXmlObject().setLocality(poly);
        }
        poly.setOrig(value);
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
            getAssignments().add(new AssignmentBean(id, assignment, model));
            id++;
        }
    }
}
