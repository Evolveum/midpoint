/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.gui.api.model;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * 
 * TODO: refactor for lazy loading
 * 
 * @author semancik
 */
public class DisplayNameModel implements IModel<String> {
	private static final long serialVersionUID = 1L;

	private String name;
	
	public DisplayNameModel(AbstractRoleType role) {
		PolyStringType displayName = role.getDisplayName();
		if (displayName == null) {
			displayName = role.getName();
		}
		if (displayName == null) {
			name = "";
		} else {
			name = displayName.getOrig();
		}
	}

	@Override
	public void detach() {
		// TODO Auto-generated method stub
	}

	@Override
	public String getObject() {
		return name;
	}

	@Override
	public void setObject(String object) {
		this.name = object;
	}
	
}
