/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.*;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RFocusActivation;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceType;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbPath;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

@Entity
@ForeignKey(name = "fk_focus")
@Table(indexes = {
        @Index(name = "iFocusAdministrative", columnList = "administrativeStatus"),
        @Index(name = "iFocusEffective", columnList = "effectiveStatus"),
        @Index(name = "iLocality", columnList = "locality_orig"),
        @Index(name = "iFocusValidFrom", columnList = "validFrom"),
        @Index(name = "iFocusValidTo", columnList = "validTo")
})
@Persister(impl = MidPointJoinedPersister.class)
@DynamicUpdate
public abstract class RFocus extends RObject {

    private Set<RObjectReference<RShadow>> linkRef; // FocusType
    private Set<RObjectReference<RFocus>> personaRef; // FocusType
    private RFocusActivation activation; // FocusType

    //photo
    private boolean hasPhoto;
    private Set<RFocusPhoto> jpegPhoto;

    private RPolyString localityFocus;
    private String costCenter;

    private String emailAddress;
    private String telephoneNumber;
    private String locale;
    private String timezone;
    private String preferredLanguage;
    private XMLGregorianCalendar passwordCreateTimestamp;
    private XMLGregorianCalendar passwordModifyTimestamp;

    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 1")
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<RObjectReference<RShadow>> getLinkRef() {
        if (linkRef == null) {
            linkRef = new HashSet<>();
        }
        return linkRef;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 10")
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<RObjectReference<RFocus>> getPersonaRef() {
        if (personaRef == null) {
            personaRef = new HashSet<>();
        }
        return personaRef;
    }

    @Embedded
    public RFocusActivation getActivation() {
        return activation;
    }

    @JaxbName(localPart = "locality")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "locality_orig")),
            @AttributeOverride(name = "norm", column = @Column(name = "locality_norm"))
    })
    public RPolyString getLocalityFocus() {
        return localityFocus;
    }

    public String getCostCenter() {
        return costCenter;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public String getLocale() {
        return locale;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public String getTimezone() {
        return timezone;
    }

    @Type(XMLGregorianCalendarType.class)
    @JaxbPath(itemPath = { @JaxbName(localPart = "credentials"), @JaxbName(localPart = "password"),
            @JaxbName(localPart = "metadata"), @JaxbName(localPart = "createTimestamp") })
    public XMLGregorianCalendar getPasswordCreateTimestamp() {
        return passwordCreateTimestamp;
    }

    @Type(XMLGregorianCalendarType.class)
    @JaxbPath(itemPath = { @JaxbName(localPart = "credentials"), @JaxbName(localPart = "password"),
            @JaxbName(localPart = "metadata"), @JaxbName(localPart = "modifyTimestamp") })
    public XMLGregorianCalendar getPasswordModifyTimestamp() {
        return passwordModifyTimestamp;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public void setTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    public void setLocalityFocus(RPolyString locality) {
        this.localityFocus = locality;
    }

    public void setLinkRef(Set<RObjectReference<RShadow>> linkRef) {
        this.linkRef = linkRef;
    }

    public void setPersonaRef(Set<RObjectReference<RFocus>> personaRef) {
        this.personaRef = personaRef;
    }

    public void setActivation(RFocusActivation activation) {
        this.activation = activation;
    }

    public void setPasswordCreateTimestamp(XMLGregorianCalendar passwordCreateTimestamp) {
        this.passwordCreateTimestamp = passwordCreateTimestamp;
    }

    public void setPasswordModifyTimestamp(XMLGregorianCalendar passwordModifyTimestamp) {
        this.passwordModifyTimestamp = passwordModifyTimestamp;
    }

    public static void copyFocusInformationFromJAXB(FocusType jaxb, RFocus repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult)
            throws DtoTranslationException {
        copyAssignmentHolderInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setLocalityFocus(RPolyString.copyFromJAXB(jaxb.getLocality()));
        repo.setCostCenter(jaxb.getCostCenter());
        repo.setLocale(jaxb.getLocale());
        repo.setTimezone(jaxb.getTimezone());
        repo.setPreferredLanguage(jaxb.getPreferredLanguage());
        repo.setEmailAddress(jaxb.getEmailAddress());
        repo.setTelephoneNumber(jaxb.getTelephoneNumber());

        repo.getLinkRef().addAll(
                RUtil.toRObjectReferenceSet(jaxb.getLinkRef(), repo, RReferenceType.USER_ACCOUNT, repositoryContext.relationRegistry));
        repo.getPersonaRef().addAll(
                RUtil.toRObjectReferenceSet(jaxb.getPersonaRef(), repo, RReferenceType.PERSONA, repositoryContext.relationRegistry));

        if (jaxb.getActivation() != null) {
            RFocusActivation activation = new RFocusActivation();
            RFocusActivation.fromJaxb(jaxb.getActivation(), activation);
            repo.setActivation(activation);
        }

        if (jaxb.getJpegPhoto() != null) {
            RFocusPhoto photo = new RFocusPhoto();
            photo.setOwner(repo);
            photo.setPhoto(jaxb.getJpegPhoto());

            repo.getJpegPhoto().add(photo);
            repo.setHasPhoto(true);
        }

        if (jaxb.getCredentials() != null && jaxb.getCredentials().getPassword() != null
                && jaxb.getCredentials().getPassword().getMetadata() != null) {
            repo.setPasswordCreateTimestamp(jaxb.getCredentials().getPassword().getMetadata().getCreateTimestamp());
            repo.setPasswordModifyTimestamp(jaxb.getCredentials().getPassword().getMetadata().getModifyTimestamp());
        }
    }

    @ColumnDefault("false")
    public boolean isHasPhoto() {
        return hasPhoto;
    }

    // setting orphanRemoval = false prevents:
    //   (1) deletion of photos for RUsers that have no photos fetched (because fetching is lazy)
    //   (2) even querying of m_focus_photo table on RFocus merge
    // (see comments in SqlRepositoryServiceImpl.modifyObjectAttempt)
    @OneToMany(mappedBy = "owner")
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<RFocusPhoto> getJpegPhoto() {
        if (jpegPhoto == null) {
            jpegPhoto = new HashSet<>();
        }
        return jpegPhoto;
    }

    public void setHasPhoto(boolean hasPhoto) {
        this.hasPhoto = hasPhoto;
    }

    public void setJpegPhoto(Set<RFocusPhoto> jpegPhoto) {
        this.jpegPhoto = jpegPhoto;
    }

}
