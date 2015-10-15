/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.prism;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class ReferenceWrapper implements ItemWrapper, Serializable {
	
	  private ContainerWrapper container;
	    private PrismReference reference;
	    private ValueStatus status;
	    private List<ValueWrapper> values;
	    private String displayName;
	    private boolean readonly;
	    private PrismReferenceDefinition itemDefinition;

	    public ReferenceWrapper(ContainerWrapper container, PrismReference reference, boolean readonly, ValueStatus status) {
	        Validate.notNull(reference, "Property must not be null.");
	        Validate.notNull(status, "Property status must not be null.");

	        this.container = container;
	        this.reference = reference;
	        this.status = status;
	        this.readonly = readonly;
	        this.itemDefinition = getItemDefinition();
	    }

	    public void revive(PrismContext prismContext) throws SchemaException {
	        if (reference != null) {
	            reference.revive(prismContext);
	        }
	        if (itemDefinition != null) {
	            itemDefinition.revive(prismContext);
	        }
	    }

	    @Override
		public PrismReferenceDefinition getItemDefinition() {
	
	    	PrismReferenceDefinition refDef = null;
	    	if (container.getItemDefinition() != null){
	    		refDef = container.getItemDefinition().findReferenceDefinition(reference.getDefinition().getName());
	    	}
	    	if (refDef == null) {
	    		refDef = reference.getDefinition();
	    	}
	    	return refDef;
	    }
	    
	    public boolean isVisible() {
	        if (reference.getDefinition().isOperational()) {
	            return false;
	        }

	        return container.isItemVisible(this);
	    }
	    
	    

	    public ContainerWrapper getContainer() {
	        return container;
	    }

	    @Override
	    public String getDisplayName() {
	        if (StringUtils.isNotEmpty(displayName)) {
	            return displayName;
	        }
	        return ContainerWrapper.getDisplayNameFromItem(reference);
	    }

	    @Override
	    public void setDisplayName(String displayName) {
	        this.displayName = displayName;
	    }

	    public ValueStatus getStatus() {
	        return status;
	    }

	    public List<ValueWrapper> getValues() {
	        if (values == null) {
	            values = createValues();
	        }
	        return values;
	    }

	    @Override
	    public PrismReference getItem() {
	        return reference;
	    }

	    private List<ValueWrapper> createValues() {
	        List<ValueWrapper> values = new ArrayList<ValueWrapper>();

	        for (PrismReferenceValue prismValue : (List<PrismReferenceValue>) reference.getValues()) {
	            values.add(new ValueWrapper(this, prismValue, prismValue, ValueStatus.NOT_CHANGED));
	        }

	        int minOccurs = reference.getDefinition().getMinOccurs();
	        while (values.size() < minOccurs) {
	            values.add(createValue());
	        }

	        if (values.isEmpty()) {
	            values.add(createValue());
	        }

	        return values;
	    }

	    public void addValue() {
	        getValues().add(createValue());
	    }

	    public ValueWrapper createValue() {
//	        PrismReferenceDefinition definition = reference.getDefinition();
//	     	definition.instantiate()
	    	
	    	PrismReferenceValue prv = new PrismReferenceValue();
	    	
	        ValueWrapper wrapper = new ValueWrapper(this, prv, ValueStatus.ADDED);
	        return wrapper;
	    }

	 

	    public boolean hasChanged() {
	        for (ValueWrapper value : getValues()) {
	            switch (value.getStatus()) {
	                case DELETED:
	                    return true;
	                case ADDED:
	                case NOT_CHANGED:
	                    if (value.hasValueChanged()) {
	                        return true;
	                    }
	            }
	        }

	        return false;
	    }

	    @Override
	    public String toString() {
	        StringBuilder builder = new StringBuilder();
	        builder.append(getDisplayName());
	        builder.append(", ");
	        builder.append(status);
	        builder.append("\n");
	        for (ValueWrapper wrapper : getValues()) {
	            builder.append("\t");
	            builder.append(wrapper.toString());
	            builder.append("\n");
	        }
	        return builder.toString();
	    }

	    @Override
	    public boolean isReadonly() {
	        return readonly;
	    }

	    public void setReadonly(boolean readonly) {
	        this.readonly = readonly;
	    }

	    @Override
		public String debugDump() {
			return debugDump(0);
		}

		@Override
		public String debugDump(int indent) {
			StringBuilder sb = new StringBuilder();
			DebugUtil.indentDebugDump(sb, indent);
			sb.append("ReferenceWrapper(\n");
			DebugUtil.debugDumpWithLabel(sb, "displayName", displayName, indent+1);
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "status", status == null?null:status.toString(), indent+1);
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "readonly", readonly, indent+1);
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "itemDefinition", itemDefinition == null?null:itemDefinition.toString(), indent+1);
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "reference", reference, indent+1);
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "values", values, indent+1);
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent);
			sb.append(")");
			return sb.toString();
		}

}
