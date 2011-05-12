/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.model.test;

import com.evolveum.midpoint.web.model.AccountShadowDto;
import com.evolveum.midpoint.web.model.AccountShadowManager;
import com.evolveum.midpoint.web.model.PagingDto;
import com.evolveum.midpoint.web.model.PropertyAvailableValues;
import com.evolveum.midpoint.web.model.PropertyChange;
import com.evolveum.midpoint.web.model.UserDto;
import com.evolveum.midpoint.web.model.WebModelException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @author katuska
 */
public class AccountShadowTypeManagerMock implements AccountShadowManager{

    Map<String, AccountShadowDto> accountTypeList = new HashMap<String, AccountShadowDto>();

    private final Class constructAccountShadowType;

    public AccountShadowTypeManagerMock(Class constructAccountShadowType) {
        this.constructAccountShadowType = constructAccountShadowType;
    }

    @Override
    public UserType listOwner(String oid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<AccountShadowDto> list() {
        return accountTypeList.values();
    }

    @Override
    public AccountShadowDto get(String oid, PropertyReferenceListType resolve) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AccountShadowDto create() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String add(AccountShadowDto newObject) {
        accountTypeList.clear();
       newObject.setOid(UUID.randomUUID().toString());
        accountTypeList.put(newObject.getOid(), newObject);
        return newObject.getOid();
    }

    @Override
    public Set<PropertyChange> submit(AccountShadowDto changedObject) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(String oid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<PropertyAvailableValues> getPropertyAvailableValues(String oid, List<String> properties) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<UserDto> list(PagingDto pagingDto) throws WebModelException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
