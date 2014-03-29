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
import com.evolveum.midpoint.repo.sql.data.common.type.RLinkRef;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.QueryEntity;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualCollection;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualQueryParam;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Where;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.*;

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
    private Set<RAssignment> assignments;
    private RActivation activation;
    private String interationToken;
    private Integer iteration;

    @Where(clause = RObjectReference.REFERENCE_TYPE + "=" + RLinkRef.DISCRIMINATOR)
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReference> getLinkRef() {
        if (linkRef == null) {
            linkRef = new HashSet<RObjectReference>();
        }
        return linkRef;
    }

    @Transient
    protected Set<RAssignment> getAssignments(RAssignmentOwner owner) {
        Set<RAssignment> assignments = getAssignments();
        Set<RAssignment> wanted = new HashSet<RAssignment>();
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

    public String getInterationToken() {
        return interationToken;
    }

    public Integer getIteration() {
        return iteration;
    }

    public void setAssignments(Set<RAssignment> assignments) {
        this.assignments = assignments;
    }

    public void setLinkRef(Set<RObjectReference> linkRef) {
        this.linkRef = linkRef;
    }

    public void setActivation(RActivation activation) {
        this.activation = activation;
    }

    public void setInterationToken(String interationToken) {
        this.interationToken = interationToken;
    }

    public void setIteration(Integer iteration) {
        this.iteration = iteration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RFocus other = (RFocus) o;

        if (assignments != null ? !assignments.equals(other.assignments) : other.assignments != null) return false;
        if (linkRef != null ? !linkRef.equals(other.linkRef) : other.linkRef != null) return false;
        if (activation != null ? !activation.equals(other.activation) : other.activation != null) return false;
        if (interationToken != null ? !interationToken.equals(other.interationToken) : other.interationToken != null) return false;
        if (iteration != null ? !iteration.equals(other.iteration) : other.iteration != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (activation != null ? activation.hashCode() : 0);
        result = 31 * result + (interationToken != null ? interationToken.hashCode() : 0);
        result = 31 * result + (iteration != null ? iteration.hashCode() : 0);

        return result;
    }

    public static <T extends FocusType> void copyFromJAXB(FocusType jaxb, RFocus<T> repo, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setInterationToken(jaxb.getIterationToken());
        repo.setIteration(jaxb.getIteration());

        repo.getLinkRef().addAll(
                RUtil.safeListReferenceToSet(jaxb.getLinkRef(), prismContext, repo, RReferenceOwner.USER_ACCOUNT));

        for (AssignmentType assignment : jaxb.getAssignment()) {
            RAssignment rAssignment = new RAssignment(repo, RAssignmentOwner.FOCUS);
            RAssignment.copyFromJAXB(assignment, rAssignment, jaxb, prismContext);

            repo.getAssignments().add(rAssignment);
        }

        if (jaxb.getActivation() != null) {
            RActivation activation = new RActivation();
            RActivation.copyFromJAXB(jaxb.getActivation(), activation, prismContext);
            repo.setActivation(activation);

        }
    }

    public static <T extends FocusType> void copyToJAXB(RFocus<T> repo, FocusType jaxb, PrismContext prismContext,
                                  Collection<SelectorOptions<GetOperationOptions>> options) throws
            DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext, options);

        jaxb.setIteration(repo.getIteration());
        jaxb.setIterationToken(repo.getInterationToken());

        if (SelectorOptions.hasToLoadPath(FocusType.F_LINK_REF, options)) {
            List linkRefs = RUtil.safeSetReferencesToList(repo.getLinkRef(), prismContext);
            if (!linkRefs.isEmpty()) {
                jaxb.getLinkRef().addAll(linkRefs);
            }
        }

        if (SelectorOptions.hasToLoadPath(FocusType.F_ASSIGNMENT, options)) {
            for (RAssignment rAssignment : repo.getAssignment()) {
                jaxb.getAssignment().add(rAssignment.toJAXB(prismContext));
            }
        }

        if (repo.getActivation() != null) {
            jaxb.setActivation(repo.getActivation().toJAXB(prismContext));
        }
    }
}
