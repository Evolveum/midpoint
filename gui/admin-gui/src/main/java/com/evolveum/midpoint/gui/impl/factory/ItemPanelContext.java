package com.evolveum.midpoint.gui.impl.factory;

import java.io.Serializable;

import org.apache.commons.lang.ClassUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;

public abstract class ItemPanelContext<T, IW extends ItemWrapper> implements Serializable {

	private String componentId;
	
	private Component parentComponent;
	
	private IModel<IW> itemWrapper;
	private ItemRealValueModel<T> realValueModel;
	
	private FeedbackPanel feedbackPanel;
	
	private Form<?> form;
	
	public ItemPanelContext(IModel<IW> itemWrapper) {
		this.itemWrapper = itemWrapper;
	}
	
	public IW unwrapWrapperModel() {
		return itemWrapper.getObject();
	}

	public PageBase getPageBase() {
		return (PageBase) parentComponent.getPage();
	}
	
	public String getComponentId() {
		return componentId;
	}
	
	public PrismReferenceValue getValueEnumerationRef() {
		return unwrapWrapperModel().getValueEnumerationRef();
	}
	
	
	public IModel<IW> getItemWrapperModel() {
		return itemWrapper;
	}
	
	public PrismContext getPrismContext() {
		return unwrapWrapperModel().getPrismContext();
	}
	
	
	
	public boolean isPropertyReadOnly() {
		return unwrapWrapperModel().isReadOnly();
	}
	
	public ItemName getDefinitionName() {
		return unwrapWrapperModel().getName();
	}
	
	public Component getParentComponent() {
		return parentComponent;
	}
	
	public Class<T> getTypeClass() {
		Class<T> clazz = unwrapWrapperModel().getTypeClass();
		if (clazz == null) {
			clazz = getPrismContext().getSchemaRegistry().determineClassForType(unwrapWrapperModel().getTypeName());
		}
		if (clazz != null && clazz.isPrimitive()) {
			clazz = ClassUtils.primitiveToWrapper(clazz);
		}
		return clazz;
	}
	
	public ItemRealValueModel<T> getRealValueModel() {
		return realValueModel;
	}
	
	
	public FeedbackPanel getFeedbackPanel() {
		return feedbackPanel;
	}
	
	public <V extends PrismValue> void setRealValueModel(IModel<? extends PrismValueWrapper<T, V>> valueWrapper) {
		this.realValueModel = new ItemRealValueModel<T>(valueWrapper);
	}
	

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}
	
	public void setParentComponent(Component parentComponent) {
		this.parentComponent = parentComponent;
	}
	

	
public void setFeedbackPanel(FeedbackPanel feedbackPanel) {
		this.feedbackPanel = feedbackPanel;
	}
	
/**
 * @return the form
 */
public Form<?> getForm() {
	return form;
}

/**
 * @param form the form to set
 */
public void setForm(Form<?> form) {
	this.form = form;
}
	
}
