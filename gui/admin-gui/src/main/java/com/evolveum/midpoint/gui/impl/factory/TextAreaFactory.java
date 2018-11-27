package com.evolveum.midpoint.gui.impl.factory;

import javax.annotation.PostConstruct;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.web.component.input.TextAreaPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

@Component
public class TextAreaFactory extends AbstractGuiComponentFactory {

	@Autowired private GuiComponentRegistry registry;
	
//	@Override
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}

	@Override
	public <T> boolean match(ValueWrapper<T> valueWrapper) {
		return FocusType.F_DESCRIPTION.equals(valueWrapper.getItem().getItemDefinition().getName());
	}
	
	@Override
	public <T> Panel createPanel(PanelContext<T> panelCtx) {
		return new TextAreaPanel<>(panelCtx.getComponentId(), new PropertyModel<>(panelCtx.getBaseModel(), panelCtx.getBaseExpression()), 0);
	}

	
}
