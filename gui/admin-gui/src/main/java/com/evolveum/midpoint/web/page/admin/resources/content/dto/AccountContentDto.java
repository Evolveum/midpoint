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

package com.evolveum.midpoint.web.page.admin.resources.content.dto;

import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class AccountContentDto extends Selectable {

    private String accountOid;
    private String accountName;

    private List<ResourceAttribute<?>> identifiers;
    private SynchronizationSituationType situation;

    private String ownerOid;
    private String ownerName;

    public String getAccountName() {
        return accountName;
    }

    public String getAccountOid() {
        return accountOid;
    }

    public List<ResourceAttribute<?>> getIdentifiers() {
        if (identifiers == null) {
            identifiers = new ArrayList<ResourceAttribute<?>>();
        }
        return identifiers;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getOwnerOid() {
        return ownerOid;
    }

    public SynchronizationSituationType getSituation() {
        return situation;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setAccountOid(String accountOid) {
        this.accountOid = accountOid;
    }

    public void setIdentifiers(List<ResourceAttribute<?>> identifiers) {
        this.identifiers = identifiers;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setSituation(SynchronizationSituationType situation) {
        this.situation = situation;
    }
}
