package com.evolveum.midpoint.gui.impl.factory;

import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ItemRealValueModel<T> extends PropertyModel<T>{

	private static final long serialVersionUID = 1L;
		
	private ValueWrapper<T> modelObject;
	
	public ItemRealValueModel(ValueWrapper<T> modelObject) {
		super(modelObject, "value.value");
		this.modelObject = modelObject;
	}

	@Override
	public T getObject() {
		if (modelObject.getItem().getItemDefinition() instanceof PrismReferenceDefinition) {
			PrismReferenceValue refValue = (PrismReferenceValue) modelObject.getValue();
			if (refValue == null) {
				return null;
			}
			return (T) refValue.asReferencable();
		}
		
		return super.getObject();
	}
	
	@Override
	public void setObject(T object) {
		if (modelObject.getItem().getItemDefinition() instanceof PrismReferenceDefinition) {
			modelObject.setValue(((Referencable) object).asReferenceValue()); 
			return;
		}
		
		super.setObject(object);
	}
	
	
}
