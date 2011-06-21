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
 */
package com.evolveum.midpoint.web.model.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;

/**
 * 
 * @author lazyman
 * 
 */
public class SystemConfigurationDto extends ObjectDto<SystemConfigurationType> {

	private static final long serialVersionUID = -111217762624854724L;

	public SystemConfigurationDto() {
	}

	public SystemConfigurationDto(SystemConfigurationType systemConfiguration) {
		super(systemConfiguration);
	}
}
