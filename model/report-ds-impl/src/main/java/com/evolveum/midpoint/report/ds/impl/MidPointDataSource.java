package com.evolveum.midpoint.report.ds.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class MidPointDataSource implements JRDataSource{

	Collection<PrismObject<? extends ObjectType>> resultList = null;
	Iterator<PrismObject<? extends ObjectType>> iterator = null;
	PrismObject<? extends ObjectType> currentObject = null; 
	
	public MidPointDataSource(Collection<PrismObject<? extends ObjectType>> results) {
		this.resultList = results;
		if (results != null){
			iterator = results.iterator();
		}
	}
	
	public MidPointDataSource(ObjectListType results){
		resultList = new ArrayList<>();
		for (ObjectType objType : results.getObject()){
			resultList.add(((Objectable)objType).asPrismObject());
		}
		iterator = resultList.iterator();
	}
	
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
//		}
//		throw new UnsupportedOperationException("dataSource.next() not supported");
//		return false;
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
//			throw new JRException("Object of type " + currentObject.getCompileTimeClass().getSimpleName() + " does not contain field " + fieldName +".");
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
			
			List<Referencable> refs = new ArrayList<Referencable>();
			for (PrismReferenceValue refVal : ((PrismReference) i).getValues()){
				refs.add(refVal.asReferencable());
			}
			return refs;
		} else if (i instanceof PrismContainer){
			if (i.isSingleValue()){
				return ((PrismContainer) i).getValue().asContainerable();
			}
			List<Containerable> containers = new ArrayList<Containerable>();
			for (Object pcv : i.getValues()){
				if (pcv instanceof PrismContainerValue){
					containers.add(((PrismContainerValue) pcv).asContainerable());
				}
			}
			return containers;
			
		} else
			throw new JRException("Could not get value of the fileld: " + fieldName);
//		return 
//		throw new UnsupportedOperationException("dataSource.getFiledValue() not supported");
	}

}
