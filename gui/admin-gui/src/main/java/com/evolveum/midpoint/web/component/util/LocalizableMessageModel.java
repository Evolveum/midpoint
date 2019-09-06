/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
