package com.evolveum.midpoint.report.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class MidPointAbstractDataSource implements JRDataSource{

	List<PrismObject<? extends ObjectType>> resultList = null;
	Iterator<PrismObject<? extends ObjectType>> iterator = null;
	PrismObject<? extends ObjectType> currentObject = null;


	@Override
	public boolean next() throws JRException {
		// TODO Auto-generated method stub
		boolean hasNext = false;
		if (this.iterator != null) {
			hasNext = this.iterator.hasNext();

			if (hasNext) {
				this.currentObject = iterator.next();
			}
		}

		return hasNext;
	}

	@Override
	public Object getFieldValue(JRField jrField) throws JRException {
		// TODO Auto-generated method stub
		String fieldName = jrField.getName();
		if (fieldName.equals("oid")){
			return currentObject.getOid();
		}
		Item i = currentObject.findItem(new QName(fieldName));
		if (i == null){
			return null;
		}

		if (i instanceof PrismProperty){
			if (i.isSingleValue()){
				return ((PrismProperty) i).getRealValue();
			}
			return ((PrismProperty) i).getRealValues();
		} else if (i instanceof PrismReference){
			if (i.isSingleValue()){
				return ((PrismReference) i).getValue().asReferencable();
			}

			List<Referencable> refs = new ArrayList<>();
			for (PrismReferenceValue refVal : ((PrismReference) i).getValues()){
				refs.add(refVal.asReferencable());
			}
			return refs;
		} else if (i instanceof PrismContainer){
			if (i.isSingleValue()){
				return ((PrismContainer) i).getValue().asContainerable();
			}
			List<Containerable> containers = new ArrayList<>();
			for (Object pcv : i.getValues()){
				if (pcv instanceof PrismContainerValue){
					containers.add(((PrismContainerValue) pcv).asContainerable());
				}
			}
			return containers;

		} else
			throw new JRException("Could not get value of the fileld: " + fieldName);
	}


}
