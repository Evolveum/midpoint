package com.evolveum.midpoint.report.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.prism.PrismContainerable;
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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class MidPointDataSource implements JRDataSource{

	public static final String PARENT_NAME = "_parent_";
	public static final String THIS_NAME = "_this_";

	Collection<PrismContainerValue<? extends Containerable>> resultList = null;
	Iterator<PrismContainerValue<? extends Containerable>> iterator = null;
	PrismContainerValue<? extends Containerable> currentObject = null;

	public MidPointDataSource(Collection<PrismContainerValue<? extends Containerable>> results) {
		this.resultList = results;
		if (results != null){
			iterator = results.iterator();
		}
	}


	@Override
	public boolean next() throws JRException {
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
		String fieldName = jrField.getName();
		if (fieldName.equals("oid")){
			if (currentObject.getParent() instanceof PrismObject) {
				return ((PrismObject) currentObject.getParent()).getOid();
			} else {
				throw new IllegalStateException("oid property is not supported for " + currentObject.getClass());
			}
		} else if (PARENT_NAME.equals(fieldName)) {
			PrismContainerable parent1 = currentObject.getParent();
			if (!(parent1 instanceof PrismContainer)) {
				return null;
			}
			return ((PrismContainer) parent1).getParent();
		} else if (THIS_NAME.equals(fieldName)) {
			return currentObject;
		}

		ItemPathType itemPathType = new ItemPathType(fieldName);
		ItemPath path = itemPathType.getItemPath();
		Item i = currentObject.findItem(path);
		if (i == null) {
			return null;
		}

		if (i instanceof PrismProperty){
			if (i.isSingleValue()){
				return normalize(((PrismProperty) i).getRealValue(), jrField.getValueClass());
			}
			List normalized = new ArrayList<>();
			for (Object real : ((PrismProperty) i).getRealValues()){
				normalized.add(normalize(real, jrField.getValueClass()));
			}
			return ((PrismProperty) i).getRealValues();
		} else if (i instanceof PrismReference){
			if (i.isSingleValue()){
				return ObjectTypeUtil.createObjectRef(((PrismReference) i).getValue());
			}

			List<Referencable> refs = new ArrayList<Referencable>();
			for (PrismReferenceValue refVal : ((PrismReference) i).getValues()){
				refs.add(ObjectTypeUtil.createObjectRef(refVal));
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

	private Object normalize(Object realValue, Class fieldClass){
		if (realValue instanceof PolyString && fieldClass.equals(String.class)){
			return ((PolyString)realValue).getOrig();
		}

		return realValue;
	}

}
