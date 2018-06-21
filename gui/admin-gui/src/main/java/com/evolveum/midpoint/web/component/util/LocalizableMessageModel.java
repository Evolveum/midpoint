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

package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageType;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * TODO is this right approach?
 * @author mederly
 */
public class LocalizableMessageModel implements Serializable, IModel<String> {

	private final IModel<LocalizableMessageType> model;
	private final Component component;

	public LocalizableMessageModel(IModel<LocalizableMessageType> model, Component component) {
		this.model = model;
		this.component = component;
	}

	@Override
	public String getObject() {
		LocalizableMessageType message = model.getObject();
		if (message != null) {
			return WebComponentUtil.resolveLocalizableMessage(message, component);
		} else {
			return null;
		}
	}

	@Override
	public void setObject(String object) {
		// silently ignored
	}

	@Override
	public void detach() {
	}
}
