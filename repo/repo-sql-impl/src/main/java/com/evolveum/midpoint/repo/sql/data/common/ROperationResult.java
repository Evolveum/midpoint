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
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.*;

/**
 * @author lazyman
 */
@Entity
public class ROperationResult implements OperationResult {

    //owner
    private RObject owner;
    private String ownerOid;
    private Short ownerId;
    //other fields
    private ROperationResultStatus status;
    private Long token;
    private String messageCode;

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
    public Short getOwnerId() {
        if (ownerId == null && owner != null) {
            ownerId = owner.getId();
        }
        return ownerId;
    }

    @Id
    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Enumerated(EnumType.ORDINAL)
    public ROperationResultStatus getStatus() {
        return status;
    }

    @Column(nullable = true)
    public Long getToken() {
        return token;
    }

    public void setOwner(RObject owner) {
        this.owner = owner;
    }

    public String getMessageCode() {
        return messageCode;
    }

    public void setOwnerId(Short ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setMessageCode(String messageCode) {
        this.messageCode = messageCode;
    }

    public void setStatus(ROperationResultStatus status) {
        this.status = status;
    }

    public void setToken(Long token) {
        this.token = token;
    }

    @Transient
    @Override
    public String getParams() {
        return null;
    }

    @Transient
    @Override
    public String getContext() {
        return null;
    }

    @Transient
    @Override
    public String getReturns() {
        return null;
    }

    @Transient
    @Override
    public String getPartialResults() {
        return null;
    }

    @Transient
    @Override
    public String getDetails() {
        return null;
    }

    @Transient
    @Override
    public String getLocalizedMessage() {
        return null;
    }

    @Transient
    @Override
    public String getMessage() {
        return null;
    }

    @Transient
    @Override
    public String getOperation() {
        return null;
    }

    @Override
    public void setParams(String params) {

    }

    @Override
    public void setContext(String context) {

    }

    @Override
    public void setReturns(String returns) {

    }

    @Override
    public void setPartialResults(String partialResults) {

    }

    @Override
    public void setDetails(String details) {

    }

    @Override
    public void setLocalizedMessage(String message) {

    }

    @Override
    public void setMessage(String message) {

    }

    @Override
    public void setOperation(String operation) {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ROperationResult that = (ROperationResult) o;

        if (messageCode != null ? !messageCode.equals(that.messageCode) : that.messageCode != null) return false;
        if (status != that.status) return false;
        if (token != null ? !token.equals(that.token) : that.token != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = status != null ? status.hashCode() : 0;
        result = 31 * result + (token != null ? token.hashCode() : 0);
        result = 31 * result + (messageCode != null ? messageCode.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(ROperationResult repo, OperationResultType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RUtil.copyResultToJAXB(repo, jaxb, prismContext);
    }

    public static void copyFromJAXB(OperationResultType jaxb, ROperationResult repo, PrismContext prismContext) throws
            DtoTranslationException {
        RUtil.copyResultFromJAXB(jaxb, repo, prismContext);
    }

    public OperationResultType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        OperationResultType result = new OperationResultType();
        ROperationResult.copyToJAXB(this, result, prismContext);
        return result;
    }
}
