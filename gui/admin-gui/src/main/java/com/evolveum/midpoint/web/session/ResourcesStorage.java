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
import com.evolveum.midpoint.web.page.admin.resources.content.dto.AccountContentSearchDto;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceSearchDto;

import java.io.Serializable;

/**
 *  @author shood
 * */
public class ResourcesStorage implements Serializable{

    /**
     *  DTO used for search purposes in {@link com.evolveum.midpoint.web.page.admin.resources.PageResources}
     * */
    private ResourceSearchDto resourceSearch;

    /**
     *  Paging DTO used in table on page {@link com.evolveum.midpoint.web.page.admin.resources.PageResources}
     * */
    private ObjectPaging resourcePaging;

    /**
     *  DTO used for search in {@link com.evolveum.midpoint.web.page.admin.resources.content.PageContentAccounts}
     * */
    private AccountContentSearchDto accountContentSearch;

    /**
     *  Paging DTO used in table on page {@link com.evolveum.midpoint.web.page.admin.resources.content.PageContentAccounts}
     * */
    private ObjectPaging accountContentPaging;

    public ResourceSearchDto getResourceSearch() {
        return resourceSearch;
    }

    public void setResourceSearch(ResourceSearchDto resourceSearch) {
        this.resourceSearch = resourceSearch;
    }

    public ObjectPaging getResourcePaging() {
        return resourcePaging;
    }

    public void setResourcePaging(ObjectPaging resourcePaging) {
        this.resourcePaging = resourcePaging;
    }

    public AccountContentSearchDto getAccountContentSearch() {
        return accountContentSearch;
    }

    public void setAccountContentSearch(AccountContentSearchDto accountContentSearch) {
        this.accountContentSearch = accountContentSearch;
    }

    public ObjectPaging getAccountContentPaging() {
        return accountContentPaging;
    }

    public void setAccountContentPaging(ObjectPaging accountContentPaging) {
        this.accountContentPaging = accountContentPaging;
    }
}
