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

package com.evolveum.midpoint.web.page.admin.resources.content.dto;

import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.xml.ns._public.common.common_2.SynchronizationSituationType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class AccountContentDto implements Serializable {

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
