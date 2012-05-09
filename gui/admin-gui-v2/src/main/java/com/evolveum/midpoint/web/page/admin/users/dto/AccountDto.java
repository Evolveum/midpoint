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

package com.evolveum.midpoint.web.page.admin.users.dto;

import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import org.apache.commons.lang.Validate;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class AccountDto implements Serializable {

    public static enum AccountDtoStatus {
        ADDED, DELETED, NOT_MODIFIED;
    }

    private ObjectWrapper object;
    private AccountDtoStatus status;

    public AccountDto(ObjectWrapper object, AccountDtoStatus status) {
        setObject(object);
        setStatus(status);
    }

    public ObjectWrapper getObject() {
        return object;
    }

    public void setObject(ObjectWrapper object) {
        Validate.notNull(object, "Object wrapper must not be null.");
        this.object = object;
    }

    public AccountDtoStatus getStatus() {
        return status;
    }

    public void setStatus(AccountDtoStatus status) {
        Validate.notNull(status, "Status must not be null.");
        this.status = status;
    }
}
