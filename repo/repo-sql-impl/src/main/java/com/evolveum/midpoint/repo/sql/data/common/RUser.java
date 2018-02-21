/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import java.util.Collection;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_user_name", columnNames = {"name_norm"}),
        indexes = {
                @Index(name = "iFullName", columnList = "fullName_orig"),
                @Index(name = "iFamilyName", columnList = "familyName_orig"),
                @Index(name = "iGivenName", columnList = "givenName_orig"),
                @Index(name = "iEmployeeNumber", columnList = "employeeNumber")})
@ForeignKey(name = "fk_user")
@Persister(impl = MidPointJoinedPersister.class)
public class RUser extends RFocus<UserType> {

    private RPolyString nameCopy;
    private RPolyString fullName;
    private RPolyString givenName;
    private RPolyString familyName;
    private RPolyString additionalName;
    private RPolyString honorificPrefix;
    private RPolyString honorificSuffix;
    private String employeeNumber;
    @Deprecated //todo remove collection in 3.9
    private Set<String> employeeType;
    private Set<RPolyString> organizationalUnit;
    private RPolyString title;
    private RPolyString nickName;
    private Set<RPolyString> organization;

    @ElementCollection
    @ForeignKey(name = "fk_user_organization")
    @CollectionTable(name = "m_user_organization", joinColumns = {
            @JoinColumn(name = "user_oid", referencedColumnName = "oid")
    })
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RPolyString> getOrganization() {
        return organization;
    }

    @Embedded
    public RPolyString getAdditionalName() {
        return additionalName;
    }

    @ElementCollection
    @ForeignKey(name = "fk_user_org_unit")
    @CollectionTable(name = "m_user_organizational_unit", joinColumns = {
            @JoinColumn(name = "user_oid", referencedColumnName = "oid")
    })
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RPolyString> getOrganizationalUnit() {
        return organizationalUnit;
    }

    @ElementCollection
    @ForeignKey(name = "fk_user_employee_type")
    @CollectionTable(name = "m_user_employee_type", joinColumns = {
            @JoinColumn(name = "user_oid", referencedColumnName = "oid")
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

    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "name_orig")),
            @AttributeOverride(name = "norm", column = @Column(name = "name_norm"))
    })
    @Embedded
    public RPolyString getNameCopy() {
        return nameCopy;
    }

    public void setNameCopy(RPolyString nameCopy) {
        this.nameCopy = nameCopy;
    }

    @Embedded
    public RPolyString getNickName() {
        return nickName;
    }

    @Embedded
    public RPolyString getTitle() {
        return title;
    }

    public void setOrganization(Set<RPolyString> organization) {
        this.organization = organization;
    }

    public void setNickName(RPolyString nickName) {
        this.nickName = nickName;
    }

    public void setTitle(RPolyString title) {
        this.title = title;
    }

    public void setAdditionalName(RPolyString additionalName) {
        this.additionalName = additionalName;
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

    public void setOrganizationalUnit(Set<RPolyString> organizationalUnit) {
        this.organizationalUnit = organizationalUnit;
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

        if (nameCopy != null ? !nameCopy.equals(rUser.nameCopy) : rUser.nameCopy != null) return false;
        if (additionalName != null ? !additionalName.equals(rUser.additionalName) : rUser.additionalName != null)
            return false;
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
        if (organizationalUnit != null ? !organizationalUnit.equals(rUser.organizationalUnit) : rUser.organizationalUnit != null)
            return false;
        if (title != null ? !title.equals(rUser.title) : rUser.title != null) return false;
        if (nickName != null ? !nickName.equals(rUser.nickName) : rUser.nickName != null) return false;
        if (organization != null ? !organization.equals(rUser.organization) : rUser.organization != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (nameCopy != null ? nameCopy.hashCode() : 0);
        result = 31 * result + (fullName != null ? fullName.hashCode() : 0);
        result = 31 * result + (givenName != null ? givenName.hashCode() : 0);
        result = 31 * result + (familyName != null ? familyName.hashCode() : 0);
        result = 31 * result + (honorificPrefix != null ? honorificPrefix.hashCode() : 0);
        result = 31 * result + (honorificSuffix != null ? honorificSuffix.hashCode() : 0);
        result = 31 * result + (employeeNumber != null ? employeeNumber.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (nickName != null ? nickName.hashCode() : 0);

        return result;
    }

    public static void copyFromJAXB(UserType jaxb, RUser repo, RepositoryContext repositoryContext,
                                    IdGeneratorResult generatorResult) throws DtoTranslationException {
        RFocus.copyFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setFullName(RPolyString.copyFromJAXB(jaxb.getFullName()));
        repo.setGivenName(RPolyString.copyFromJAXB(jaxb.getGivenName()));
        repo.setFamilyName(RPolyString.copyFromJAXB(jaxb.getFamilyName()));
        repo.setHonorificPrefix(RPolyString.copyFromJAXB(jaxb.getHonorificPrefix()));
        repo.setHonorificSuffix(RPolyString.copyFromJAXB(jaxb.getHonorificSuffix()));
        repo.setEmployeeNumber(jaxb.getEmployeeNumber());
        repo.setAdditionalName(RPolyString.copyFromJAXB(jaxb.getAdditionalName()));
        repo.setEmailAddress(jaxb.getEmailAddress());
        repo.setTelephoneNumber(jaxb.getTelephoneNumber());

        repo.setLocale(jaxb.getLocale());
        repo.setTimezone(jaxb.getTimezone());
        repo.setPreferredLanguage(jaxb.getPreferredLanguage());
        repo.setTitle(RPolyString.copyFromJAXB(jaxb.getTitle()));
        repo.setNickName(RPolyString.copyFromJAXB(jaxb.getNickName()));

        //sets
        repo.setEmployeeType(RUtil.listToSet(jaxb.getEmployeeType()));
        repo.setOrganizationalUnit(RUtil.listPolyToSet(jaxb.getOrganizationalUnit()));
        repo.setOrganization(RUtil.listPolyToSet(jaxb.getOrganization()));
    }

    @Override
    public UserType toJAXB(PrismContext prismContext, Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {
        UserType object = new UserType();
        RUtil.revive(object, prismContext);
        RUser.copyToJAXB(this, object, prismContext, options);

        return object;
    }
}
