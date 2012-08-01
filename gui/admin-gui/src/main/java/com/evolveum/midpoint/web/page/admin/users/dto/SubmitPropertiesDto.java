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

import com.evolveum.midpoint.prism.PrismPropertyValue;

/**
 * @author mserbak
 */

public class SubmitPropertiesDto implements Serializable {
	private PrismPropertyValue submitedPropertie;
	private SubmitPropertiesStatus status;

	public SubmitPropertiesDto(PrismPropertyValue submitedPropertie, SubmitPropertiesStatus status) {
		this.submitedPropertie = submitedPropertie;
		this.status = status;
	}

	public PrismPropertyValue getSubmitedPropertie() {
		return submitedPropertie;
	}

	public SubmitPropertiesStatus getStatus() {
		return status;
	}
	
}
