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
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
import com.evolveum.midpoint.repo.sql.query.QueryEntity;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
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

    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReference> getAccountRefs() {
        if (accountRefs == null) {
            accountRefs = new HashSet<RObjectReference>();
        }
        return accountRefs;
    }

    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAssignment> getAssignments() {
        if (assignments == null) {
            assignments = new HashSet<RAssignment>();
        }
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

    @Override
    public boolean equals(Object o) {
        if (1 == 1) return true;


        TraceManager.getTrace(SqlRepositoryServiceImpl.class).info("@@@equals {} \n{}\n{}",
                new Object[]{getClass().getSimpleName(), ReflectionToStringBuilder.toString(this),
                        ReflectionToStringBuilder.toString(o)});
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RUser rUser = (RUser) o;

        if (activation != null ? !activation.equals(rUser.activation) : rUser.activation != null) return false;
        if (additionalNames != null ? !additionalNames.equals(rUser.additionalNames) : rUser.additionalNames != null)
            return false;
        if (credentials != null ? !credentials.equals(rUser.credentials) : rUser.credentials != null) return false;
        if (emailAddress != null ? !emailAddress.equals(rUser.emailAddress) : rUser.emailAddress != null) return false;
        if (employeeNumber != null ? !employeeNumber.equals(rUser.employeeNumber) : rUser.employeeNumber != null)
            return false;
        if (employeeType != null ? !employeeType.equals(rUser.employeeType) : rUser.employeeType != null) return false;
        if (familyName != null ? !familyName.equals(rUser.familyName) : rUser.familyName != null) return false;
        if (fullName != null ? !fullName.equals(rUser.fullName) : rUser.fullName != null) return false;
        if (givenName != null ? !givenName.equals(rUser.givenName) : rUser.givenName != null) return false;
        if (honorificPrefix != null ? !honorificPrefix.equals(rUser.honorificPrefix) : rUser.honorificPrefix != null)
            return false;
        if (honorificSuffix != null ? !honorificSuffix.equals(rUser.honorificSuffix) : rUser.honorificSuffix != null)
            return false;
        if (locality != null ? !locality.equals(rUser.locality) : rUser.locality != null) return false;
        if (organizationalUnit != null ? !organizationalUnit.equals(rUser.organizationalUnit) : rUser.organizationalUnit != null)
            return false;
        if (telephoneNumber != null ? !telephoneNumber.equals(rUser.telephoneNumber) : rUser.telephoneNumber != null)
            return false;

        //        if (assignments != null ? !assignments.equals(rUser.assignments) : rUser.assignments != null) return false;
        //        if (accountRefs != null ? !accountRefs.equals(rUser.accountRefs) : rUser.accountRefs != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        if (1 == 1) return 37;

        TraceManager.getTrace(SqlRepositoryServiceImpl.class).info("@@@hashCode {} ", getClass().getSimpleName());
        int result = super.hashCode();
        result = 31 * result + (fullName != null ? fullName.hashCode() : 0);
        result = 31 * result + (givenName != null ? givenName.hashCode() : 0);
        result = 31 * result + (familyName != null ? familyName.hashCode() : 0);
        result = 31 * result + (honorificPrefix != null ? honorificPrefix.hashCode() : 0);
        result = 31 * result + (honorificSuffix != null ? honorificSuffix.hashCode() : 0);
        result = 31 * result + (employeeNumber != null ? employeeNumber.hashCode() : 0);
        result = 31 * result + (locality != null ? locality.hashCode() : 0);
        result = 31 * result + (credentials != null ? credentials.hashCode() : 0);
        result = 31 * result + (activation != null ? activation.hashCode() : 0);
        return result;
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

        for (ObjectReferenceType accountRef : jaxb.getAccountRef()) {
            RObjectReference ref = RUtil.jaxbRefToRepo(accountRef, repo, prismContext);
            if (ref != null) {
                repo.getAccountRefs().add(ref);
            }
        }

        for (AssignmentType assignment : jaxb.getAssignment()) {
            RAssignment rAssignment = new RAssignment();
            rAssignment.setOwner(repo);

            rAssignment.setOid(repo.getOid());
            rAssignment.setId(RUtil.getLongFromString(assignment.getId()));

            RAssignment.copyFromJAXB(assignment, rAssignment, jaxb, prismContext);

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

        for (RObjectReference repoRef : repo.getAccountRefs()) {
            jaxb.getAccountRef().add(repoRef.toJAXB(prismContext));
        }

        for (RAssignment rAssignment : repo.getAssignments()) {
            jaxb.getAssignment().add(rAssignment.toJAXB(prismContext));
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
