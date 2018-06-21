/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO that contains provisioning run-time configuration and diagnostic information.
 *
 * All information contained in this class are meant for information purposes only.
 * They are not meant to be used by a machine or algorithm, they are meant to be displayed
 * to a human user.
 *
 * @author Radovan Semancik
 * @author mederly
 *
 */
public class ProvisioningDiag implements Serializable {

    // TODO some information about connector frameworks used etc

	/**
	 * Additional repository information that do not fit the structured data above.
	 * May be anything that the implementations thinks is important.
     *
     * Currently used as a hack to display connId version
	 */
	private List<LabeledString> additionalDetails = new ArrayList<>();

	public List<LabeledString> getAdditionalDetails() {
		return additionalDetails;
	}

	public void setAdditionalDetails(List<LabeledString> additionalDetails) {
		this.additionalDetails = additionalDetails;
	}

	@Override
	public String toString() {
		return "ProvisioningDiag(additionalDetails=" + additionalDetails + ")";
	}


}
