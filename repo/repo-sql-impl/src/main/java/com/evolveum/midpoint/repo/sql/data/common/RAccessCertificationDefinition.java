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
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collection;

@Entity
@Table(name = RAccessCertificationDefinition.TABLE_NAME,
        uniqueConstraints = @UniqueConstraint(name = "uc_acc_cert_definition_name", columnNames = {"name_norm"}))
@ForeignKey(name = "fk_acc_cert_definition")
public class RAccessCertificationDefinition extends RObject<AccessCertificationDefinitionType> {

    public static final String TABLE_NAME = "m_acc_cert_definition";

    private RPolyString name;
    private String handlerUri;
    private REmbeddedReference ownerRef;
    private XMLGregorianCalendar lastCampaignStartedTimestamp;
    private XMLGregorianCalendar lastCampaignClosedTimestamp;
//    private String campaignSchedulingInterval;

    @Embedded
    public RPolyString getName() {
        return name;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    public String getHandlerUri() {
        return handlerUri;
    }

    @Embedded
    public REmbeddedReference getOwnerRef() {
        return ownerRef;
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

    public void setOwnerRef(REmbeddedReference ownerRef) {
        this.ownerRef = ownerRef;
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

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (handlerUri != null ? !handlerUri.equals(that.handlerUri) : that.handlerUri != null) return false;
        if (ownerRef != null ? !ownerRef.equals(that.ownerRef) : that.ownerRef != null) return false;
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
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (handlerUri != null ? handlerUri.hashCode() : 0);
        return result;
    }

    public static void copyFromJAXB(AccessCertificationDefinitionType jaxb, RAccessCertificationDefinition repo,
                                    PrismContext prismContext, IdGeneratorResult generatorResult)
            throws DtoTranslationException {

        RObject.copyFromJAXB(jaxb, repo, prismContext, generatorResult);

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
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