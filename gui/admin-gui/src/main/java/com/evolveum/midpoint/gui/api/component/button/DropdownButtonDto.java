/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.gui.api.component.button;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.web.component.data.column.InlineMenuable;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;

public class DropdownButtonDto implements Serializable, InlineMenuable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String info;
	private String icon;
	private String label;
	
	private List<InlineMenuItem> items;

	public DropdownButtonDto(String info, String icon, String label, List<InlineMenuItem> items) {
		this.info = info;
		this.icon = icon;
		this.label = label;
		this.items = items;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	

	@Override
	public List<InlineMenuItem> getMenuItems() {
		return items;
	}

}
