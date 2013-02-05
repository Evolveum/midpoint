/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatusType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LocalizedMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ParamsType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author lazyman
 */
@Entity
public class ROperationResult implements Serializable {

    //owner
    private RObject owner;
    private String ownerOid;
    private Long ownerId;
    //other fields
    private String operation;
    private ROperationResultStatusType status;
    private Long token;
    private String messageCode;
    private String message;
    private String details;

    private String localizedMessage;
    private String params;
    private String partialResults;

    @ForeignKey(name = "fk_result_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "owner_oid", referencedColumnName = "oid"),
            @PrimaryKeyJoinColumn(name = "owner_id", referencedColumnName = "id")
    })
    public RObject getOwner() {
        return owner;
    }

    @Id
    @Column(name = "owner_id")
    public Long getOwnerId() {
        if (ownerId == null && owner != null) {
            ownerId = owner.getId();
        }
        return ownerId;
    }

    @Id
    @Column(name = "owner_oid", length = 36)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getParams() {
        return params;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getPartialResults() {
        return partialResults;
    }

    @Enumerated(EnumType.ORDINAL)
    public ROperationResultStatusType getStatus() {
        return status;
    }

    @Column(nullable = true)
    public Long getToken() {
        return token;
    }

    public void setOwner(RObject owner) {
        this.owner = owner;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getDetails() {
        return details;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getLocalizedMessage() {
        return localizedMessage;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getMessage() {
        return message;
    }

    public String getMessageCode() {
        return messageCode;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getOperation() {
        return operation;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setLocalizedMessage(String localizedMessage) {
        this.localizedMessage = localizedMessage;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setMessageCode(String messageCode) {
        this.messageCode = messageCode;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public void setPartialResults(String partialResults) {
        this.partialResults = partialResults;
    }

    public void setStatus(ROperationResultStatusType status) {
        this.status = status;
    }

    public void setToken(Long token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ROperationResult that = (ROperationResult) o;

        if (details != null ? !details.equals(that.details) : that.details != null) return false;
        if (localizedMessage != null ? !localizedMessage.equals(that.localizedMessage) : that.localizedMessage != null)
            return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        if (messageCode != null ? !messageCode.equals(that.messageCode) : that.messageCode != null) return false;
        if (operation != null ? !operation.equals(that.operation) : that.operation != null) return false;
        if (params != null ? !params.equals(that.params) : that.params != null) return false;
        if (partialResults != null ? !partialResults.equals(that.partialResults) : that.partialResults != null)
            return false;
        if (status != that.status) return false;
        if (token != null ? !token.equals(that.token) : that.token != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = operation != null ? operation.hashCode() : 0;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (token != null ? token.hashCode() : 0);
        result = 31 * result + (messageCode != null ? messageCode.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (details != null ? details.hashCode() : 0);
        result = 31 * result + (localizedMessage != null ? localizedMessage.hashCode() : 0);
        result = 31 * result + (params != null ? params.hashCode() : 0);
        result = 31 * result + (partialResults != null ? partialResults.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(ROperationResult repo, OperationResultType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        jaxb.setDetails(repo.getDetails());
        jaxb.setMessage(repo.getMessage());
        jaxb.setMessageCode(repo.getMessageCode());
        jaxb.setOperation(repo.getOperation());
        if (repo.getStatus() != null) {
            jaxb.setStatus(repo.getStatus().getStatus());
        }
        jaxb.setToken(repo.getToken());

        try {
            jaxb.setLocalizedMessage(RUtil.toJAXB(OperationResultType.class, new ItemPath(
                    OperationResultType.F_LOCALIZED_MESSAGE), repo.getLocalizedMessage(), LocalizedMessageType.class,
                    prismContext));
            jaxb.setParams(RUtil.toJAXB(OperationResultType.class, new ItemPath(OperationResultType.F_PARAMS),
                    repo.getParams(), ParamsType.class, prismContext));

            if (StringUtils.isNotEmpty(repo.getPartialResults())) {
                OperationResultType result = RUtil.toJAXB(repo.getPartialResults(), OperationResultType.class, prismContext);
                jaxb.getPartialResults().addAll(result.getPartialResults());
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(OperationResultType jaxb, ROperationResult repo, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        repo.setDetails(jaxb.getDetails());
        repo.setMessage(jaxb.getMessage());
        repo.setMessageCode(jaxb.getMessageCode());
        repo.setOperation(jaxb.getOperation());
        repo.setStatus(ROperationResultStatusType.toRepoType(jaxb.getStatus()));
        repo.setToken(jaxb.getToken());

        try {
            repo.setLocalizedMessage(RUtil.toRepo(jaxb.getLocalizedMessage(), prismContext));
            repo.setParams(RUtil.toRepo(jaxb.getParams(), prismContext));

            if (!jaxb.getPartialResults().isEmpty()) {
                OperationResultType result = new OperationResultType();
                result.getPartialResults().addAll(jaxb.getPartialResults());
                repo.setPartialResults(RUtil.toRepo(result, prismContext));
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public OperationResultType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        OperationResultType result = new OperationResultType();
        ROperationResult.copyToJAXB(this, result, prismContext);
        return result;
    }
}
