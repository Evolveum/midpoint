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
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collection;

@Entity
@Table(name = RAccessCertificationDefinition.TABLE_NAME,
        uniqueConstraints = @UniqueConstraint(name = "uc_acc_cert_definition_name", columnNames = {"name_norm"}))
@Persister(impl = MidPointJoinedPersister.class)
@ForeignKey(name = "fk_acc_cert_definition")
public class RAccessCertificationDefinition extends RObject<AccessCertificationDefinitionType> {

    public static final String TABLE_NAME = "m_acc_cert_definition";

    private RPolyString nameCopy;
    private String handlerUri;
    private REmbeddedReference ownerRefDefinition;
    private XMLGregorianCalendar lastCampaignStartedTimestamp;
    private XMLGregorianCalendar lastCampaignClosedTimestamp;
//    private String campaignSchedulingInterval;

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

    public String getHandlerUri() {
        return handlerUri;
    }

    @JaxbName(localPart = "ownerRef")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "relation", column = @Column(name = "ownerRef_relation", length = RUtil.COLUMN_LENGTH_QNAME)),
            @AttributeOverride(name = "targetOid", column = @Column(name = "ownerRef_targetOid", length = RUtil.COLUMN_LENGTH_OID)),
            @AttributeOverride(name = "type", column = @Column(name = "ownerRef_type"))
    })
    public REmbeddedReference getOwnerRefDefinition() {
        return ownerRefDefinition;
    }

    public XMLGregorianCalendar getLastCampaignStartedTimestamp() {
        return lastCampaignStartedTimestamp;
    }

    public XMLGregorianCalendar getLastCampaignClosedTimestamp() {
        return lastCampaignClosedTimestamp;
    }

//    public String getCampaignSchedulingInterval() {
//        return campaignSchedulingInterval;
//    }

    public void setHandlerUri(String handlerUri) {
        this.handlerUri = handlerUri;
    }

    public void setOwnerRefDefinition(REmbeddedReference ownerRefDefinition) {
        this.ownerRefDefinition = ownerRefDefinition;
    }

    public void setLastCampaignStartedTimestamp(XMLGregorianCalendar lastCampaignStartedTimestamp) {
        this.lastCampaignStartedTimestamp = lastCampaignStartedTimestamp;
    }

    public void setLastCampaignClosedTimestamp(XMLGregorianCalendar lastCampaignClosedTimestamp) {
        this.lastCampaignClosedTimestamp = lastCampaignClosedTimestamp;
    }

//    public void setCampaignSchedulingInterval(String campaignSchedulingInterval) {
//        this.campaignSchedulingInterval = campaignSchedulingInterval;
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RAccessCertificationDefinition)) return false;
        if (!super.equals(o)) return false;

        RAccessCertificationDefinition that = (RAccessCertificationDefinition) o;

        if (nameCopy != null ? !nameCopy.equals(that.nameCopy) : that.nameCopy != null) return false;
        if (handlerUri != null ? !handlerUri.equals(that.handlerUri) : that.handlerUri != null) return false;
        if (ownerRefDefinition != null ? !ownerRefDefinition.equals(that.ownerRefDefinition) : that.ownerRefDefinition != null) return false;
        if (lastCampaignStartedTimestamp != null ? !lastCampaignStartedTimestamp.equals(that.lastCampaignStartedTimestamp) : that.lastCampaignStartedTimestamp != null)
            return false;
        if (lastCampaignClosedTimestamp != null ? !lastCampaignClosedTimestamp.equals(that.lastCampaignClosedTimestamp) : that.lastCampaignClosedTimestamp != null)
            return false;
        //return !(campaignSchedulingInterval != null ? !campaignSchedulingInterval.equals(that.campaignSchedulingInterval) : that.campaignSchedulingInterval != null);
        return true;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (nameCopy != null ? nameCopy.hashCode() : 0);
        result = 31 * result + (handlerUri != null ? handlerUri.hashCode() : 0);
        return result;
    }

    public static void copyFromJAXB(AccessCertificationDefinitionType jaxb, RAccessCertificationDefinition repo,
            RepositoryContext repositoryContext, IdGeneratorResult generatorResult)
            throws DtoTranslationException {

        RObject.copyFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setHandlerUri(jaxb.getHandlerUri());
        repo.setOwnerRefDefinition(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getOwnerRef(), repositoryContext.prismContext));
        repo.setLastCampaignStartedTimestamp(jaxb.getLastCampaignStartedTimestamp());
        repo.setLastCampaignClosedTimestamp(jaxb.getLastCampaignClosedTimestamp());
    }

    @Override
    public AccessCertificationDefinitionType toJAXB(PrismContext prismContext,
                                                    Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {

        AccessCertificationDefinitionType object = new AccessCertificationDefinitionType();
        RUtil.revive(object, prismContext);
        RAccessCertificationDefinition.copyToJAXB(this, object, prismContext, options);

        return object;
    }
}