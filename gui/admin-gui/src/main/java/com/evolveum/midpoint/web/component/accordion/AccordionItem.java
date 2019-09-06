/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.accordion;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.model.IModel;

/**
 * Don't use this component, it will be gradually removed from gui.
 * Maybe replaced with something else later. [lazyman]
 */
@Deprecated
public class AccordionItem extends Border {

	private static final long serialVersionUID = 1L;

	public AccordionItem(String id, IModel<String> headerText) {
		super(id);
		addToBorder(new Label("headerText", headerText));
	}
}
