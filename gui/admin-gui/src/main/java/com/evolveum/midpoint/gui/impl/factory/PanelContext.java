package com.evolveum.midpoint.gui.impl.factory;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.ClassUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.ValueWrapperImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.ReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapperOld;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

public class PanelContext<T> {

	private String componentId;
	
	private Component parentComponent;
	
	private IModel<ItemWrapper> itemWrapper;
	private ItemRealValueModel<T> realValueModel;
	
	private Form form;
	
	private FeedbackPanel feedbackPanel;
	
	public PanelContext(IModel<ItemWrapper> itemWrapper) {
		this.itemWrapper = itemWrapper;
	}
	
	private ItemWrapper getItemWrapper() {
		return itemWrapper.getObject();
	}
	
	public PageBase getPageBase() {
		return (PageBase) parentComponent.getPage();
	}
	
	public String getComponentId() {
		return componentId;
	}
	
	public PrismReferenceValue getValueEnumerationRef() {
		return getItemWrapper().getValueEnumerationRef();
	}
	
	public Collection<T> getAllowedValues() {
		if (getItemWrapper() instanceof PrismPropertyDefinition) {
			return ((PrismPropertyDefinition) itemWrapper.getItemDefinition()).getAllowedValues();
		}
		
		return null;
	}
	
	public PrismContext getPrismContext() {
		return getItemWrapper().getPrismContext();
	}
	
	public LookupTableType getPredefinedValues() {
		if (getItemWrapper() instanceof PropertyWrapper) {
			return ((PropertyWrapper) getItemWrapper()).getPredefinedValues();
		}
		return null;
	}
	
	public boolean isPropertyReadOnly() {
		return getItemWrapper().isReadOnly();
	}
	
	public QName getDefinitionName() {
		return getItemWrapper().getItemDefinition().getName();
	}
	
	public Component getParentComponent() {
		return parentComponent;
	}
	
	public Class<T> getTypeClass() {
		Class<T> clazz = getItemWrapper().getTypeClass();
		if (clazz == null) {
			clazz = getPrismContext().getSchemaRegistry().determineClassForType(itemWrapper.getTypeName());
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
	
	public void setRealValueModel(IModel<PrismValueWrapper<T>> valueWrapper) {
		this.realValueModel = new ItemRealValueModel<>(valueWrapper);
	}
	
	public void setItemWrapper(ItemWrapper itemWrapper) {
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
