package com.evolveum.midpoint.gui.impl.factory;

import org.apache.wicket.markup.html.panel.Panel;

import com.evolveum.midpoint.web.component.prism.ValueWrapper;

public interface GuiComponentFactory {

//	public void register();
	
	<T> boolean match(ValueWrapper<T> valueWrapper);
	
	<T> Panel createPanel(PanelContext<T> panelCtx);
	
//	Panel build(PanelContext panelContext);
	
}
