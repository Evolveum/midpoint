/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.InlineMenuable;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

/**
 * @author lazyman
 */
public class SelectableBean<T extends Serializable> extends Selectable<T> implements InlineMenuable, DebugDumpable {
	private static final long serialVersionUID = 1L;

	public static final String F_VALUE = "value";
	
	private static final Trace LOGGER = TraceManager.getTrace(SelectableBean.class);

    /**
     * Value of object that this bean represents. It may be null in case that non-success result is set.
     */
    private T value;
    
    /**
     * Result of object retrieval (or attempt of object retrieval). It case that it is not error the result is optional.
     */
    private OperationResult result;
    
    private List<InlineMenuItem> menuItems;

    public SelectableBean() {
    }

    public SelectableBean(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
    
    public OperationResult getResult() {
		return result;
	}

	public void setResult(OperationResult result) {
		this.result = result;
	}
	
	public void setResult(OperationResultType resultType) {
		this.result = OperationResult.createOperationResult(resultType);
	}

	public List<InlineMenuItem> getMenuItems() {
    	if (menuItems == null) {
    		menuItems = new ArrayList<InlineMenuItem>();
    	}
		return menuItems;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.result == null) ? 0 : this.result.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SelectableBean other = (SelectableBean) obj;
		if (result == null) {
			if (other.result != null) {
				return false;
			}
		} else if (!result.equals(other.result)) {
			return false;
		}
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		// In case both values are objects then compare only OIDs.
		// that should be enough. Comparing complete objects may be slow
		// (e.g. if the objects have many assignments) and Wicket
		// invokes compare a lot ...
		} else if (!MiscSchemaUtil.quickEquals(value, other.value)) {
			return false;
		}
		return true;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("SelectableBean\n");
		DebugUtil.debugDumpWithLabelLn(sb, "value", value==null?null:value.toString(), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "result", result==null?null:result.toString(), indent+1);
		DebugUtil.debugDumpWithLabel(sb, "menuItems", menuItems, indent+1);
		return sb.toString();
	}

}
