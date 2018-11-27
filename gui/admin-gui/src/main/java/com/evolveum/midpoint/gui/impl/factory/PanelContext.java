package com.evolveum.midpoint.gui.impl.factory;

import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

public class PanelContext<T> {

	private PageBase pageBase;
	private String baseExpression;
	private String componentId;
	private IModel<ValueWrapper<T>> baseModel;
	private ItemDefinition itemDefinition;
	
	private Component parentComponent;
	
	private LookupTableType predefinedValues;
	
	private ItemRealValueModel<T> realValueModel;
	
	public PageBase getPageBase() {
		return pageBase;
	}
	public String getBaseExpression() {
		return baseExpression;
	}
	public String getComponentId() {
		return componentId;
	}
	public IModel<ValueWrapper<T>> getBaseModel() {
		return baseModel;
	}
	
	public PrismReferenceValue getValueEnumerationRef() {
		return itemDefinition.getValueEnumerationRef();
	}
	
	public Collection<T> getAllowedValues() {
		if (itemDefinition instanceof PrismPropertyDefinition) {
			return ((PrismPropertyDefinition) itemDefinition).getAllowedValues();
		}
		
		return null;
	}
	
	public PrismContext getPrismContext() {
		return pageBase.getPrismContext();
	}
	
	public LookupTableType getPredefinedValues() {
		ItemWrapper item = baseModel.getObject().getItem();
		if (item instanceof PropertyWrapper) {
			return ((PropertyWrapper) item).getPredefinedValues();
		}
		return null;
	}
	
	public QName getDefinitionName() {
		return itemDefinition.getName();
	}
	
	public Component getParentComponent() {
		return parentComponent;
	}
	
	public Class<T> getTypeClass() {
		Class<T> clazz = itemDefinition.getTypeClass();
		if (clazz == null) {
			clazz = getPrismContext().getSchemaRegistry().determineClassForType(itemDefinition.getTypeName());
		}
		return clazz;
	}
	
	public ItemRealValueModel<T> getRealValueModel() {
		return realValueModel;
	}
	
	void setPageBase(PageBase pageBase) {
		this.pageBase = pageBase;
	}
	void setBaseExpression(String baseExpression) {
		this.baseExpression = baseExpression;
	}
	void setComponentId(String componentId) {
		this.componentId = componentId;
	}
	void setBaseModel(IModel<ValueWrapper<T>> baseModel) {
		this.baseModel = baseModel;
		this.realValueModel = new ItemRealValueModel<>(baseModel.getObject());
	}
	public void setItemDefinition(ItemDefinition itemDefinition) {
		this.itemDefinition = itemDefinition;
	}
	
	public void setPredefinedValues(LookupTableType predefinedValues) {
		this.predefinedValues = predefinedValues;
	}
	
	public void setParentComponent(Component parentComponent) {
		this.parentComponent = parentComponent;
	}
}
