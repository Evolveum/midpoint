package com.evolveum.midpoint.gui.impl.factory;

import javax.annotation.PostConstruct;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.web.component.LockoutStatusPanel;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;

@Component
public class LockoutStatusPanelFactory implements GuiComponentFactory {

	@Autowired GuiComponentRegistry registry;
	
//	@Override
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}

	@Override
	public <T> boolean match(ValueWrapper<T> valueWrapper) {
		return ActivationType.F_LOCKOUT_STATUS.equals(valueWrapper.getItem().getItemDefinition().getName());
	}

	@Override
	public <T> Panel createPanel(String id, IModel<ValueWrapper<T>> model, String baseExpression) {
		return new LockoutStatusPanel(id, model.getObject(), new PropertyModel<>(model, baseExpression));
	}

}
