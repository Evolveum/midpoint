/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;

/**
 * @author katka
 *
 */
public class PrismContainerPanelContext<C extends Containerable> extends ItemPanelContext<C, PrismContainerWrapper<C>>{

	public PrismContainerPanelContext(IModel<PrismContainerWrapper<C>> itemWrapper) {
		super(itemWrapper);
		// TODO Auto-generated constructor stub
	}

}
