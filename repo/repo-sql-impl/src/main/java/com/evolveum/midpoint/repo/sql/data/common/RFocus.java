/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.*;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.*;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RActivation;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.other.RAssignmentOwner;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.QueryEntity;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualCollection;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualQueryParam;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author lazyman
 */
@QueryEntity(collections = {
        @VirtualCollection(jaxbName = @JaxbName(localPart = "assignment"), jaxbType = Set.class,
                jpaName = "assignments", jpaType = Set.class, additionalParams = {
                @VirtualQueryParam(name = "assignmentOwner", type = RAssignmentOwner.class,
                        value = "FOCUS") }, collectionType = RAssignment.class) })
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
public abstract class RFocus extends RObject {

    private Set<RObjectReference<RShadow>> linkRef;                         // FocusType
    private Set<RObjectReference<RFocus>> personaRef;                       // FocusType
    private RActivation activation;                                         // FocusType
    private Set<String> policySituation;                                    // ObjectType
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
    public RActivation getActivation() {
        return activation;
    }

    @ElementCollection
    @ForeignKey(name = "fk_focus_policy_situation")
    @CollectionTable(name = "m_focus_policy_situation", joinColumns = {
            @JoinColumn(name = "focus_oid", referencedColumnName = "oid")
    })
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<String> getPolicySituation() {
        return policySituation;
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

    public void setPolicySituation(Set<String> policySituation) {
        this.policySituation = policySituation;
    }

    public void setLinkRef(Set<RObjectReference<RShadow>> linkRef) {
        this.linkRef = linkRef;
    }

    public void setPersonaRef(Set<RObjectReference<RFocus>> personaRef) {
        this.personaRef = personaRef;
    }

    public void setActivation(RActivation activation) {
        this.activation = activation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }

        RFocus other = (RFocus) o;

        if (linkRef != null ? !linkRef.equals(other.linkRef) : other.linkRef != null) { return false; }
        if (activation != null ? !activation.equals(other.activation) : other.activation != null) { return false; }
        if (policySituation != null ? !policySituation.equals(other.policySituation) : other.policySituation != null) {
            return false;
        }
        if (localityFocus != null ? !localityFocus.equals(other.localityFocus) : other.localityFocus != null) { return false; }
        if (costCenter != null ? !costCenter.equals(other.costCenter) : other.costCenter != null) { return false; }
        if (emailAddress != null ? !emailAddress.equals(other.emailAddress) : other.emailAddress != null) { return false; }
        if (telephoneNumber != null ? !telephoneNumber.equals(other.telephoneNumber) : other.telephoneNumber != null) {
            return false;
        }
        if (locale != null ? !locale.equals(other.locale) : other.locale != null) { return false; }
        if (preferredLanguage != null ? !preferredLanguage.equals(other.preferredLanguage) :
                other.preferredLanguage != null) { return false; }
        if (timezone != null ? !timezone.equals(other.timezone) : other.timezone != null) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (activation != null ? activation.hashCode() : 0);
        result = 31 * result + (localityFocus != null ? localityFocus.hashCode() : 0);
        result = 31 * result + (costCenter != null ? costCenter.hashCode() : 0);
        result = 31 * result + (locale != null ? locale.hashCode() : 0);
        result = 31 * result + (preferredLanguage != null ? preferredLanguage.hashCode() : 0);
        result = 31 * result + (timezone != null ? timezone.hashCode() : 0);

        return result;
    }

    public static void copyFocusInformationFromJAXB(FocusType jaxb, RFocus repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult)
            throws DtoTranslationException {
        copyAssignmentHolderInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setLocalityFocus(RPolyString.copyFromJAXB(jaxb.getLocality()));
        repo.setCostCenter(jaxb.getCostCenter());
        repo.getLinkRef().addAll(
                RUtil.safeListReferenceToSet(jaxb.getLinkRef(), repo, RReferenceOwner.USER_ACCOUNT, repositoryContext.relationRegistry));
        repo.getPersonaRef().addAll(
                RUtil.safeListReferenceToSet(jaxb.getPersonaRef(), repo, RReferenceOwner.PERSONA, repositoryContext.relationRegistry));

        if (jaxb.getActivation() != null) {
            RActivation activation = new RActivation();
            RActivation.fromJaxb(jaxb.getActivation(), activation, repositoryContext);
            repo.setActivation(activation);
        }

        if (jaxb.getJpegPhoto() != null) {
            RFocusPhoto photo = new RFocusPhoto();
            photo.setOwner(repo);
            photo.setPhoto(jaxb.getJpegPhoto());

            repo.getJpegPhoto().add(photo);
            repo.setHasPhoto(true);
        }

        repo.setPolicySituation(RUtil.listToSet(jaxb.getPolicySituation()));
    }

    @ColumnDefault("false")
    public boolean isHasPhoto() {
        return hasPhoto;
    }

    // setting orphanRemoval = false prevents:
    //   (1) deletion of photos for RUsers that have no photos fetched (because fetching is lazy)
    //   (2) even querying of m_focus_photo table on RFocus merge
    // (see comments in SqlRepositoryServiceImpl.modifyObjectAttempt)
    @OneToMany(mappedBy = "owner", orphanRemoval = false)
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
