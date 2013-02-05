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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RActivation;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RCredentials;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.data.common.type.RAccountRef;
import com.evolveum.midpoint.repo.sql.data.common.type.RParentOrgRef;
import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
import com.evolveum.midpoint.repo.sql.query.QueryEntity;
import com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name_norm"}))
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
    private RPolyString name;
    @QueryAttribute(polyString = true)
    private RPolyString fullName;
    @QueryAttribute(polyString = true)
    private RPolyString givenName;
    @QueryAttribute(polyString = true)
    private RPolyString familyName;
    @QueryAttribute(polyString = true)
    private RPolyString additionalName;
    @QueryAttribute(polyString = true)
    private RPolyString honorificPrefix;
    @QueryAttribute(polyString = true)
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
    @QueryAttribute
    private String costCenter;
    @QueryAttribute
    private String locale;
    @QueryAttribute
    private String timezone;
    @QueryAttribute(polyString = true)
    private RPolyString title;
    @QueryAttribute(polyString = true)
    private RPolyString nickName;
    @QueryAttribute
    private String preferredLanguage;
    @QueryAttribute(name = "accountRef", multiValue = true, reference = true)
    private Set<RObjectReference> accountRefs;
    private Set<RAssignment> assignments;
    private Set<RPolyString> organization;

    @ElementCollection
    @ForeignKey(name = "fk_user_organization")
    @CollectionTable(name = "m_user_organization", joinColumns = {
            @JoinColumn(name = "user_oid", referencedColumnName = "oid"),
            @JoinColumn(name = "user_id", referencedColumnName = "id")
    })
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RPolyString> getOrganization() {
        return organization;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "=" + RAccountRef.DISCRIMINATOR)
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

    @Embedded
    public RPolyString getFamilyName() {
        return familyName;
    }

    @Embedded
    public RPolyString getFullName() {
        return fullName;
    }

    @Embedded
    public RPolyString getGivenName() {
        return givenName;
    }

    @Embedded
    public RPolyString getLocality() {
        return locality;
    }

    @Index(name = "iEmployeeNumber")
    public String getEmployeeNumber() {
        return employeeNumber;
    }

    @Embedded
    public RPolyString getHonorificPrefix() {
        return honorificPrefix;
    }

    @Embedded
    public RPolyString getHonorificSuffix() {
        return honorificSuffix;
    }

    @Embedded
    public RPolyString getName() {
        return name;
    }

    public String getCostCenter() {
        return costCenter;
    }

    public String getLocale() {
        return locale;
    }

    @Embedded
    public RPolyString getNickName() {
        return nickName;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public String getTimezone() {
        return timezone;
    }

    @Embedded
    public RPolyString getTitle() {
        return title;
    }

    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void setOrganization(Set<RPolyString> organization) {
        this.organization = organization;
    }

    public void setNickName(RPolyString nickName) {
        this.nickName = nickName;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public void setTitle(RPolyString title) {
        this.title = title;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    public void setActivation(RActivation activation) {
        this.activation = activation;
    }

    public void setAdditionalName(RPolyString additionalName) {
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

        if (name != null ? !name.equals(rUser.name) : rUser.name != null) return false;
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
        if (locale != null ? !locale.equals(rUser.locale) : rUser.locale != null) return false;
        if (title != null ? !title.equals(rUser.title) : rUser.title != null) return false;
        if (nickName != null ? !nickName.equals(rUser.nickName) : rUser.nickName != null) return false;
        if (preferredLanguage != null ? !preferredLanguage.equals(rUser.preferredLanguage) :
                rUser.preferredLanguage != null) return false;
        if (timezone != null ? !timezone.equals(rUser.timezone) : rUser.timezone != null) return false;
        if (costCenter != null ? !costCenter.equals(rUser.costCenter) : rUser.costCenter != null) return false;
        if (organization != null ? !organization.equals(rUser.organization) : rUser.organization != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (fullName != null ? fullName.hashCode() : 0);
        result = 31 * result + (givenName != null ? givenName.hashCode() : 0);
        result = 31 * result + (familyName != null ? familyName.hashCode() : 0);
        result = 31 * result + (honorificPrefix != null ? honorificPrefix.hashCode() : 0);
        result = 31 * result + (honorificSuffix != null ? honorificSuffix.hashCode() : 0);
        result = 31 * result + (employeeNumber != null ? employeeNumber.hashCode() : 0);
        result = 31 * result + (locality != null ? locality.hashCode() : 0);
        result = 31 * result + (credentials != null ? credentials.hashCode() : 0);
        result = 31 * result + (activation != null ? activation.hashCode() : 0);
        result = 31 * result + (costCenter != null ? costCenter.hashCode() : 0);
        result = 31 * result + (locale != null ? locale.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (nickName != null ? nickName.hashCode() : 0);
        result = 31 * result + (preferredLanguage != null ? preferredLanguage.hashCode() : 0);
        result = 31 * result + (timezone != null ? timezone.hashCode() : 0);

        return result;
    }

    public static void copyFromJAXB(UserType jaxb, RUser repo, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setFullName(RPolyString.copyFromJAXB(jaxb.getFullName()));
        repo.setGivenName(RPolyString.copyFromJAXB(jaxb.getGivenName()));
        repo.setFamilyName(RPolyString.copyFromJAXB(jaxb.getFamilyName()));
        repo.setHonorificPrefix(RPolyString.copyFromJAXB(jaxb.getHonorificPrefix()));
        repo.setHonorificSuffix(RPolyString.copyFromJAXB(jaxb.getHonorificSuffix()));
        repo.setEmployeeNumber(jaxb.getEmployeeNumber());
        repo.setLocality(RPolyString.copyFromJAXB(jaxb.getLocality()));
        repo.setAdditionalName(RPolyString.copyFromJAXB(jaxb.getAdditionalName()));
        repo.setEmailAddress(jaxb.getEmailAddress());
        repo.setTelephoneNumber(jaxb.getTelephoneNumber());

        repo.setCostCenter(jaxb.getCostCenter());
        repo.setLocale(jaxb.getLocale());
        repo.setTimezone(jaxb.getTimezone());
        repo.setPreferredLanguage(jaxb.getPreferredLanguage());
        repo.setTitle(RPolyString.copyFromJAXB(jaxb.getTitle()));
        repo.setNickName(RPolyString.copyFromJAXB(jaxb.getNickName()));

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
        repo.setOrganization(RUtil.listPolyToSet(jaxb.getOrganization()));

        repo.getAccountRefs().addAll(RUtil.safeListReferenceToSet(jaxb.getAccountRef(), prismContext, repo, RReferenceOwner.USER_ACCOUNT));

        ContainerIdGenerator gen = new ContainerIdGenerator();
        for (AssignmentType assignment : jaxb.getAssignment()) {
            RAssignment rAssignment = new RAssignment();
            rAssignment.setOwner(repo);

            RAssignment.copyFromJAXB(assignment, rAssignment, jaxb, prismContext);
            rAssignment.setId((Long) gen.generate(null, rAssignment));

            repo.getAssignments().add(rAssignment);
        }
    }

    public static void copyToJAXB(RUser repo, UserType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setName(RPolyString.copyToJAXB(repo.getName()));
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

        jaxb.setCostCenter(repo.getCostCenter());
        jaxb.setTimezone(repo.getTimezone());
        jaxb.setLocale(repo.getLocale());
        jaxb.setPreferredLanguage(repo.getPreferredLanguage());
        jaxb.setTitle(RPolyString.copyToJAXB(repo.getTitle()));
        jaxb.setNickName(RPolyString.copyToJAXB(repo.getNickName()));

        if (repo.getActivation() != null) {
            jaxb.setActivation(repo.getActivation().toJAXB(prismContext));
        }

        if (repo.getCredentials() != null) {
            ItemPath path = new ItemPath(UserType.F_CREDENTIALS);
            jaxb.setCredentials(repo.getCredentials().toJAXB(jaxb, path, prismContext));
        }

        List types = RUtil.safeSetToList(repo.getEmployeeType());
        if (!types.isEmpty()) {
            jaxb.getEmployeeType().addAll(types);
        }

        List units = RUtil.safeSetPolyToList(repo.getOrganizationalUnit());
        if (!units.isEmpty()) {
            jaxb.getOrganizationalUnit().addAll(units);
        }

        units = RUtil.safeSetPolyToList(repo.getOrganization());
        if (!units.isEmpty()) {
            jaxb.getOrganization().addAll(units);
        }

        List accRefs = RUtil.safeSetReferencesToList(repo.getAccountRefs(), prismContext);
        if (!accRefs.isEmpty()) {
            jaxb.getAccountRef().addAll(accRefs);
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
