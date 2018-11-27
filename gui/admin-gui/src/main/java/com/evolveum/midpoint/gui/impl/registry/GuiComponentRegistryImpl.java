package com.evolveum.midpoint.gui.impl.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;

public class GuiComponentRegistryImpl implements GuiComponentRegistry {

	List<GuiComponentFactory> guiComponentFactories = new ArrayList<>();
	
	@Override
	public void addToRegistry(GuiComponentFactory factory) {
		guiComponentFactories.add(factory);
	}

	@Override
	public <T> GuiComponentFactory findFactory(ValueWrapper<T> valueWrapper) {
		
		Optional<GuiComponentFactory> opt = guiComponentFactories.stream().filter(f -> f.match(valueWrapper)).findFirst();
		if (!opt.isPresent()) {
			return null;
		}
		
		return opt.get();
	}
	
	

}
