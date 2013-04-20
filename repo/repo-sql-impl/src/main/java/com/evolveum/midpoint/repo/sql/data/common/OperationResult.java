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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;

import java.io.Serializable;

/**
 * This interface helps handling and translation from and to entity classes like {@link ROperationResult}
 * to {@link com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType}.
 *
 * @author lazyman
 */
public interface OperationResult extends Serializable {

    String getParams();

    String getPartialResults();

    ROperationResultStatus getStatus();

    Long getToken();

    String getDetails();

    String getLocalizedMessage();

    String getMessage();

    String getMessageCode();

    String getOperation();

    void setParams(String params);

    void setPartialResults(String partialResults);

    void setStatus(ROperationResultStatus status);

    void setToken(Long token);

    void setDetails(String details);

    void setLocalizedMessage(String message);

    void setMessage(String message);

    void setMessageCode(String messageCode);

    void setOperation(String operation);
}
