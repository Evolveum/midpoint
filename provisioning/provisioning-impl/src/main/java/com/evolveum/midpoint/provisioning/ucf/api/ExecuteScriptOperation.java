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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptOrderType;

/**
 * 
 * @author Radovan Semancik
 */
public class ExecuteScriptOperation extends Operation {

	private static final int DEBUG_MAX_CODE_LENGTH = 32;

	private List<ExecuteScriptArgument> argument;

	private boolean connectorHost;
	private boolean resourceHost;

	private String textCode;
	private String language;

	private ScriptOrderType scriptOrder;
	
	public ExecuteScriptOperation() {

	}

	public List<ExecuteScriptArgument> getArgument() {
		if (argument == null){
			argument = new ArrayList<ExecuteScriptArgument>();
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

	public ScriptOrderType getScriptOrder() {
		return scriptOrder;
	}

	public void setScriptOrder(ScriptOrderType scriptOrder) {
		this.scriptOrder = scriptOrder;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		SchemaDebugUtil.indentDebugDump(sb, indent);
		sb.append("Script execution ");
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
		return sb.toString();
	}
}
