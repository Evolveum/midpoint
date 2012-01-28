/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.common;


import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Entity
@Table(name = "user")
public class RUserType extends RExtensibleObjectType {

    private String fullName;
    private String givenName;
    private String familyName;
    private Set<String> additionalNames;
    private String honorificPrefix;
    private String honorificSuffix;
    private Set<String> eMailAddress;
    private Set<String> telephoneNumber;
    private String employeeNumber;
    private Set<String> employeeType;
    private Set<String> organizationalUnit;
    private String locality;
    private RCredentialsType credentials;
    private RActivationType activation;
//    private Set<AssignmentType> assignment;
//    private Set<AccountShadowType> account;
//    private Set<ObjectReferenceType> accountRef;
//
//    //todo mapping
//    public Set<AccountShadowType> getAccount() {
//        return account;
//    }
//
//    //todo mapping
//    public Set<AssignmentType> getAssignment() {
//        return assignment;
//    }
//
//    //todo mapping
//    public Set<ObjectReferenceType> getAccountRef() {
//        return accountRef;
//    }

    @Embedded
    public RCredentialsType getCredentials() {
        return credentials;
    }
//
//    public void setAccount(Set<AccountShadowType> account) {
//        this.account = account;
//    }
//
//    public void setAccountRef(Set<ObjectReferenceType> accountRef) {
//        this.accountRef = accountRef;
//    }
//
//    public void setAssignment(Set<AssignmentType> assignment) {
//        this.assignment = assignment;
//    }

    public void setCredentials(RCredentialsType credentials) {
        this.credentials = credentials;
    }

    @Embedded
    public RActivationType getActivation() {
        return activation;
    }

    public void setActivation(RActivationType activation) {
        this.activation = activation;
    }

    @ElementCollection//(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_additional_name", joinColumns =
            {@JoinColumn(name = "userId")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getAdditionalNames() {
        return additionalNames;
    }

    public void setAdditionalNames(Set<String> additionalNames) {
        this.additionalNames = additionalNames;
    }

    @ElementCollection//(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_email_address", joinColumns =
            {@JoinColumn(name = "userId")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getEMailAddress() {
        return eMailAddress;
    }

    public void setEMailAddress(Set<String> eMailAddress) {
        this.eMailAddress = eMailAddress;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    @ElementCollection//(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_employee_type", joinColumns =
            {@JoinColumn(name = "userId")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getEmployeeType() {
        return employeeType;
    }

    public void setEmployeeType(Set<String> employeeType) {
        this.employeeType = employeeType;
    }

    @Index(name = "iFamilyName")
    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    @Index(name = "iFullName")
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Index(name = "iGivenName")
    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getHonorificPrefix() {
        return honorificPrefix;
    }

    public void setHonorificPrefix(String honorificPrefix) {
        this.honorificPrefix = honorificPrefix;
    }

    public String getHonorificSuffix() {
        return honorificSuffix;
    }

    public void setHonorificSuffix(String honorificSuffix) {
        this.honorificSuffix = honorificSuffix;
    }

    @Index(name = "iLocality")
    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    @ElementCollection//(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_organizational_unit", joinColumns =
            {@JoinColumn(name = "userId")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getOrganizationalUnit() {
        return organizationalUnit;
    }

    public void setOrganizationalUnit(Set<String> organizationalUnit) {
        this.organizationalUnit = organizationalUnit;
    }

    @ElementCollection//(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_telephone_number", joinColumns =
            {@JoinColumn(name = "userId")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getTelephoneNumber() {
        return telephoneNumber;
    }

    public void setTelephoneNumber(Set<String> telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    public static void copyFromJAXB(UserType jaxb, RUserType repo) throws DtoTranslationException {
        RExtensibleObjectType.copyFromJAXB(jaxb, repo);

        repo.setFullName(jaxb.getFullName());
        repo.setGivenName(jaxb.getGivenName());
        repo.setFamilyName(jaxb.getFamilyName());
        repo.setHonorificPrefix(jaxb.getHonorificPrefix());
        repo.setHonorificSuffix(jaxb.getHonorificSuffix());
        repo.setEmployeeNumber(jaxb.getEmployeeNumber());
        repo.setLocality(jaxb.getLocality());

        RActivationType activation = new RActivationType();
        if (jaxb.getActivation() != null) {
            RActivationType.copyFromJAXB(jaxb.getActivation(), activation);
        }
        repo.setActivation(activation);
        RCredentialsType credentials = new RCredentialsType();
        if (jaxb.getCredentials() != null) {
            RCredentialsType.copyFromJAXB(jaxb.getCredentials(), credentials);
        }
        repo.setCredentials(credentials);

        //sets
        List<String> strings = jaxb.getAdditionalNames();
        if (strings != null && !strings.isEmpty()) {
            repo.setAdditionalNames(new HashSet<String>(strings));
        }
        strings = jaxb.getEMailAddress();
        if (strings != null && !strings.isEmpty()) {
            repo.setEMailAddress(new HashSet<String>(strings));
        }
        strings = jaxb.getEmployeeType();
        if (strings != null && !strings.isEmpty()) {
            repo.setEmployeeType(new HashSet<String>(strings));
        }
        strings = jaxb.getOrganizationalUnit();
        if (strings != null && !strings.isEmpty()) {
            repo.setOrganizationalUnit(new HashSet<String>(strings));
        }
        strings = jaxb.getTelephoneNumber();
        if (strings != null && !strings.isEmpty()) {
            repo.setTelephoneNumber(new HashSet<String>(strings));
        }

        //todo implement
    }

    public static void copyToJAXB(RUserType repo, UserType jaxb) throws DtoTranslationException {
        RExtensibleObjectType.copyToJAXB(repo, jaxb);

        jaxb.setFullName(repo.getFullName());
        jaxb.setGivenName(repo.getGivenName());
        jaxb.setFamilyName(repo.getFamilyName());
        jaxb.setHonorificPrefix(repo.getHonorificPrefix());
        jaxb.setHonorificSuffix(repo.getHonorificSuffix());
        jaxb.setEmployeeNumber(repo.getEmployeeNumber());
        jaxb.setLocality(repo.getLocality());

        if (repo.getActivation() != null) {
            ActivationType activation = new ActivationType();
            RActivationType.copyToJAXB(repo.getActivation(), activation);
            jaxb.setActivation(activation);
        }

        if (repo.getCredentials() != null) {
            CredentialsType credentials = new CredentialsType();
            RCredentialsType.copyToJAXB(repo.getCredentials(), credentials);
            jaxb.setCredentials(credentials);
        }

        Set<String> strings = repo.getAdditionalNames();
        if (strings != null && !strings.isEmpty()) {
            jaxb.getAdditionalNames().addAll(strings);
        }
        strings = repo.getEMailAddress();
        if (strings != null && !strings.isEmpty()) {
            jaxb.getEMailAddress().addAll(strings);
        }
        strings = repo.getEmployeeType();
        if (strings != null && !strings.isEmpty()) {
            jaxb.getEmployeeType().addAll(strings);
        }
        strings = repo.getOrganizationalUnit();
        if (strings != null && !strings.isEmpty()) {
            jaxb.getOrganizationalUnit().addAll(strings);
        }
        strings = repo.getTelephoneNumber();
        if (strings != null && !strings.isEmpty()) {
            jaxb.getTelephoneNumber().addAll(strings);
        }

        //todo implement
    }

    @Override
    public UserType toJAXB() throws DtoTranslationException {
        UserType object = new UserType();
        RUserType.copyToJAXB(this, object);
        return object;
    }
}

