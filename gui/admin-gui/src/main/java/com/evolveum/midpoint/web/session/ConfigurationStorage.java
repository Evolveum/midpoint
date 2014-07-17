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

package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.page.admin.configuration.dto.AccountDetailsSearchDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugSearchDto;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ConfigurationStorage implements Serializable {

    private DebugSearchDto debugSearchDto;
    private AccountDetailsSearchDto accountSearchDto;

    private ObjectPaging debugSearchPaging;
    private ObjectPaging accountDetailsPaging;

    public DebugSearchDto getDebugSearchDto() {
        if (debugSearchDto == null) {
            debugSearchDto = new DebugSearchDto();
            debugSearchDto.setType(ObjectTypes.SYSTEM_CONFIGURATION);
        }
        return debugSearchDto;
    }

    public void setDebugSearchDto(DebugSearchDto debugSearchDto) {
        this.debugSearchDto = debugSearchDto;
    }

    public AccountDetailsSearchDto getAccountSearchDto() {
        if(accountSearchDto == null){
            accountSearchDto = new AccountDetailsSearchDto();
        }

        return accountSearchDto;
    }

    public void setAccountSearchDto(AccountDetailsSearchDto accountSearchDto) {
        this.accountSearchDto = accountSearchDto;
    }

    public ObjectPaging getDebugSearchPaging() {
        return debugSearchPaging;
    }

    public void setDebugSearchPaging(ObjectPaging debugSearchPaging) {
        this.debugSearchPaging = debugSearchPaging;
    }

    public ObjectPaging getAccountDetailsPaging() {
        return accountDetailsPaging;
    }

    public void setAccountDetailsPaging(ObjectPaging accountDetailsPaging) {
        this.accountDetailsPaging = accountDetailsPaging;
    }
}
