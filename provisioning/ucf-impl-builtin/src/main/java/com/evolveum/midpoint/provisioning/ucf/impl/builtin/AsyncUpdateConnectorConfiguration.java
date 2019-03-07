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
package com.evolveum.midpoint.provisioning.ucf.impl.builtin;

import com.evolveum.midpoint.provisioning.ucf.api.ConfigurationProperty;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageSourceConfigurationType;

/**
 *
 */
public class AsyncUpdateConnectorConfiguration {

	private MessageSourceConfigurationType messageSource;
	private ExpressionType transformExpression;

	@ConfigurationProperty
	public MessageSourceConfigurationType getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSourceConfigurationType messageSource) {
		this.messageSource = messageSource;
	}

	@ConfigurationProperty
	public ExpressionType getTransformExpression() {
		return transformExpression;
	}

	public void setTransformExpression(ExpressionType transformExpression) {
		this.transformExpression = transformExpression;
	}
}
