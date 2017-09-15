/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PropertyWrapper<T> extends PropertyOrReferenceWrapper<PrismProperty<T>, PrismPropertyDefinition<T>> implements Serializable, DebugDumpable {

	private static final long serialVersionUID = -6347026284758253783L;

	public PropertyWrapper(@Nullable ContainerValueWrapper container, PrismProperty<T> property, boolean readonly, ValueStatus status) {
		super(container, property, readonly, status);

        if (container != null && SchemaConstants.PATH_PASSWORD.equivalent(container.getPath())
                && PasswordType.F_VALUE.equals(property.getElementName())) {
			super.setDisplayName("prismPropertyPanel.name.credentials.password");
		}

        values = createValues();
    }

	// TODO consider unifying with ReferenceWrapper.createValues  (difference is in oldValue in ValueWrapper constructor: null vs. prismValue)
    private List<ValueWrapper> createValues() {
        List<ValueWrapper> values = new ArrayList<>();

        for (PrismValue prismValue : item.getValues()) {
            values.add(new ValueWrapper<T>(this, prismValue, ValueStatus.NOT_CHANGED));
        }

        int minOccurs = getItemDefinition().getMinOccurs();
        while (values.size() < minOccurs) {
            values.add(createAddedValue());
        }

        if (values.isEmpty()) {
            values.add(createAddedValue());
        }

        return values;
    }

	@Override
    public ValueWrapper<T> createAddedValue() {
        ItemDefinition definition = item.getDefinition();

        ValueWrapper wrapper;
        if (SchemaConstants.T_POLY_STRING_TYPE.equals(definition.getTypeName())) {
            wrapper = new ValueWrapper(this, new PrismPropertyValue(new PolyString("")),
                    new PrismPropertyValue(new PolyString("")), ValueStatus.ADDED);
//        } else if (isUser() && isThisPropertyActivationEnabled()) {
//            wrapper = new ValueWrapper(this, new PrismPropertyValue(null),
//                    new PrismPropertyValue(null), ValueStatus.ADDED);
        } else {
            wrapper = new ValueWrapper(this, new PrismPropertyValue(null), ValueStatus.ADDED);
        }

        return wrapper;
    }

//    private boolean isUser() {
//		if (getContainerValue() == null) {
//			return false;
//		}
//        ObjectWrapper wrapper = getContainerValue().getContainer().getObject();
//		if (wrapper == null) {
//			return false;
//		}
//        PrismObject object = wrapper.getObject();
//
//        return UserType.class.isAssignableFrom(object.getCompileTimeClass());
//    }

    private boolean isThisPropertyActivationEnabled() {
        if (!new ItemPath(UserType.F_ACTIVATION).equivalent(container.getPath())) {
            return false;
        }

        if (!ActivationType.F_ADMINISTRATIVE_STATUS.equals(item.getElementName())) {
            return false;
        }

//        if (container.getContainer().getObject() == null || ContainerStatus.MODIFYING.equals(container.getContainer().getObject().getStatus())) {
//            //when modifying then we don't want to create "true" value for c:activation/c:enabled, only during add
//            return false;
//        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PropertyWrapper(");
        builder.append(getDisplayName());
        builder.append(" (");
        builder.append(status);
        builder.append(") ");
        builder.append(getValues() == null ? null :  getValues().size());
		builder.append(" values)");
        builder.append(")");
        return builder.toString();
    }

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append(getDebugName());
		sb.append(": ").append(PrettyPrinter.prettyPrint(getName())).append("\n");
		DebugUtil.debugDumpWithLabel(sb, "displayName", displayName, indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "status", status == null?null:status.toString(), indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "readonly", readonly, indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "itemDefinition", getItemDefinition() == null?null:getItemDefinition().toString(), indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "property", item == null?null:item.toString(), indent+1);
		sb.append("\n");
		DebugUtil.debugDumpLabel(sb, "values", indent+1);
		sb.append("\n");
		DebugUtil.debugDump(sb, values, indent+2, false);
		return sb.toString();
	}

	protected String getDebugName() {
		return "PropertyWrapper";
	}
}

	