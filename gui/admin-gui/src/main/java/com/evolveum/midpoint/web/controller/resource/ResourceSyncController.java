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
package com.evolveum.midpoint.web.controller.resource;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.web.controller.TemplateController;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("resourceSync")
@Scope("session")
public class ResourceSyncController implements Serializable {

	public static final String PAGE_NAVIGATION = "/resource/synchronization?faces-redirect=true";
	public static final String NAVIGATION_LEFT = "leftSyncStatus";
	private static final long serialVersionUID = 7495585784483264032L;
	@Autowired(required = true)
	private TemplateController template;

	public String backPerformed() {
		template.setSelectedLeftId("leftResourceDetails");

		return ResourceDetailsController.PAGE_NAVIGATION;
	}
}
