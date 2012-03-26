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
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
import com.evolveum.midpoint.repo.sql.query.QueryEntity;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@Table(name = "user")
@ForeignKey(name = "fk_user")
public class RUser extends RObject {

    @QueryAttribute
    private String fullName;
    @QueryAttribute
    private String givenName;
    @QueryAttribute
    private String familyName;
    private Set<String> additionalNames;
    @QueryAttribute
    private String honorificPrefix;
    @QueryAttribute
    private String honorificSuffix;
    private Set<String> emailAddress;
    private Set<String> telephoneNumber;
    @QueryAttribute
    private String employeeNumber;
    private Set<String> employeeType;
    private Set<String> organizationalUnit;
    @QueryAttribute
    private String locality;
    @QueryEntity(embedded = true)
    private RCredentials credentials;
    @QueryEntity(embedded = true)
    private RActivation activation;

    private Set<RObjectReference> accountRefs;
    private Set<RAssignment> assignments;

    @OneToMany(mappedBy = "owner")
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReference> getAccountRefs() {
        return accountRefs;
    }

    @OneToMany(mappedBy = "owner")
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAssignment> getAssignments() {
        return assignments;
    }

    @Embedded
    public RActivation getActivation() {
        return activation;
    }

    @ElementCollection
    @ForeignKey(name = "fk_user_additional_name")
    @CollectionTable(name = "user_additional_name", joinColumns = {
            @JoinColumn(name = "user_oid", referencedColumnName = "oid"),
            @JoinColumn(name = "user_id", referencedColumnName = "id")
    })
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getAdditionalNames() {
        return additionalNames;
    }

    @Embedded
    public RCredentials getCredentials() {
        return credentials;
    }

    @ElementCollection
    @ForeignKey(name = "fk_user_email_address")
    @CollectionTable(name = "user_email_address", joinColumns = {
            @JoinColumn(name = "user_oid", referencedColumnName = "oid"),
            @JoinColumn(name = "user_id", referencedColumnName = "id")
    })
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getEmailAddress() {
        return emailAddress;
    }

    @ElementCollection
    @ForeignKey(name = "fk_user_org_unit")
    @CollectionTable(name = "user_organizational_unit", joinColumns = {
            @JoinColumn(name = "user_oid", referencedColumnName = "oid"),
            @JoinColumn(name = "user_id", referencedColumnName = "id")
    })
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getOrganizationalUnit() {
        return organizationalUnit;
    }

    @ElementCollection
    @ForeignKey(name = "fk_user_telephone_number")
    @CollectionTable(name = "user_telephone_number", joinColumns = {
            @JoinColumn(name = "user_oid", referencedColumnName = "oid"),
            @JoinColumn(name = "user_id", referencedColumnName = "id")
    })
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getTelephoneNumber() {
        return telephoneNumber;
    }

    @ElementCollection
    @ForeignKey(name = "fk_user_employee_type")
    @CollectionTable(name = "user_employee_type", joinColumns = {
            @JoinColumn(name = "user_oid", referencedColumnName = "oid"),
            @JoinColumn(name = "user_id", referencedColumnName = "id")
    })
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

    @Index(name = "iEmployeeNumber")
    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getHonorificPrefix() {
        return honorificPrefix;
    }

    public String getHonorificSuffix() {
        return honorificSuffix;
    }

    public void setActivation(RActivation activation) {
        this.activation = activation;
    }

    public void setAdditionalNames(Set<String> additionalNames) {
        this.additionalNames = additionalNames;
    }

    public void setCredentials(RCredentials credentials) {
        this.credentials = credentials;
    }

    public void setEmailAddress(Set<String> emailAddress) {
        this.emailAddress = emailAddress;
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

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public void setHonorificPrefix(String honorificPrefix) {
        this.honorificPrefix = honorificPrefix;
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

    public void setAssignments(Set<RAssignment> assignments) {
        this.assignments = assignments;
    }

    public void setAccountRefs(Set<RObjectReference> accountRefs) {
        this.accountRefs = accountRefs;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public static void copyFromJAXB(UserType jaxb, RUser repo, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setFullName(jaxb.getFullName());
        repo.setGivenName(jaxb.getGivenName());
        repo.setFamilyName(jaxb.getFamilyName());
        repo.setHonorificPrefix(jaxb.getHonorificPrefix());
        repo.setHonorificSuffix(jaxb.getHonorificSuffix());
        repo.setEmployeeNumber(jaxb.getEmployeeNumber());
        repo.setLocality(jaxb.getLocality());

        if (jaxb.getActivation() != null) {
            RActivation activation = new RActivation();
            RActivation.copyFromJAXB(jaxb.getActivation(), activation, prismContext);
            repo.setActivation(activation);

        }
        if (jaxb.getCredentials() != null) {
            RCredentials credentials = new RCredentials();
            RCredentials.copyFromJAXB(jaxb.getCredentials(), credentials, prismContext);
            repo.setCredentials(credentials);
        }

        //sets
        repo.setAdditionalNames(RUtil.listToSet(jaxb.getAdditionalNames()));
        repo.setEmailAddress(RUtil.listToSet(jaxb.getEmailAddress()));
        repo.setEmployeeType(RUtil.listToSet(jaxb.getEmployeeType()));
        repo.setOrganizationalUnit(RUtil.listToSet(jaxb.getOrganizationalUnit()));
        repo.setTelephoneNumber(RUtil.listToSet(jaxb.getTelephoneNumber()));

        if (jaxb.getAccountRef() != null && !jaxb.getAccountRef().isEmpty()) {
            repo.setAccountRefs(new HashSet<RObjectReference>());
        }
        for (ObjectReferenceType accountRef : jaxb.getAccountRef()) {
            RObjectReference ref = RUtil.jaxbRefToRepo(accountRef, repo, prismContext);
            if (ref != null) {
                repo.getAccountRefs().add(ref);
            }
        }

        if (jaxb.getAssignment() != null && !jaxb.getAssignment().isEmpty()) {
            repo.setAssignments(new HashSet<RAssignment>());
        }
        for (AssignmentType assignment : jaxb.getAssignment()) {
            RAssignment rAssignment = new RAssignment();
            rAssignment.setOwner(repo);
            RAssignment.copyFromJAXB(assignment, rAssignment, prismContext);

            repo.getAssignments().add(rAssignment);
        }
    }

    public static void copyToJAXB(RUser repo, UserType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setFullName(repo.getFullName());
        jaxb.setGivenName(repo.getGivenName());
        jaxb.setFamilyName(repo.getFamilyName());
        jaxb.setHonorificPrefix(repo.getHonorificPrefix());
        jaxb.setHonorificSuffix(repo.getHonorificSuffix());
        jaxb.setEmployeeNumber(repo.getEmployeeNumber());
        jaxb.setLocality(repo.getLocality());

        if (repo.getActivation() != null) {
            jaxb.setActivation(repo.getActivation().toJAXB(prismContext));
        }

        if (repo.getCredentials() != null) {
            PropertyPath path = new PropertyPath(UserType.F_CREDENTIALS);
            jaxb.setCredentials(repo.getCredentials().toJAXB(jaxb, path, prismContext));
        }

        jaxb.getAdditionalNames().addAll(RUtil.safeSetToList(repo.getAdditionalNames()));
        jaxb.getEmailAddress().addAll(RUtil.safeSetToList(repo.getEmailAddress()));
        jaxb.getEmployeeType().addAll(RUtil.safeSetToList(repo.getEmployeeType()));
        jaxb.getTelephoneNumber().addAll(RUtil.safeSetToList(repo.getTelephoneNumber()));
        jaxb.getOrganizationalUnit().addAll(RUtil.safeSetToList(repo.getOrganizationalUnit()));

        if (repo.getAccountRefs() != null) {
            for (RObjectReference repoRef : repo.getAccountRefs()) {
                jaxb.getAccountRef().add(repoRef.toJAXB(prismContext));
            }
        }

        if (repo.getAssignments() != null) {
            for (RAssignment rAssignment : repo.getAssignments()) {
                jaxb.getAssignment().add(rAssignment.toJAXB(prismContext));
            }
        }
    }

    @Override
    public UserType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        UserType object = new UserType();
        RUtil.revive(object.asPrismObject(), UserType.class, prismContext);
        RUser.copyToJAXB(this, object, prismContext);

        return object;
    }
}
