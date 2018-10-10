package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;

public interface GuiComponentRegistry {

	public void addToRegistry(GuiComponentFactory factory);
	
	public <T> GuiComponentFactory findFactory(ValueWrapper<T> valueWrapper);
	
}
