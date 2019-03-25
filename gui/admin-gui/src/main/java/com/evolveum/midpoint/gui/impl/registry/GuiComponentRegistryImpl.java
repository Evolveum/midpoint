package com.evolveum.midpoint.gui.impl.registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.factory.ItemWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.PrismObjectWrapperFactoryImpl;
import com.evolveum.midpoint.gui.impl.factory.WrapperFactory;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.prism.ValueWrapperOld;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class GuiComponentRegistryImpl implements GuiComponentRegistry {

	List<GuiComponentFactory> guiComponentFactories = new ArrayList<>();
	
	Map<Class<? extends ItemWrapper<?, ?, ?,?>>, Class<?>> wrapperPanels = new HashMap<>(); 
	
	List<ItemWrapperFactory<?,?>> wrapperFactories = new ArrayList<>();
	
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
	
	public void registerWrapperPanel(Class<? extends ItemWrapper<?,?,?,?>> wrapperClass, Class<?> panelClass) {
		if (wrapperPanels.containsKey(wrapperClass)) {
			if (!panelClass.equals(wrapperPanels.get(wrapperClass))) {
				wrapperPanels.replace(wrapperClass, wrapperPanels.get(wrapperClass), panelClass);
				return;
			}
			return;
		}
		wrapperPanels.put(wrapperClass, panelClass);		
	}
	
	public <IW extends ItemWrapper<?,?,?,?>> Class<?> getPanelClass(Class<IW> wrapperClass) {
		return wrapperPanels.get(wrapperClass);
	}

	
	
	@Override
	public <T> GuiComponentFactory findValuePanelFactory(ItemWrapper itemWrapper) {
		
		
		Optional<GuiComponentFactory> opt = guiComponentFactories.stream().filter(f -> f.match(itemWrapper)).findFirst();
		if (!opt.isPresent()) {
			return null;
		}
		
		return opt.get();
	}
	
	public ItemWrapperFactory<?,?> findWrapperFactory(ItemDefinition<?> def) {
		Optional<ItemWrapperFactory<?,?>> opt = wrapperFactories.stream().filter(f -> f.match(def)).findFirst();
		if (!opt.isPresent()) {
			return null;
		}
		
		return opt.get();
	}
	
	public <O extends ObjectType> PrismObjectWrapperFactory<O> getObjectWrapperFactory(PrismObjectDefinition<O> objectDef) {
		return (PrismObjectWrapperFactory) findWrapperFactory(objectDef);
	}
	
//	@Override
	public void addToRegistry(ItemWrapperFactory factory) {
		wrapperFactories.add(factory);

		Comparator<? super ItemWrapperFactory> comparator = (f1, f2) -> {

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

		wrapperFactories.sort(comparator);

	}
	
	

}
