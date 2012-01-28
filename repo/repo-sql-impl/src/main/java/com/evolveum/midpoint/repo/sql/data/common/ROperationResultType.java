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

import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.Identifiable;
import com.evolveum.midpoint.repo.sql.jaxb.XOperationResultType;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.xml.bind.JAXBElement;
import java.util.List;

/**
 * TODO Maybe we need to store parent operation result as object (in more columns) and only
 * partial results as XML. That way results could be partially searchable (you can find
 * results with error statuses).
 *
 * @author lazyman
 */
@Entity
@Table(name = "operation_result")
public class ROperationResultType implements Identifiable {

    private long id;
    private String result;

    @Id
    @GeneratedValue
    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Type(type = "org.hibernate.type.MaterializedClobType")
    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public static void copyToJAXB(ROperationResultType repo, OperationResultType jaxb) throws DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        if (jaxb instanceof XOperationResultType) {
            XOperationResultType xRes = (XOperationResultType) jaxb;
            xRes.setId(repo.getId());
        }

        try {
            if (StringUtils.isNotEmpty(repo.getResult())) {
                JAXBElement<OperationResultType> result = (JAXBElement<OperationResultType>)
                        JAXBUtil.unmarshal(repo.getResult());

                OperationResultType resultType = result.getValue();
                jaxb.setDetails(resultType.getDetails());
                jaxb.setLocalizedMessage(resultType.getLocalizedMessage());
                jaxb.setMessage(resultType.getMessage());
                jaxb.setMessageCode(resultType.getMessageCode());
                jaxb.setOperation(resultType.getOperation());
                jaxb.setParams(resultType.getParams());
                jaxb.setStatus(resultType.getStatus());
                jaxb.setDetails(resultType.getDetails());
                jaxb.setToken(resultType.getToken());
                jaxb.getPartialResults().addAll(resultType.getPartialResults());
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(OperationResultType jaxb, ROperationResultType repo) throws
            DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        if (jaxb instanceof XOperationResultType) {
            XOperationResultType xRes = (XOperationResultType) jaxb;
            repo.setId(xRes.getId());
        }

        try {
            String result = JAXBUtil.marshalWrap(jaxb);
            repo.setResult(result);
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }
}
