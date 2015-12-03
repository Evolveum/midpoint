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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationCase;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RAccessCertificationCampaignState;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = RAccessCertificationCampaign.TABLE_NAME,
        uniqueConstraints = @UniqueConstraint(name = "uc_acc_cert_campaign_name", columnNames = {"name_norm"}))
@ForeignKey(name = "fk_acc_cert_campaign")
public class RAccessCertificationCampaign extends RObject<AccessCertificationCampaignType> {

    public static final String TABLE_NAME = "m_acc_cert_campaign";

    private RPolyString name;
    private REmbeddedReference definitionRef;
    private Set<RAccessCertificationCase> cases;

    private REmbeddedReference ownerRef;
    private String handlerUri;
    private XMLGregorianCalendar start;
    private XMLGregorianCalendar end;
    private RAccessCertificationCampaignState state;
    private Integer stageNumber;

    @Embedded
    public RPolyString getName() {
        return name;
    }

    @Embedded
    public REmbeddedReference getDefinitionRef() {
        return definitionRef;
    }

    @OneToMany(mappedBy = RAccessCertificationCase.F_OWNER, orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAccessCertificationCase> getCase() {
        if (cases == null) {
            cases = new HashSet<>();
        }
        return cases;
    }

    @Embedded
    public REmbeddedReference getOwnerRef() {
        return ownerRef;
    }

    public String getHandlerUri() {
        return handlerUri;
    }

    @Column(name = "startTimestamp")
    public XMLGregorianCalendar getStart() {
        return start;
    }

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

    public void setName(RPolyString name) {
        this.name = name;
    }

    public void setDefinitionRef(REmbeddedReference definitionRef) {
        this.definitionRef = definitionRef;
    }

    public void setCase(Set<RAccessCertificationCase> cases) {
        this.cases = cases;
    }

    public void setOwnerRef(REmbeddedReference ownerRef) {
        this.ownerRef = ownerRef;
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

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (definitionRef != null ? !definitionRef.equals(that.definitionRef) : that.definitionRef != null)
            return false;
        if (ownerRef != null ? !ownerRef.equals(that.ownerRef) : that.ownerRef != null) return false;
        if (handlerUri != null ? !handlerUri.equals(that.handlerUri) : that.handlerUri != null) return false;
        if (start != null ? !start.equals(that.start) : that.start != null) return false;
        if (end != null ? !end.equals(that.end) : that.end != null) return false;
        if (state != that.state) return false;
        return !(stageNumber != null ? !stageNumber.equals(that.stageNumber) : that.stageNumber != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (handlerUri != null ? handlerUri.hashCode() : 0);
        result = 31 * result + (start != null ? start.hashCode() : 0);
        result = 31 * result + (end != null ? end.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (stageNumber != null ? stageNumber.hashCode() : 0);
        return result;
    }

    public static void copyFromJAXB(AccessCertificationCampaignType jaxb, RAccessCertificationCampaign repo,
                                    PrismContext prismContext, IdGeneratorResult generatorResult)
            throws DtoTranslationException {

        RObject.copyFromJAXB(jaxb, repo, prismContext, generatorResult);
        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setDefinitionRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getDefinitionRef(), prismContext));

        List<AccessCertificationCaseType> cases = jaxb.getCase();
        if (!cases.isEmpty()) {
            for (AccessCertificationCaseType case1 : cases) {
                case1.setCampaignRef(ObjectTypeUtil.createObjectRef(jaxb));
                RAccessCertificationCase rCase = RAccessCertificationCase.toRepo(repo, case1, generatorResult, prismContext);
                rCase.setTransient(generatorResult.isTransient(case1.asPrismContainerValue()));
                repo.getCase().add(rCase);
            }
        }

        repo.setOwnerRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getOwnerRef(), prismContext));
        repo.setHandlerUri(jaxb.getHandlerUri());
        repo.setStart(jaxb.getStart());
        repo.setEnd(jaxb.getEnd());
        repo.setState(RUtil.getRepoEnumValue(jaxb.getState(), RAccessCertificationCampaignState.class));
        repo.setStageNumber(jaxb.getStageNumber());
    }

    @Override
    public AccessCertificationCampaignType toJAXB(PrismContext prismContext,
                                                  Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {

        AccessCertificationCampaignType object = new AccessCertificationCampaignType();
        RUtil.revive(object, prismContext);
        RAccessCertificationCampaign.copyToJAXB(this, object, prismContext, options);

        return object;
    }
}