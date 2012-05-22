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
import com.evolveum.midpoint.repo.sql.ContainerIdGenerator;
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
@Table(name = "m_user")
@org.hibernate.annotations.Table(appliesTo = "m_user",
        indexes = {@Index(name = "iUserEnabled", columnNames = "enabled"),
                @Index(name = "iFullName", columnNames = "fullName_norm"),
                @Index(name = "iFamilyName", columnNames = "familyName_norm"),
                @Index(name = "iGivenName", columnNames = "givenName_norm"),
                @Index(name = "iLocality", columnNames = "locality_norm"),
                @Index(name = "iAdditionalName", columnNames = "additionalName_norm"),
                @Index(name = "iHonorificPrefix", columnNames = "honorificPrefix_norm"),
                @Index(name = "iHonorificSuffix", columnNames = "honorificSuffix_norm")})
@ForeignKey(name = "fk_user")
public class RUser extends RObject {

    @QueryAttribute(polyString = true)
    private RPolyString fullName;
    @QueryAttribute
    private RPolyString givenName;
    @QueryAttribute
    private RPolyString familyName;
    @QueryAttribute
    private RPolyString additionalName;
    @QueryAttribute
    private RPolyString honorificPrefix;
    @QueryAttribute
    private RPolyString honorificSuffix;
    @QueryAttribute
    private String emailAddress;
    @QueryAttribute
    private String telephoneNumber;
    @QueryAttribute
    private String employeeNumber;
    private Set<String> employeeType;
    private Set<RPolyString> organizationalUnit;
    @QueryAttribute
    private RPolyString locality;
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

    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "additionalName_orig", nullable = true)),
            @AttributeOverride(name = "norm", column = @Column(name = "additionalName_norm", nullable = true))
    })
    @Embedded
    public RPolyString getAdditionalName() {
        return additionalName;
    }

    @Embedded
    public RCredentials getCredentials() {
        return credentials;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    @ElementCollection
    @ForeignKey(name = "fk_user_org_unit")
    @CollectionTable(name = "m_user_organizational_unit", joinColumns = {
            @JoinColumn(name = "user_oid", referencedColumnName = "oid"),
            @JoinColumn(name = "user_id", referencedColumnName = "id")
    })
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RPolyString> getOrganizationalUnit() {
        return organizationalUnit;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    @ElementCollection
    @ForeignKey(name = "fk_user_employee_type")
    @CollectionTable(name = "m_user_employee_type", joinColumns = {
            @JoinColumn(name = "user_oid", referencedColumnName = "oid"),
            @JoinColumn(name = "user_id", referencedColumnName = "id")
    })
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getEmployeeType() {
        return employeeType;
    }

    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "familyName_orig", nullable = true)),
            @AttributeOverride(name = "norm", column = @Column(name = "familyName_norm", nullable = true))
    })
    @Embedded
    public RPolyString getFamilyName() {
        return familyName;
    }

    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "fullName_orig", nullable = true)),
            @AttributeOverride(name = "norm", column = @Column(name = "fullName_norm", nullable = true))
    })
    @Embedded
    public RPolyString getFullName() {
        return fullName;
    }

    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "givenName_orig", nullable = true)),
            @AttributeOverride(name = "norm", column = @Column(name = "givenName_norm", nullable = true))
    })
    @Embedded
    public RPolyString getGivenName() {
        return givenName;
    }

    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "locality_orig", nullable = true)),
            @AttributeOverride(name = "norm", column = @Column(name = "locality_norm", nullable = true))
    })
    @Embedded
    public RPolyString getLocality() {
        return locality;
    }

    @Index(name = "iEmployeeNumber")
    public String getEmployeeNumber() {
        return employeeNumber;
    }

    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "honorificPrefix_orig", nullable = true)),
            @AttributeOverride(name = "norm", column = @Column(name = "honorificPrefix_norm", nullable = true))
    })
    @Embedded
    public RPolyString getHonorificPrefix() {
        return honorificPrefix;
    }

    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "honorificSuffix_orig", nullable = true)),
            @AttributeOverride(name = "norm", column = @Column(name = "honorificSuffix_norm", nullable = true))
    })
    @Embedded
    public RPolyString getHonorificSuffix() {
        return honorificSuffix;
    }

    public void setActivation(RActivation activation) {
        this.activation = activation;
    }

    public void setAdditionalNames(RPolyString additionalName) {
        this.additionalName = additionalName;
    }

    public void setCredentials(RCredentials credentials) {
        this.credentials = credentials;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public void setEmployeeType(Set<String> employeeType) {
        this.employeeType = employeeType;
    }

    public void setFamilyName(RPolyString familyName) {
        this.familyName = familyName;
    }

    public void setGivenName(RPolyString givenName) {
        this.givenName = givenName;
    }

    public void setHonorificPrefix(RPolyString honorificPrefix) {
        this.honorificPrefix = honorificPrefix;
    }

    public void setHonorificSuffix(RPolyString honorificSuffix) {
        this.honorificSuffix = honorificSuffix;
    }

    public void setLocality(RPolyString locality) {
        this.locality = locality;
    }

    public void setOrganizationalUnit(Set<RPolyString> organizationalUnit) {
        this.organizationalUnit = organizationalUnit;
    }

    public void setTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    public void setAssignments(Set<RAssignment> assignments) {
        this.assignments = assignments;
    }

    public void setAccountRefs(Set<RObjectReference> accountRefs) {
        this.accountRefs = accountRefs;
    }

    public void setFullName(RPolyString fullName) {
        this.fullName = fullName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RUser rUser = (RUser) o;

        if (activation != null ? !activation.equals(rUser.activation) : rUser.activation != null) return false;
        if (additionalName != null ? !additionalName.equals(rUser.additionalName) : rUser.additionalName != null)
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
        if (assignments != null ? !assignments.equals(rUser.assignments) : rUser.assignments != null) return false;
        if (accountRefs != null ? !accountRefs.equals(rUser.accountRefs) : rUser.accountRefs != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
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

        repo.setFullName(RPolyString.copyFromJAXB(jaxb.getFullName()));
        repo.setGivenName(RPolyString.copyFromJAXB(jaxb.getGivenName()));
        repo.setFamilyName(RPolyString.copyFromJAXB(jaxb.getFamilyName()));
        repo.setHonorificPrefix(RPolyString.copyFromJAXB(jaxb.getHonorificPrefix()));
        repo.setHonorificSuffix(RPolyString.copyFromJAXB(jaxb.getHonorificSuffix()));
        repo.setEmployeeNumber(jaxb.getEmployeeNumber());
        repo.setLocality(RPolyString.copyFromJAXB(jaxb.getLocality()));
        repo.setAdditionalNames(RPolyString.copyFromJAXB(jaxb.getAdditionalName()));
        repo.setEmailAddress(jaxb.getEmailAddress());
        repo.setTelephoneNumber(jaxb.getTelephoneNumber());

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
        repo.setEmployeeType(RUtil.listToSet(jaxb.getEmployeeType()));
        repo.setOrganizationalUnit(RUtil.listPolyToSet(jaxb.getOrganizationalUnit()));

        for (ObjectReferenceType accountRef : jaxb.getAccountRef()) {
            RObjectReference ref = RUtil.jaxbRefToRepo(accountRef, repo, prismContext);
            if (ref != null) {
                repo.getAccountRefs().add(ref);
            }
        }

        ContainerIdGenerator gen = new ContainerIdGenerator();
        for (AssignmentType assignment : jaxb.getAssignment()) {
            RAssignment rAssignment = new RAssignment();
            rAssignment.setOwner(repo);

            RAssignment.copyFromJAXB(assignment, rAssignment, jaxb, prismContext);
            gen.generate(null, rAssignment);

            repo.getAssignments().add(rAssignment);
        }
    }

    public static void copyToJAXB(RUser repo, UserType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setFullName(RPolyString.copyToJAXB(repo.getFullName()));
        jaxb.setGivenName(RPolyString.copyToJAXB(repo.getGivenName()));
        jaxb.setFamilyName(RPolyString.copyToJAXB(repo.getFamilyName()));
        jaxb.setHonorificPrefix(RPolyString.copyToJAXB(repo.getHonorificPrefix()));
        jaxb.setHonorificSuffix(RPolyString.copyToJAXB(repo.getHonorificSuffix()));
        jaxb.setEmployeeNumber(repo.getEmployeeNumber());
        jaxb.setLocality(RPolyString.copyToJAXB(repo.getLocality()));
        jaxb.setAdditionalName(RPolyString.copyToJAXB(repo.getAdditionalName()));
        jaxb.setEmailAddress(repo.getEmailAddress());
        jaxb.setTelephoneNumber(repo.getTelephoneNumber());

        if (repo.getActivation() != null) {
            jaxb.setActivation(repo.getActivation().toJAXB(prismContext));
        }

        if (repo.getCredentials() != null) {
            PropertyPath path = new PropertyPath(UserType.F_CREDENTIALS);
            jaxb.setCredentials(repo.getCredentials().toJAXB(jaxb, path, prismContext));
        }

        jaxb.getEmployeeType().addAll(RUtil.safeSetToList(repo.getEmployeeType()));
        jaxb.getOrganizationalUnit().addAll(RUtil.safeSetPolyToList(repo.getOrganizationalUnit()));

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
        RUtil.revive(object, prismContext);
        RUser.copyToJAXB(this, object, prismContext);

        return object;
    }
}
