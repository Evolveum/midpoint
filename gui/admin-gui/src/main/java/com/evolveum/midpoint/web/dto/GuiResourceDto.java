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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.dto;

import com.evolveum.midpoint.web.model.ObjectStage;
import com.evolveum.midpoint.web.model.ResourceDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * 
 * @author katuska
 */
public class GuiResourceDto extends ResourceDto {

	private static final long serialVersionUID = -7504862931524553291L;
	private String connectorUsed;
	private String connectorVersion;
	private boolean selected;

	public GuiResourceDto(ResourceType object) {
		super(object);
	}

	public GuiResourceDto(ObjectStage stage) {
		super(stage);
	}

	public GuiResourceDto() {
	}

	public String getConnectorUsed() {
		return connectorUsed;
	}

	public void setConnectorUsed(String connectorUsed) {
		this.connectorUsed = connectorUsed;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String getConnectorVersion() {
		return connectorVersion;
	}

	public void setConnectorVersion(String connectorVersion) {
		this.connectorVersion = connectorVersion;
	}

	@Override
	public String toString() {
		return super.getName();
	}

}
