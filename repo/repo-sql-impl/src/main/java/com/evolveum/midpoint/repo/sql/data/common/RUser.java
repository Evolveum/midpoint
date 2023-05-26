/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import java.util.Set;
import jakarta.persistence.*;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_user_name", columnNames = { "name_norm" }),
        indexes = {
                @Index(name = "iFullName", columnList = "fullName_orig"),
                @Index(name = "iFamilyName", columnList = "familyName_orig"),
                @Index(name = "iGivenName", columnList = "givenName_orig"),
                @Index(name = "iEmployeeNumber", columnList = "employeeNumber"),
                @Index(name = "iUserNameOrig", columnList = "name_orig") })
@ForeignKey(name = "fk_user")
@Persister(impl = MidPointJoinedPersister.class)
@DynamicUpdate
public class RUser extends RFocus {

    private RPolyString nameCopy;
    private RPolyString fullName;
    private RPolyString givenName;
    private RPolyString familyName;
    private RPolyString additionalName;
    private RPolyString honorificPrefix;
    private RPolyString honorificSuffix;
    private String employeeNumber;
    private Set<RPolyString> organizationalUnit;
    private RPolyString title;
    private RPolyString nickName;
    private Set<RPolyString> organization;

    @ElementCollection
    @ForeignKey(name = "fk_user_organization")
    @CollectionTable(name = "m_user_organization", joinColumns = {
            @JoinColumn(name = "user_oid", referencedColumnName = "oid")
    })
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
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
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<RPolyString> getOrganizationalUnit() {
        return organizationalUnit;
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

    @JaxbName(localPart = "name")
    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "name_orig")),
            @AttributeOverride(name = "norm", column = @Column(name = "name_norm"))
    })
    @Embedded
    @NeverNull
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

    // dynamically called
    public static void copyFromJAXB(UserType jaxb, RUser repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        copyFocusInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setFullName(RPolyString.copyFromJAXB(jaxb.getFullName()));
        repo.setGivenName(RPolyString.copyFromJAXB(jaxb.getGivenName()));
        repo.setFamilyName(RPolyString.copyFromJAXB(jaxb.getFamilyName()));
        repo.setHonorificPrefix(RPolyString.copyFromJAXB(jaxb.getHonorificPrefix()));
        repo.setHonorificSuffix(RPolyString.copyFromJAXB(jaxb.getHonorificSuffix()));
        repo.setEmployeeNumber(jaxb.getEmployeeNumber());
        repo.setAdditionalName(RPolyString.copyFromJAXB(jaxb.getAdditionalName()));
        repo.setTitle(RPolyString.copyFromJAXB(jaxb.getTitle()));
        repo.setNickName(RPolyString.copyFromJAXB(jaxb.getNickName()));

        //sets
        repo.setOrganizationalUnit(RUtil.listPolyToSet(jaxb.getOrganizationalUnit()));
        repo.setOrganization(RUtil.listPolyToSet(jaxb.getOrganization()));
    }
}
