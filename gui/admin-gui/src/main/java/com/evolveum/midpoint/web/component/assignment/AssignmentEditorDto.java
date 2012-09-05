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

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class AssignmentEditorDto implements Serializable {

    public static final String F_DESCRIPTION = "description";
    public static final String F_EXTENSION = "extension";

    private AccountConstructionType accountConstruction;
    private PrismObject<ResourceType> resource;

    public AccountConstructionType getAccountConstruction() {
        if (accountConstruction == null) {
            accountConstruction = new AccountConstructionType();
        }
        return accountConstruction;
    }

    public void setAccountConstruction(AccountConstructionType accountConstruction) {
        this.accountConstruction = accountConstruction;
    }

    public String getDescription() {
        return getAccountConstruction().getDescription();
    }

    public void setDescription(String description) {
        getAccountConstruction().setDescription(description);
    }

    public PrismObject<ResourceType> getResource() {
        return resource;
    }

    public void setResource(PrismObject<ResourceType> resource) {
        this.resource = resource;
    }

    public String getExtension() {
        ExtensionType extension = getAccountConstruction().getExtension();
        return "not implemented yet...";
    }

    public void setExtension(String extension) {
        //todo implement
    }
}
