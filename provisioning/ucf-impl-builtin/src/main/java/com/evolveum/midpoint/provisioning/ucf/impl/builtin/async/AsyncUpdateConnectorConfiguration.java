/*
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
package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async;

import com.evolveum.midpoint.provisioning.ucf.api.ConfigurationProperty;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateSourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateSourcesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class AsyncUpdateConnectorConfiguration {

	private AsyncUpdateSourcesType sources;
	private ExpressionType transformExpression;

	@ConfigurationProperty
	public AsyncUpdateSourcesType getSources() {
		return sources;
	}

	public void setSources(AsyncUpdateSourcesType sources) {
		this.sources = sources;
	}

	@ConfigurationProperty
	public ExpressionType getTransformExpression() {
		return transformExpression;
	}

	public void setTransformExpression(ExpressionType transformExpression) {
		this.transformExpression = transformExpression;
	}

	public void validate() {
		getSingleSourceConfiguration();
	}

	public AsyncUpdateSourceType getSingleSourceConfiguration() {
		List<AsyncUpdateSourceType> allSources = new ArrayList<>();
		if (sources != null) {
			allSources.addAll(sources.getAmqp091());
			allSources.addAll(sources.getOther());
		}
		if (allSources.isEmpty()) {
			throw new IllegalStateException("No asynchronous update sources were configured");
		} else if (allSources.size() > 1) {
			throw new IllegalStateException("Multiple asynchronous update sources were configured. This is not supported yet.");
		} else {
			return allSources.get(0);
		}
	}

}
