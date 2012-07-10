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

import java.io.Serializable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;

/**
 * @author mserbak
 */
public class SubmitAccountProvider implements Serializable {
	private PrismObject account;
	private String name;
	private String resourceName;

	public SubmitAccountProvider(PrismObject account) {
		this.account = account;
	}

	public String getName() {
		return WebMiscUtil.getName(account) == null ? "unknown" : WebMiscUtil.getName(account);
		
	}

	public String getResourceName() {
		PrismReference reference = account.findReference(ResourceObjectShadowType.F_RESOURCE_REF);
		if (reference == null || reference.isEmpty()) {
            return "unknown";
        }
		return WebMiscUtil.getName(reference.getValue().getObject());		
	}

}
