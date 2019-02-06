package com.evolveum.midpoint.gui.impl.factory;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.ClassUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.ReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

public class PanelContext<T> {

	private String componentId;
	
	private Component parentComponent;
	
	private ItemWrapperOld itemWrapper;
	private ItemRealValueModel<T> realValueModel;
	
	private Form form;
	
	private FeedbackPanel feedbackPanel;
	
	public PageBase getPageBase() {
		return (PageBase) parentComponent.getPage();
	}
	
	public String getComponentId() {
		return componentId;
	}
	
	public PrismReferenceValue getValueEnumerationRef() {
		return itemWrapper.getItemDefinition().getValueEnumerationRef();
	}
	
	public Collection<T> getAllowedValues() {
		if (itemWrapper.getItemDefinition() instanceof PrismPropertyDefinition) {
			return ((PrismPropertyDefinition) itemWrapper.getItemDefinition()).getAllowedValues();
		}
		
		return null;
	}
	
	public PrismContext getPrismContext() {
		return itemWrapper.getItemDefinition().getPrismContext();
	}
	
	public LookupTableType getPredefinedValues() {
		if (itemWrapper instanceof PropertyWrapper) {
			return ((PropertyWrapper) itemWrapper).getPredefinedValues();
		}
		return null;
	}
	
	public boolean isPropertyReadOnly() {
		return itemWrapper.isReadonly();
	}
	
	public QName getDefinitionName() {
		return itemWrapper.getItemDefinition().getName();
	}
	
	public Component getParentComponent() {
		return parentComponent;
	}
	
	public Class<T> getTypeClass() {
		Class<T> clazz = itemWrapper.getItemDefinition().getTypeClass();
		if (clazz == null) {
			clazz = getPrismContext().getSchemaRegistry().determineClassForType(itemWrapper.getItemDefinition().getTypeName());
		}
		if (clazz != null && clazz.isPrimitive()) {
			clazz = ClassUtils.primitiveToWrapper(clazz);
		}
		return clazz;
	}
	
	public ItemRealValueModel<T> getRealValueModel() {
		return realValueModel;
	}
	
	public Form getForm() {
		return form;
	}
	
	public FeedbackPanel getFeedbackPanel() {
		return feedbackPanel;
	}
	
	public void setRealValueModel(ValueWrapper<T> valueWrapper) {
		this.realValueModel = new ItemRealValueModel<>(valueWrapper);
	}
	
	public void setItemWrapper(ItemWrapperOld itemWrapper) {
		this.itemWrapper = itemWrapper;
	}
	
	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}
	
	public void setParentComponent(Component parentComponent) {
		this.parentComponent = parentComponent;
	}
	
	public void setForm(Form form) {
		this.form = form;
	}
	
public void setFeedbackPanel(FeedbackPanel feedbackPanel) {
		this.feedbackPanel = feedbackPanel;
	}
	
	public ObjectFilter getFilter() {
		if (!(itemWrapper instanceof ReferenceWrapper)) {
			return null;
		}
		return ((ReferenceWrapper) itemWrapper).getFilter();
	}

	public List<QName> getTargetTypes() {
		if (!(itemWrapper instanceof ReferenceWrapper)) {
			return null;
		}
		
		return ((ReferenceWrapper) itemWrapper.getItemDefinition()).getTargetTypes();
	}
	
	public QName getTargetTypeName() {
		if (!(itemWrapper instanceof ReferenceWrapper)) {
			return null;
		}
		
		return ((PrismReferenceDefinition) itemWrapper.getItemDefinition()).getTargetTypeName();
	}
}
