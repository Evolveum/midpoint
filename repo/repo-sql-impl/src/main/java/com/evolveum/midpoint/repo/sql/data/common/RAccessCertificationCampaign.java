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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationCase;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RAccessCertificationCampaignState;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = RAccessCertificationCampaign.TABLE_NAME,
        uniqueConstraints = @UniqueConstraint(name = "uc_acc_cert_campaign_name", columnNames = {"name_norm"}),
        indexes = {
                @Index(name = "iCertCampaignNameOrig", columnList = "name_orig")
        }
)
@Persister(impl = MidPointJoinedPersister.class)
@ForeignKey(name = "fk_acc_cert_campaign")
public class RAccessCertificationCampaign extends RObject<AccessCertificationCampaignType> {

    public static final String TABLE_NAME = "m_acc_cert_campaign";

    private RPolyString nameCopy;
    private REmbeddedReference definitionRef;
    private Set<RAccessCertificationCase> cases;

    private REmbeddedReference ownerRefCampaign;
    private String handlerUri;
    private XMLGregorianCalendar start;
    private XMLGregorianCalendar end;
    private RAccessCertificationCampaignState state;
    private Integer stageNumber;

    @JaxbName(localPart = "name")
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
    public REmbeddedReference getDefinitionRef() {
        return definitionRef;
    }

    @Transient
    public Set<RAccessCertificationCase> getCase() {
        if (cases == null) {
            cases = new HashSet<>();
        }
        return cases;
    }

    @JaxbName(localPart = "ownerRef")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "relation", column = @Column(name = "ownerRef_relation", length = RUtil.COLUMN_LENGTH_QNAME)),
            @AttributeOverride(name = "targetOid", column = @Column(name = "ownerRef_targetOid", length = RUtil.COLUMN_LENGTH_OID)),
            @AttributeOverride(name = "type", column = @Column(name = "ownerRef_type"))
    })
    public REmbeddedReference getOwnerRefCampaign() {       // name changed because of collision with RAbstractRole.ownerRef
        return ownerRefCampaign;
    }

    public String getHandlerUri() {
        return handlerUri;
    }

    @JaxbName(localPart = "startTimestamp")
    @Column(name = "startTimestamp")
    public XMLGregorianCalendar getStart() {
        return start;
    }

    @JaxbName(localPart = "endTimestamp")
    @Column(name = "endTimestamp")
    public XMLGregorianCalendar getEnd() {
        return end;
    }

    public RAccessCertificationCampaignState getState() {
        return state;
    }

    public Integer getStageNumber() {
        return stageNumber;
    }

    public void setDefinitionRef(REmbeddedReference definitionRef) {
        this.definitionRef = definitionRef;
    }

    public void setCase(Set<RAccessCertificationCase> cases) {
        this.cases = cases;
    }

    public void setOwnerRefCampaign(REmbeddedReference ownerRefCampaign) {
        this.ownerRefCampaign = ownerRefCampaign;
    }

    public void setHandlerUri(String handlerUri) {
        this.handlerUri = handlerUri;
    }

    public void setStart(XMLGregorianCalendar start) {
        this.start = start;
    }

    public void setEnd(XMLGregorianCalendar end) {
        this.end = end;
    }

    public void setState(RAccessCertificationCampaignState state) {
        this.state = state;
    }

    public void setStageNumber(Integer stageNumber) {
        this.stageNumber = stageNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RAccessCertificationCampaign)) return false;
        if (!super.equals(o)) return false;

        RAccessCertificationCampaign that = (RAccessCertificationCampaign) o;

        if (nameCopy != null ? !nameCopy.equals(that.nameCopy) : that.nameCopy != null) return false;
        if (definitionRef != null ? !definitionRef.equals(that.definitionRef) : that.definitionRef != null)
            return false;
        if (ownerRefCampaign != null ? !ownerRefCampaign.equals(that.ownerRefCampaign) : that.ownerRefCampaign
                != null) return false;
        if (handlerUri != null ? !handlerUri.equals(that.handlerUri) : that.handlerUri != null) return false;
        if (start != null ? !start.equals(that.start) : that.start != null) return false;
        if (end != null ? !end.equals(that.end) : that.end != null) return false;
        if (state != that.state) return false;
        return !(stageNumber != null ? !stageNumber.equals(that.stageNumber) : that.stageNumber != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (nameCopy != null ? nameCopy.hashCode() : 0);
        result = 31 * result + (handlerUri != null ? handlerUri.hashCode() : 0);
        result = 31 * result + (start != null ? start.hashCode() : 0);
        result = 31 * result + (end != null ? end.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (stageNumber != null ? stageNumber.hashCode() : 0);
        return result;
    }

    public static void copyFromJAXB(AccessCertificationCampaignType jaxb, RAccessCertificationCampaign repo,
            RepositoryContext repositoryContext, IdGeneratorResult generatorResult)
            throws DtoTranslationException {

        RObject.copyFromJAXB(jaxb, repo, repositoryContext, generatorResult);
        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setDefinitionRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getDefinitionRef(), repositoryContext.prismContext));

        List<AccessCertificationCaseType> cases = jaxb.getCase();
        if (!cases.isEmpty()) {
            for (AccessCertificationCaseType case1 : cases) {
                RAccessCertificationCase rCase = RAccessCertificationCase.toRepo(repo, case1, repositoryContext);
                rCase.setTransient(generatorResult.isTransient(case1.asPrismContainerValue()));     // redundant?
                repo.getCase().add(rCase);
            }
        }

        repo.setOwnerRefCampaign(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getOwnerRef(), repositoryContext.prismContext));
        repo.setHandlerUri(jaxb.getHandlerUri());
        repo.setStart(jaxb.getStartTimestamp());
        repo.setEnd(jaxb.getEndTimestamp());
        repo.setState(RUtil.getRepoEnumValue(jaxb.getState(), RAccessCertificationCampaignState.class));
        repo.setStageNumber(jaxb.getStageNumber());
    }
}