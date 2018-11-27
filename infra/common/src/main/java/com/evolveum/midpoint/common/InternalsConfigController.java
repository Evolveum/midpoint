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
package com.evolveum.midpoint.common;

import javax.annotation.PostConstruct;

import org.apache.commons.configuration.Configuration;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.schema.internals.InternalsConfig;

/**
 * @author semancik
 *
 */
public class InternalsConfigController {
	
	private MidpointConfiguration midpointConfiguration;
	
	public MidpointConfiguration getMidpointConfiguration() {
		return midpointConfiguration;
	}

	public void setMidpointConfiguration(MidpointConfiguration midpointConfiguration) {
		this.midpointConfiguration = midpointConfiguration;
	}

	@PostConstruct
	public void init() {
		Configuration internalsConfig = midpointConfiguration.getConfiguration(MidpointConfiguration.INTERNALS_CONFIGURATION);
		InternalsConfig.set(internalsConfig);
	}
}
