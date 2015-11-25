/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.common.container;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.RAccessCertificationCampaign;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedNamedReference;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.data.common.other.RCReferenceOwner;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lazyman
 * @author mederly
 */

@JaxbType(type = AccessCertificationCaseType.class)
@Entity
@IdClass(RContainerId.class)
@Table(name = "m_acc_cert_case", indexes = {
        @Index(name = "iObjectRefTargetOid", columnList = "objectRef_targetOid"),
        @Index(name = "iTargetRefTargetOid", columnList = "targetRef_targetOid")
})
public class RAccessCertificationCase implements Container {

    private static final Trace LOGGER = TraceManager.getTrace(RAccessCertificationCase.class);

    public static final String F_OWNER = "owner";

    private Boolean trans;

    private byte[] fullObject;

    private RObject owner;
    private String ownerOid;
    private Integer id;

    private Set<RCertCaseReference> reviewerRef;
    private REmbeddedNamedReference objectRef;
    private REmbeddedNamedReference targetRef;

    private boolean enabled;
    private XMLGregorianCalendar reviewRequestedTimestamp;
    private XMLGregorianCalendar reviewDeadline;
    private XMLGregorianCalendar remediedTimestamp;
    // TODO: private Set<RAccessCertificationDecision> decisions;
    // TODO: currentResponse, currentResponseStage

    public RAccessCertificationCase() {
        this(null);
    }

    public RAccessCertificationCase(RObject owner) {
        this.setOwner(owner);
    }

    @Id
    @org.hibernate.annotations.ForeignKey(name = "fk_ac_case_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    public RObject getOwner() {
        return owner;
    }

    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    @NotQueryable
    public String getOwnerOid() {
        if (owner != null && ownerOid == null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator")
    @Column(name = "id")
    @NotQueryable
    public Integer getId() {
        return id;
    }

    @Where(clause = RCertCaseReference.REFERENCE_TYPE + "= 2")
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @org.hibernate.annotations.ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RCertCaseReference> getReviewerRef() {
        if (reviewerRef == null) {
            reviewerRef = new HashSet<>();
        }
        return reviewerRef;
    }

    public void setReviewerRef(Set<RCertCaseReference> reviewerRef) {
        this.reviewerRef = reviewerRef;
    }

    @Embedded
    public REmbeddedNamedReference getTargetRef() {
        return targetRef;
    }

    @Embedded
    public REmbeddedNamedReference getObjectRef() {
        return objectRef;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public XMLGregorianCalendar getReviewRequestedTimestamp() {
        return reviewRequestedTimestamp;
    }

    public XMLGregorianCalendar getReviewDeadline() {
        return reviewDeadline;
    }

    public XMLGregorianCalendar getRemediedTimestamp() {
        return remediedTimestamp;
    }

    public void setOwner(RObject owner) {
        this.owner = owner;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setTargetRef(REmbeddedNamedReference targetRef) {
        this.targetRef = targetRef;
    }

    public void setObjectRef(REmbeddedNamedReference objectRef) {
        this.objectRef = objectRef;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setReviewRequestedTimestamp(XMLGregorianCalendar reviewRequestedTimestamp) {
        this.reviewRequestedTimestamp = reviewRequestedTimestamp;
    }

    public void setReviewDeadline(XMLGregorianCalendar reviewDeadline) {
        this.reviewDeadline = reviewDeadline;
    }

    public void setRemediedTimestamp(XMLGregorianCalendar remediedTimestamp) {
        this.remediedTimestamp = remediedTimestamp;
    }

    @Lob
    public byte[] getFullObject() {
        return fullObject;
    }

    public void setFullObject(byte[] fullObject) {
        this.fullObject = fullObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RAccessCertificationCase)) return false;

        RAccessCertificationCase that = (RAccessCertificationCase) o;

        if (enabled != that.enabled) return false;
        if (!Arrays.equals(fullObject, that.fullObject)) return false;
        if (ownerOid != null ? !ownerOid.equals(that.ownerOid) : that.ownerOid != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (reviewerRef != null ? !reviewerRef.equals(that.reviewerRef) : that.reviewerRef != null) return false;
        if (objectRef != null ? !objectRef.equals(that.objectRef) : that.objectRef != null) return false;
        if (targetRef != null ? !targetRef.equals(that.targetRef) : that.targetRef != null) return false;
        if (reviewRequestedTimestamp != null ? !reviewRequestedTimestamp.equals(that.reviewRequestedTimestamp) : that.reviewRequestedTimestamp != null)
            return false;
        if (reviewDeadline != null ? !reviewDeadline.equals(that.reviewDeadline) : that.reviewDeadline != null)
            return false;
        return !(remediedTimestamp != null ? !remediedTimestamp.equals(that.remediedTimestamp) : that.remediedTimestamp != null);
    }

    @Override
    public int hashCode() {
        int result = ownerOid != null ? ownerOid.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (reviewerRef != null ? reviewerRef.hashCode() : 0);
        result = 31 * result + (objectRef != null ? objectRef.hashCode() : 0);
        result = 31 * result + (targetRef != null ? targetRef.hashCode() : 0);
        result = 31 * result + (enabled ? 1 : 0);
        result = 31 * result + (reviewRequestedTimestamp != null ? reviewRequestedTimestamp.hashCode() : 0);
        result = 31 * result + (reviewDeadline != null ? reviewDeadline.hashCode() : 0);
        result = 31 * result + (remediedTimestamp != null ? remediedTimestamp.hashCode() : 0);
        return result;
    }

//    public static void copyFromJAXB(AccessCertificationCaseType jaxb, RAccessCertificationCase repo, ObjectType parent, PrismContext prismContext,
//                                    IdGeneratorResult generatorResult) throws DtoTranslationException {
//        Validate.notNull(repo, "Repo object must not be null.");
//        Validate.notNull(jaxb, "JAXB object must not be null.");
//
//        repo.setOwnerOid(parent.getOid());
//        repo.setId(RUtil.toInteger(jaxb.getId()));
//
//        repo.setTargetRef(RUtil.jaxbRefToEmbeddedNamedRepoRef(jaxb.getTargetRef(), prismContext));
//        repo.setTargetRef(RUtil.jaxbRefToEmbeddedNamedRepoRef(jaxb.getObjectRef(), prismContext));
//        repo.getReviewerRef().addAll(RCertCaseReference.safeListReferenceToSet(
//                jaxb.getReviewerRef(), prismContext, repo, RCReferenceOwner.CASE_REVIEWER));
//        repo.setEnabled(BooleanUtils.isTrue(jaxb.isEnabled()));
//        repo.setReviewRequestedTimestamp(jaxb.getReviewRequestedTimestamp());
//        repo.setReviewDeadline(jaxb.getReviewDeadline());
//        repo.setRemediedTimestamp(jaxb.getRemediedTimestamp());
//    }

    @Override
    public String toString() {
        return "RAccessCertificationCase{" +
                "id=" + id +
                ", ownerOid='" + ownerOid + '\'' +
                ", owner=" + owner +
                ", targetRef=" + targetRef +
                ", objectRef=" + objectRef +
                '}';
    }

    @Override
    @Transient
    public Boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    public static RAccessCertificationCase toRepo(RAccessCertificationCampaign owner, AccessCertificationCaseType case1, PrismContext prismContext) {
        RAccessCertificationCase rCase = toRepo(case1, prismContext);
        rCase.setOwner(owner);
        return rCase;
    }

    public static RAccessCertificationCase toRepo(String ownerOid, AccessCertificationCaseType case1, PrismContext prismContext) {
        RAccessCertificationCase rCase = toRepo(case1, prismContext);
        rCase.setOwnerOid(ownerOid);
        return rCase;
    }

    private static RAccessCertificationCase toRepo(AccessCertificationCaseType case1, PrismContext prismContext) {
        RAccessCertificationCase rCase = new RAccessCertificationCase();
        rCase.setId(RUtil.toInteger(case1.getId()));
        rCase.setObjectRef(RUtil.jaxbRefToEmbeddedNamedRepoRef(case1.getObjectRef(), prismContext));
        rCase.setTargetRef(RUtil.jaxbRefToEmbeddedNamedRepoRef(case1.getTargetRef(), prismContext));
        rCase.getReviewerRef().addAll(RCertCaseReference.safeListReferenceToSet(
                case1.getReviewerRef(), prismContext, rCase, RCReferenceOwner.CASE_REVIEWER));
        rCase.setEnabled(case1.isEnabled());
        rCase.setReviewRequestedTimestamp(case1.getReviewRequestedTimestamp());
        rCase.setReviewDeadline(case1.getReviewDeadline());
        rCase.setRemediedTimestamp(case1.getRemediedTimestamp());
        String xml;
        try {
            xml = prismContext.serializeContainerValueToString(case1.asPrismContainerValue(), new QName("value"), PrismContext.LANG_XML);
        } catch (SchemaException e) {
            throw new IllegalStateException("Couldn't serialize certification case to string", e);
        }
        byte[] fullObject = RUtil.getByteArrayFromXml(xml, false);
        rCase.setFullObject(fullObject);

        return rCase;
    }

    public AccessCertificationCaseType toJAXB(PrismContext prismContext) throws SchemaException {
        return createJaxb(fullObject, prismContext);
    }

    // TODO find appropriate name
    public static AccessCertificationCaseType createJaxb(byte[] fullObject, PrismContext prismContext) throws SchemaException {
        String xml = RUtil.getXmlFromByteArray(fullObject, false);
        PrismContainer<AccessCertificationCaseType> caseContainer;
        try {
            // TODO tolerant mode
            caseContainer = prismContext.parseContainer(xml, AccessCertificationCaseType.class, PrismContext.LANG_XML);
        } catch (SchemaException e) {
            LOGGER.debug("Couldn't parse certification case because of schema exception ({}):\nData: {}", e, xml);
            throw e;
        } catch (RuntimeException e) {
            LOGGER.debug("Couldn't parse certification case because of unexpected exception ({}):\nData: {}", e, xml);
            throw e;
        }
        return caseContainer.getValue().asContainerable().clone();      // clone in order to make it parent-less
    }
}
