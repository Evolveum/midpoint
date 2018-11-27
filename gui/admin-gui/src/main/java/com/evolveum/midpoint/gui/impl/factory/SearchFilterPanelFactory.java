package com.evolveum.midpoint.gui.impl.factory;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

@Component
public class SearchFilterPanelFactory extends AbstractGuiComponentFactory {

	private static transient Trace LOGGER = TraceManager.getTrace(SearchFilterPanelFactory.class);
	
	@Autowired GuiComponentRegistry registry;
	
	public void register() {
		registry.addToRegistry(this);
	}
	
	@Override
	public <T> boolean match(ValueWrapper<T> valueWrapper) {
		return SearchFilterType.COMPLEX_TYPE.equals(valueWrapper.getItem().getItemDefinition().getTypeName());
	}

	@Override
	public <T> Panel createPanel(PanelContext<T> panelCtx) {
			return new AceEditorPanel(panelCtx.getComponentId(), null, new SearchFilterTypeModel((IModel) panelCtx.getBaseModel(), panelCtx.getPrismContext()));
		}

	
	class SearchFilterTypeModel implements IModel<String> {
	
		private static final long serialVersionUID = 1L;
		
		private IModel<ValueWrapper<SearchFilterType>> baseModel;
		private PrismContext prismCtx;
		
		public SearchFilterTypeModel(IModel<ValueWrapper<SearchFilterType>> valueWrapper, PrismContext prismCtx) {
			this.baseModel = valueWrapper;
			this.prismCtx = prismCtx;
		}

		@Override
		public void detach() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getObject() {
			try {
				PrismValue value = baseModel.getObject().getValue();
				if (value == null || value.isEmpty()) {
					return null;
				}
				
				return prismCtx.xmlSerializer().serialize(value);
			} catch (SchemaException e) {
				// TODO handle!!!!
				LoggingUtils.logException(LOGGER, "Cannot serialize filter", e);
//				getSession().error("Cannot serialize filter");
			}
			return null;
		}

		@Override
		public void setObject(String object) {
			if (StringUtils.isBlank(object)) {
				return;
			}
			
			try {
				SearchFilterType filter = prismCtx.parserFor(object).parseRealValue(SearchFilterType.class);
				((PrismPropertyValue<SearchFilterType>) baseModel.getObject().getValue()).setValue(filter);
			} catch (SchemaException e) {
				// TODO handle!!!!
				LoggingUtils.logException(LOGGER, "Cannot parse filter", e);
//				getSession().error("Cannot parse filter");
			}
			
		}
	}
	
	
}
