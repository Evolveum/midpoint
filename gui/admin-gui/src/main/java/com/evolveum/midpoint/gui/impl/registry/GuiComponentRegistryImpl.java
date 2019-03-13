package com.evolveum.midpoint.gui.impl.registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.web.component.prism.ValueWrapperOld;

public class GuiComponentRegistryImpl implements GuiComponentRegistry {

	List<GuiComponentFactory> guiComponentFactories = new ArrayList<>();
	
	@Override
	public void addToRegistry(GuiComponentFactory factory) {
		guiComponentFactories.add(factory);
		
		Comparator<? super GuiComponentFactory> comparator = 
				(f1,f2) -> {
					
					Integer f1Order = f1.getOrder();
					Integer f2Order = f2.getOrder();
					
					if (f1Order == null) {
						if (f2Order != null) {
							return 1; 
						}
						return 0;
					}
					
					if (f2Order == null) {
						if (f1Order != null) {
							return -1;
						}
					}
					
					return Integer.compare(f1Order, f2Order);
					
				};
		
		guiComponentFactories.sort(comparator);
		
	}

	@Override
	public <T> GuiComponentFactory findFactory(ItemWrapper itemWrapper) {
		
		
		Optional<GuiComponentFactory> opt = guiComponentFactories.stream().filter(f -> f.match(itemWrapper)).findFirst();
		if (!opt.isPresent()) {
			return null;
		}
		
		return opt.get();
	}
	
	

}
