package com.evolveum.midpoint.gui.impl.factory;

import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.web.model.LookupPropertyModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

@Component
public class TextPanelFactory extends AbstractGuiComponentFactory {

	@Autowired GuiComponentRegistry registry;

	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}

	@Override
	public <T> boolean match(ValueWrapper<T> valueWrapper) {
		QName type = valueWrapper.getItem().getItemDefinition().getTypeName();
		return SchemaConstants.T_POLY_STRING_TYPE.equals(type) || DOMUtil.XSD_STRING.equals(type) || DOMUtil.XSD_DURATION.equals(type);
	}

	@Override
	public <T> Panel createPanel(PanelContext<T> panelCtx) {
		
		LookupTableType lookupTable = panelCtx.getPredefinedValues();
		if (lookupTable == null) {
			return new TextPanel<>(panelCtx.getComponentId(),
					panelCtx.getRealValueModel(), panelCtx.getTypeClass());
		}
		
		
		return new AutoCompleteTextPanel<T>(panelCtx.getComponentId(),
				panelCtx.getRealValueModel(), panelCtx.getTypeClass(), false, lookupTable) {

			private static final long serialVersionUID = 1L;

			@Override
			public Iterator<T> getIterator(String input) {
				return (Iterator<T>) prepareAutoCompleteList(input, lookupTable).iterator();
			}
		};

		
		
	

	}

}
