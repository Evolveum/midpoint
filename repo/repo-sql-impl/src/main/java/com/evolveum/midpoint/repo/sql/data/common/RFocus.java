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
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RActivation;
import com.evolveum.midpoint.repo.sql.data.common.other.RAssignmentOwner;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.QueryEntity;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualCollection;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualQueryParam;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import org.hibernate.annotations.*;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author lazyman
 */
@QueryEntity(collections = {
        @VirtualCollection(jaxbName = @JaxbName(localPart = "assignment"), jaxbType = Set.class,
                jpaName = "assignments", jpaType = Set.class, additionalParams = {
                @VirtualQueryParam(name = "assignmentOwner", type = RAssignmentOwner.class,
                        value = "FOCUS")}, collectionType = RAssignment.class)})
@Entity
@ForeignKey(name = "fk_focus")
@org.hibernate.annotations.Table(appliesTo = "m_focus",
        indexes = {@Index(name = "iFocusAdministrative", columnNames = "administrativeStatus"),
                @Index(name = "iFocusEffective", columnNames = "effectiveStatus")})
public abstract class RFocus<T extends FocusType> extends RObject<T> {

    private Set<RObjectReference> linkRef;
    private Set<RObjectReference> roleMembershipRef;
    private Set<RAssignment> assignments;
    private RActivation activation;
    //photo
    private boolean hasPhoto;
    private Set<RFocusPhoto> jpegPhoto;

    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 1")
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReference> getLinkRef() {
        if (linkRef == null) {
            linkRef = new HashSet<>();
        }
        return linkRef;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 8")
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReference> getRoleMembershipRef() {
        if (roleMembershipRef == null) {
            roleMembershipRef = new HashSet<>();
        }
        return roleMembershipRef;
    }

    @Transient
    protected Set<RAssignment> getAssignments(RAssignmentOwner owner) {
        Set<RAssignment> assignments = getAssignments();
        Set<RAssignment> wanted = new HashSet<>();
        if (assignments == null) {
            return wanted;
        }

        Iterator<RAssignment> iterator = assignments.iterator();
        while (iterator.hasNext()) {
            RAssignment ass = iterator.next();
            if (owner.equals(ass.getAssignmentOwner())) {
                wanted.add(ass);
            }
        }

        return wanted;
    }

    @Transient
    public Set<RAssignment> getAssignment() {
        return getAssignments(RAssignmentOwner.FOCUS);
    }

    @OneToMany(mappedBy = RAssignment.F_OWNER, orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAssignment> getAssignments() {
        if (assignments == null) {
            assignments = new HashSet<>();
        }
        return assignments;
    }

    @Embedded
    public RActivation getActivation() {
        return activation;
    }

    public void setAssignments(Set<RAssignment> assignments) {
        this.assignments = assignments;
    }

    public void setLinkRef(Set<RObjectReference> linkRef) {
        this.linkRef = linkRef;
    }

    public void setRoleMembershipRef(Set<RObjectReference> roleMembershipRef) {
        this.roleMembershipRef = roleMembershipRef;
    }

    public void setActivation(RActivation activation) {
        this.activation = activation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RFocus other = (RFocus) o;

        if (assignments != null ? !assignments.equals(other.assignments) : other.assignments != null) return false;
        if (linkRef != null ? !linkRef.equals(other.linkRef) : other.linkRef != null) return false;
        if (roleMembershipRef != null ? !roleMembershipRef.equals(other.roleMembershipRef) : other.roleMembershipRef != null) return false;
        if (activation != null ? !activation.equals(other.activation) : other.activation != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (activation != null ? activation.hashCode() : 0);

        return result;
    }

    public static <T extends FocusType> void copyFromJAXB(FocusType jaxb, RFocus<T> repo, PrismContext prismContext,
                                                          IdGeneratorResult generatorResult)
            throws DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext, generatorResult);

        repo.getLinkRef().addAll(
                RUtil.safeListReferenceToSet(jaxb.getLinkRef(), prismContext, repo, RReferenceOwner.USER_ACCOUNT));

        repo.getRoleMembershipRef().addAll(
                RUtil.safeListReferenceToSet(jaxb.getRoleMembershipRef(), prismContext, repo, RReferenceOwner.ROLE_MEMBER));

        for (AssignmentType assignment : jaxb.getAssignment()) {
            RAssignment rAssignment = new RAssignment(repo, RAssignmentOwner.FOCUS);
            RAssignment.copyFromJAXB(assignment, rAssignment, jaxb, prismContext, generatorResult);

            repo.getAssignments().add(rAssignment);
        }

        if (jaxb.getActivation() != null) {
            RActivation activation = new RActivation();
            RActivation.copyFromJAXB(jaxb.getActivation(), activation, prismContext);
            repo.setActivation(activation);
        }

        if (jaxb.getJpegPhoto() != null) {
            RFocusPhoto photo = new RFocusPhoto();
            photo.setOwner(repo);
            photo.setPhoto(jaxb.getJpegPhoto());

            repo.getJpegPhoto().add(photo);
            repo.setHasPhoto(true);
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
    @OneToMany(mappedBy = "owner", orphanRemoval = false)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
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
