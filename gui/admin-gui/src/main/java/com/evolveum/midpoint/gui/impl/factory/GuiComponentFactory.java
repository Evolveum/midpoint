package com.evolveum.midpoint.gui.impl.factory;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;

public interface GuiComponentFactory {

//	public void register();
	
	public <T> boolean match(ValueWrapper<T> valueWrapper);
	
	public <T> Panel createPanel(String id, IModel<ValueWrapper<T>> model, String baseExpression);
}
