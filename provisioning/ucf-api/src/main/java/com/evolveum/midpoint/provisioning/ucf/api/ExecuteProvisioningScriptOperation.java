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
package com.evolveum.midpoint.provisioning.ucf.api;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BeforeAfterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CriticalityType;

/**
 *
 * @author Radovan Semancik
 */
public class ExecuteProvisioningScriptOperation extends Operation {

	private static final int DEBUG_MAX_CODE_LENGTH = 32;

	private List<ExecuteScriptArgument> argument;

	private boolean connectorHost;
	private boolean resourceHost;

	private String textCode;
	private String language;

	private BeforeAfterType scriptOrder;

	private CriticalityType criticality;

	public ExecuteProvisioningScriptOperation() {

	}

	public List<ExecuteScriptArgument> getArgument() {
		if (argument == null){
			argument = new ArrayList<>();
		}
		return argument;
	}

	public boolean isConnectorHost() {
		return connectorHost;
	}

	public void setConnectorHost(boolean connectorHost) {
		this.connectorHost = connectorHost;
	}

	public boolean isResourceHost() {
		return resourceHost;
	}

	public void setResourceHost(boolean resourceHost) {
		this.resourceHost = resourceHost;
	}

	public String getTextCode() {
		return textCode;
	}

	public void setTextCode(String textCode) {
		this.textCode = textCode;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public BeforeAfterType getScriptOrder() {
		return scriptOrder;
	}

	public void setScriptOrder(BeforeAfterType scriptOrder) {
		this.scriptOrder = scriptOrder;
	}

	public CriticalityType getCriticality() {
		return criticality;
	}

	public void setCriticality(CriticalityType criticality) {
		this.criticality = criticality;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((argument == null) ? 0 : argument.hashCode());
		result = prime * result + (connectorHost ? 1231 : 1237);
		result = prime * result + ((criticality == null) ? 0 : criticality.hashCode());
		result = prime * result + ((language == null) ? 0 : language.hashCode());
		result = prime * result + (resourceHost ? 1231 : 1237);
		result = prime * result + ((scriptOrder == null) ? 0 : scriptOrder.hashCode());
		result = prime * result + ((textCode == null) ? 0 : textCode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ExecuteProvisioningScriptOperation other = (ExecuteProvisioningScriptOperation) obj;
		if (argument == null) {
			if (other.argument != null) {
				return false;
			}
		} else if (!argument.equals(other.argument)) {
			return false;
		}
		if (connectorHost != other.connectorHost) {
			return false;
		}
		if (criticality != other.criticality) {
			return false;
		}
		if (language == null) {
			if (other.language != null) {
				return false;
			}
		} else if (!language.equals(other.language)) {
			return false;
		}
		if (resourceHost != other.resourceHost) {
			return false;
		}
		if (scriptOrder != other.scriptOrder) {
			return false;
		}
		if (textCode == null) {
			if (other.textCode != null) {
				return false;
			}
		} else if (!textCode.equals(other.textCode)) {
			return false;
		}
		return true;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		SchemaDebugUtil.indentDebugDump(sb, indent);
		sb.append("Script execution ");
		sb.append(toStringInternal());
		return sb.toString();
	}

	@Override
	public String toString() {
		return "ExecuteProvisioningScriptOperation(" + toStringInternal() + ")";
	}

	private String toStringInternal() {
		StringBuilder sb = new StringBuilder();
		if (connectorHost) {
			sb.append("on connector ");
		}
		if (resourceHost) {
			sb.append("on resource ");
		}
		sb.append(scriptOrder);
		sb.append(" : ");
		if (textCode.length() <= DEBUG_MAX_CODE_LENGTH) {
			sb.append(textCode);
		} else {
			sb.append(textCode.substring(0, DEBUG_MAX_CODE_LENGTH));
			sb.append(" ...(truncated)...");
		}
		if (criticality != null) {
			sb.append(" criticality=").append(criticality.value());
		}
		return sb.toString();
	}
}
