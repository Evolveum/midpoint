/**
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.model.api.interaction;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

/**
 * @author skublik
 */
public class DashboardWidget {
	
	private DisplayType display;
	private String numberMessage;
	private DashboardWidgetType widget;
	
	public DashboardWidget() {
	}
	
	public DashboardWidget(DashboardWidgetType widget, DisplayType display, String numberMessage) {
		this.widget = widget;
		this.display = display;
		this.numberMessage = numberMessage;
	}
	
	public DisplayType getDisplay() {
		return display;
	}
	
	public void setDisplay(DisplayType display) {
		this.display = display;
	}
	
	public String getNumberMessage() {
		return numberMessage;
	}
	
	public void setNumberMessage(String numberMessage) {
		this.numberMessage = numberMessage;
	}
	
	public DashboardWidgetType getWidget() {
		return widget;
	}
	
	public void setWidget(DashboardWidgetType widget) {
		this.widget = widget;
	}
	
	public String getLabel() {
		if(getDisplay() != null && getDisplay().getLabel() != null) {
        	return getDisplay().getLabel().toString();
		} else {
			return getWidget().getIdentifier();
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{widgetIdentifier:").append(widget == null ? null : widget.getIdentifier())
		.append(", numberMessage:").append(numberMessage)
		.append(", display:").append(display).append("}");
		return sb.toString();
	}
}
