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


import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "user")
public class RUserType extends RObjectType {

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
    private Set<RAssignmentType> assignment;
    private Set<RObjectReferenceType> accountRef;

    @OneToMany
    @JoinTable(name = "user_assignment", joinColumns = @JoinColumn(name = "userOid"),
            inverseJoinColumns = {@JoinColumn(name = "owner"), @JoinColumn(name = "assignmentId")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAssignmentType> getAssignment() {
        return assignment;
    }

    @OneToMany
    @JoinTable(name = "user_account_ref", joinColumns = @JoinColumn(name = "userOid"),
            inverseJoinColumns = {@JoinColumn(name = "owner"), @JoinColumn(name = "containerId"), @JoinColumn(name = "target")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReferenceType> getAccountRef() {
        return accountRef;
    }

    @ElementCollection
    @CollectionTable(name = "user_email_address", joinColumns =
            {@JoinColumn(name = "userOid")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getEMailAddress() {
        return eMailAddress;
    }

    @ElementCollection
    @CollectionTable(name = "user_employee_type", joinColumns =
            {@JoinColumn(name = "userOid")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getEmployeeType() {
        return employeeType;
    }

    @Index(name = "iFamilyName")
    public String getFamilyName() {
        return familyName;
    }

    @Index(name = "iFullName")
    public String getFullName() {
        return fullName;
    }

    @Index(name = "iGivenName")
    public String getGivenName() {
        return givenName;
    }

    @Index(name = "iLocality")
    public String getLocality() {
        return locality;
    }

    @ElementCollection
    @CollectionTable(name = "user_organizational_unit", joinColumns =
            {@JoinColumn(name = "userOid")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getOrganizationalUnit() {
        return organizationalUnit;
    }

    @ElementCollection
    @CollectionTable(name = "user_telephone_number", joinColumns =
            {@JoinColumn(name = "userOid")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getTelephoneNumber() {
        return telephoneNumber;
    }

    @Embedded
    public RCredentialsType getCredentials() {
        return credentials;
    }

    @Embedded
    public RActivationType getActivation() {
        return activation;
    }

    @ElementCollection
    @CollectionTable(name = "user_additional_name", joinColumns =
            {@JoinColumn(name = "userOid")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getAdditionalNames() {
        return additionalNames;
    }

    public void setAccountRef(Set<RObjectReferenceType> accountRef) {
        this.accountRef = accountRef;
    }

    public void setAssignment(Set<RAssignmentType> assignment) {
        this.assignment = assignment;
    }

    public void setCredentials(RCredentialsType credentials) {
        this.credentials = credentials;
    }

    public void setActivation(RActivationType activation) {
        this.activation = activation;
    }

    public void setAdditionalNames(Set<String> additionalNames) {
        this.additionalNames = additionalNames;
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

    public void setEmployeeType(Set<String> employeeType) {
        this.employeeType = employeeType;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public void setOrganizationalUnit(Set<String> organizationalUnit) {
        this.organizationalUnit = organizationalUnit;
    }

    public void setTelephoneNumber(Set<String> telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    public static void copyFromJAXB(UserType jaxb, RUserType repo, PrismContext prismContext) throws
            DtoTranslationException {
        RObjectType.copyFromJAXB(jaxb, repo, prismContext);

        repo.setFullName(jaxb.getFullName());
        repo.setGivenName(jaxb.getGivenName());
        repo.setFamilyName(jaxb.getFamilyName());
        repo.setHonorificPrefix(jaxb.getHonorificPrefix());
        repo.setHonorificSuffix(jaxb.getHonorificSuffix());
        repo.setEmployeeNumber(jaxb.getEmployeeNumber());
        repo.setLocality(jaxb.getLocality());

        RActivationType activation = new RActivationType();
        if (jaxb.getActivation() != null) {
            RActivationType.copyFromJAXB(jaxb.getActivation(), activation, prismContext);
        }
        repo.setActivation(activation);
        RCredentialsType credentials = new RCredentialsType();
        if (jaxb.getCredentials() != null) {
            RCredentialsType.copyFromJAXB(jaxb.getCredentials(), credentials, prismContext);
        }
        repo.setCredentials(credentials);

        //sets
        repo.setAdditionalNames(RUtil.listToSet(jaxb.getAdditionalNames()));
        repo.setEMailAddress(RUtil.listToSet(jaxb.getEmailAddress()));
        repo.setEmployeeType(RUtil.listToSet(jaxb.getEmployeeType()));
        repo.setOrganizationalUnit(RUtil.listToSet(jaxb.getOrganizationalUnit()));
        repo.setTelephoneNumber(RUtil.listToSet(jaxb.getTelephoneNumber()));

        if (jaxb.getAccountRef() != null && !jaxb.getAccountRef().isEmpty()) {
            repo.setAccountRef(new HashSet<RObjectReferenceType>());
        }
        for (ObjectReferenceType accountRef : jaxb.getAccountRef()) {
            repo.getAccountRef().add(RUtil.jaxbRefToRepo(accountRef, jaxb, prismContext));
        }

        for (AssignmentType assignment : jaxb.getAssignment()) {
            RAssignmentType rAssignment = new RAssignmentType();
            RAssignmentType.copyFromJAXB(assignment, rAssignment, prismContext);

            repo.getAssignment().add(rAssignment);
        }
    }

    public static void copyToJAXB(RUserType repo, UserType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObjectType.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setFullName(repo.getFullName());
        jaxb.setGivenName(repo.getGivenName());
        jaxb.setFamilyName(repo.getFamilyName());
        jaxb.setHonorificPrefix(repo.getHonorificPrefix());
        jaxb.setHonorificSuffix(repo.getHonorificSuffix());
        jaxb.setEmployeeNumber(repo.getEmployeeNumber());
        jaxb.setLocality(repo.getLocality());

        if (repo.getActivation() != null) {
            ActivationType activation = new ActivationType();
            RActivationType.copyToJAXB(repo.getActivation(), activation, prismContext);
            jaxb.setActivation(activation);
        }

        if (repo.getCredentials() != null) {
            CredentialsType credentials = new CredentialsType();
            RCredentialsType.copyToJAXB(repo.getCredentials(), credentials, prismContext);
            jaxb.setCredentials(credentials);
        }

        jaxb.getAdditionalNames().addAll(RUtil.safeSetToList(repo.getAdditionalNames()));
        jaxb.getEmailAddress().addAll(RUtil.safeSetToList(repo.getEMailAddress()));
        jaxb.getEmployeeType().addAll(RUtil.safeSetToList(repo.getEmployeeType()));
        jaxb.getTelephoneNumber().addAll(RUtil.safeSetToList(repo.getTelephoneNumber()));
        jaxb.getOrganizationalUnit().addAll(RUtil.safeSetToList(repo.getOrganizationalUnit()));

        if (repo.getAccountRef() != null) {
            for (RObjectReferenceType repoRef : repo.getAccountRef()) {
                jaxb.getAccountRef().add(repoRef.toJAXB(prismContext));
            }
        }

        if (repo.getAssignment() != null) {
            for (RAssignmentType rAssignment : repo.getAssignment()) {
                jaxb.getAssignment().add(rAssignment.toJAXB(prismContext));
            }
        }
    }

    @Override
    public UserType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        UserType object = new UserType();
        RUserType.copyToJAXB(this, object, prismContext);
        RUtil.revive(object.asPrismObject(), UserType.class, prismContext);
        return object;
    }
}

