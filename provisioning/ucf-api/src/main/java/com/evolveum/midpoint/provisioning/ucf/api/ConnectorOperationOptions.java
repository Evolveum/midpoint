/**
 * Copyright (c) 2018 Evolveum
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

import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;

/**
 * @author semancik
 *
 */
public class ConnectorOperationOptions {
	
	/**
	 * Run the operations on resource using the specified identity.
	 * Provided identification should identify valid, active account.
	 */
	private ResourceObjectIdentification runAsIdentification;

	public ResourceObjectIdentification getRunAsIdentification() {
		return runAsIdentification;
	}

	public void setRunAsIdentification(ResourceObjectIdentification runAsIdentification) {
		this.runAsIdentification = runAsIdentification;
	}
}
