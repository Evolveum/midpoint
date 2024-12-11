/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import jakarta.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;
import org.hibernate.annotations.Type;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RSimpleEmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;

@Entity
@Table(name = RAccessCertificationDefinition.TABLE_NAME,
        uniqueConstraints = @UniqueConstraint(name = "uc_acc_cert_definition_name", columnNames = { "name_norm" }),
        indexes = {
                @Index(name = "iCertDefinitionNameOrig", columnList = "name_orig"),
        }
)
@Persister(impl = MidPointJoinedPersister.class)
@ForeignKey(name = "fk_acc_cert_definition")
@DynamicUpdate
public class RAccessCertificationDefinition extends RObject {

    public static final String TABLE_NAME = "m_acc_cert_definition";

    private RPolyString nameCopy;
    private String handlerUri;
    private RSimpleEmbeddedReference ownerRefDefinition;
    private XMLGregorianCalendar lastCampaignStartedTimestamp;
    private XMLGregorianCalendar lastCampaignClosedTimestamp;

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

    public String getHandlerUri() {
        return handlerUri;
    }

    @JaxbName(localPart = "ownerRef")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "relation", column = @Column(name = "ownerRef_relation", length = RUtil.COLUMN_LENGTH_QNAME)),
            @AttributeOverride(name = "targetOid", column = @Column(name = "ownerRef_targetOid", length = RUtil.COLUMN_LENGTH_OID)),
            @AttributeOverride(name = "targetType", column = @Column(name = "ownerRef_targetType"))
    })
    public RSimpleEmbeddedReference getOwnerRefDefinition() {
        return ownerRefDefinition;
    }

    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getLastCampaignStartedTimestamp() {
        return lastCampaignStartedTimestamp;
    }

    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getLastCampaignClosedTimestamp() {
        return lastCampaignClosedTimestamp;
    }

    public void setHandlerUri(String handlerUri) {
        this.handlerUri = handlerUri;
    }

    public void setOwnerRefDefinition(RSimpleEmbeddedReference ownerRefDefinition) {
        this.ownerRefDefinition = ownerRefDefinition;
    }

    public void setLastCampaignStartedTimestamp(XMLGregorianCalendar lastCampaignStartedTimestamp) {
        this.lastCampaignStartedTimestamp = lastCampaignStartedTimestamp;
    }

    public void setLastCampaignClosedTimestamp(XMLGregorianCalendar lastCampaignClosedTimestamp) {
        this.lastCampaignClosedTimestamp = lastCampaignClosedTimestamp;
    }

    // dynamically called
    public static void copyFromJAXB(AccessCertificationDefinitionType jaxb, RAccessCertificationDefinition repo,
            RepositoryContext repositoryContext, IdGeneratorResult generatorResult)
            throws DtoTranslationException {

        copyAssignmentHolderInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setHandlerUri(jaxb.getHandlerUri());
        repo.setOwnerRefDefinition(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getOwnerRef(), repositoryContext.relationRegistry));
        repo.setLastCampaignStartedTimestamp(jaxb.getLastCampaignStartedTimestamp());
        repo.setLastCampaignClosedTimestamp(jaxb.getLastCampaignClosedTimestamp());
    }
}
