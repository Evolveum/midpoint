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

package com.evolveum.midpoint.web.model.dto;

import java.io.Serializable;

/**
 * 
 * @author Katuska
 */
public class GuiTestResultDto implements Serializable {

	private static final long serialVersionUID = -11744078835543869L;
	private String configurationValidation;
	private String connectionInitialization;
	private String connectorConnection;
	private String connectionSanity;
	private String connectionSchema;
	private String extraTestName;
	private String extraTestResult;

	public GuiTestResultDto() {
	}

	public String getConfigurationValidation() {
		return configurationValidation;
	}

	public void setConfigurationValidation(String configurationValidation) {
		this.configurationValidation = configurationValidation;
	}

	public String getConnectionSanity() {
		return connectionSanity;
	}

	public void setConnectionSanity(String connectionSanity) {
		this.connectionSanity = connectionSanity;
	}

	public String getConnectionSchema() {
		return connectionSchema;
	}

	public void setConnectionSchema(String connectionSchema) {
		this.connectionSchema = connectionSchema;
	}

	public String getConnectorConnection() {
		return connectorConnection;
	}

	public void setConnectorConnection(String connectorConnection) {
		this.connectorConnection = connectorConnection;
	}

	public String getExtraTestName() {
		return extraTestName;
	}

	public void setExtraTestName(String extraTestName) {
		this.extraTestName = extraTestName;
	}

	public String getExtraTestResult() {
		return extraTestResult;
	}

	public void setExtraTestResult(String extraTestResult) {
		this.extraTestResult = extraTestResult;
	}

	public String getConnectionInitialization() {
		return connectionInitialization;
	}

	public void setConnectionInitialization(String connectionInitialization) {
		this.connectionInitialization = connectionInitialization;
	}

}
