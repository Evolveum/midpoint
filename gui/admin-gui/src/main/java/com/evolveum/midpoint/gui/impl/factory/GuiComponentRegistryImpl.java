package com.evolveum.midpoint.gui.impl.factory;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.component.prism.ValueWrapper;

public class GuiComponentRegistryImpl implements GuiComponentRegistry {

	List<GuiComponentFactory> guiComponentFactories = new ArrayList<>();
	
	@Override
	public void addToRegistry(GuiComponentFactory factory) {
		guiComponentFactories.add(factory);
	}

	@Override
	public <T> GuiComponentFactory findFactory(ValueWrapper<T> valueWrapper) {
		return guiComponentFactories.stream().filter(f -> f.match(valueWrapper)).findFirst().get();
	}

}
