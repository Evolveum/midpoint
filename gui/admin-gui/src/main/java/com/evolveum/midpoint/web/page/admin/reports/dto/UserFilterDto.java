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

package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.io.Serializable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

/**
 * @author mserbak
 */
public class UserFilterDto implements Serializable {
	private String searchText;
    private Boolean activated = null;
    private PrismObject<ResourceType> resource = null;
    
    
	public String getSearchText() {
		return searchText;
	}
	
	public Boolean isActivated() {
		return activated;
	}
	
	public PrismObject<ResourceType> getResource() {
		return resource;
	}
	
	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}
	
	public void setActivated(Boolean activated) {
		this.activated = activated;
	}
	
	public void setResource(PrismObject<ResourceType> resource) {
		this.resource = resource;
	} 
}
