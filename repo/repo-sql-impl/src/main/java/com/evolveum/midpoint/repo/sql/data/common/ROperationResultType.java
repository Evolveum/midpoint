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
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LocalizedMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ParamsType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Type;

import javax.persistence.*;

/**
 * @author lazyman
 */
@Entity
@Table(name = "operation_result")
@org.hibernate.annotations.GenericGenerator(name = "owner-primary-key", strategy = "foreign",
        parameters = {@org.hibernate.annotations.Parameter(name = "property", value = "owner")
        })
public class ROperationResultType {

    private String id;
    private RObjectType owner;
    private String operation;
    private OperationResultStatusType status;
    private Long token;
    private String messageCode;
    private String message;
    private String details;

    private String localizedMessage;
    private String params;
    private String partialResults;

    @OneToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn
    public RObjectType getOwner() {
        return owner;
    }

    public void setOwner(RObjectType owner) {
        this.owner = owner;
    }

    @Id
    @GeneratedValue(generator = "owner-primary-key")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDetails() {
        return details;
    }

    public String getLocalizedMessage() {
        return localizedMessage;
    }

    public String getMessage() {
        return message;
    }

    public String getMessageCode() {
        return messageCode;
    }

    public String getOperation() {
        return operation;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getParams() {
        return params;
    }

    @Type(type = "org.hibernate.type.MaterializedClobType")
    public String getPartialResults() {
        return partialResults;
    }

    @Enumerated(EnumType.ORDINAL)
    public OperationResultStatusType getStatus() {
        return status;
    }

    @Column(nullable = true)
    public Long getToken() {
        return token;
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

    public void setStatus(OperationResultStatusType status) {
        this.status = status;
    }

    public void setToken(Long token) {
        this.token = token;
    }

    public static void copyToJAXB(ROperationResultType repo, OperationResultType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        jaxb.setDetails(repo.getDetails());
        jaxb.setMessage(repo.getMessage());
        jaxb.setMessageCode(repo.getMessageCode());
        jaxb.setOperation(repo.getOperation());
        jaxb.setStatus(repo.getStatus());
        jaxb.setToken(repo.getToken());

        try {
            jaxb.setLocalizedMessage(RUtil.toJAXB(repo.getLocalizedMessage(), LocalizedMessageType.class, prismContext));
            jaxb.setParams(RUtil.toJAXB(repo.getParams(), ParamsType.class, prismContext));

//            if (StringUtils.isNotEmpty(repo.getPartialResults())) {
//                JAXBElement<OperationResultType> result = (JAXBElement<OperationResultType>)
//                        JAXBUtil.unmarshal(repo.getPartialResults());
//                jaxb.getPartialResults().addAll(result.getValue().getPartialResults());
//            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(OperationResultType jaxb, ROperationResultType repo,
            PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        repo.setDetails(jaxb.getDetails());
        repo.setMessage(jaxb.getMessage());
        repo.setMessageCode(jaxb.getMessageCode());
        repo.setOperation(jaxb.getOperation());
        repo.setStatus(jaxb.getStatus());
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
        ROperationResultType.copyToJAXB(this, result, prismContext);
        return result;
    }
}
