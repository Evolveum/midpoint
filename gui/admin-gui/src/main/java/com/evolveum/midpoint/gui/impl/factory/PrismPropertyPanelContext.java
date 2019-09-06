/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import java.util.Collection;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

/**
 * @author katka
 *
 */
public class PrismPropertyPanelContext<T> extends ItemPanelContext<T, PrismPropertyWrapper<T>>{
	
	
	public PrismPropertyPanelContext(IModel<PrismPropertyWrapper<T>> itemWrapper) {
		super(itemWrapper);
	}

	
	public Collection<? extends DisplayableValue<T>> getAllowedValues() {
		return unwrapWrapperModel().getAllowedValues();
	}

	public LookupTableType getPredefinedValues() {
		return unwrapWrapperModel().getPredefinedValues();
	}
	
	public boolean hasValueEnumerationRef() {
		return unwrapWrapperModel().getValueEnumerationRef() != null;
	}
}
