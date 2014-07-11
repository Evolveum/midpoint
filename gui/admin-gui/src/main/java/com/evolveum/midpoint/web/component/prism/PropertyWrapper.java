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

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PropertyWrapper implements ItemWrapper, Serializable {

    private ContainerWrapper container;
    private PrismProperty property;
    private ValueStatus status;
    private List<ValueWrapper> values;
    private String displayName;
    private boolean readonly;
    private PrismPropertyDefinition itemDefinition;

    public PropertyWrapper(ContainerWrapper container, PrismProperty property, boolean readonly, ValueStatus status) {
        Validate.notNull(property, "Property must not be null.");
        Validate.notNull(status, "Property status must not be null.");

        this.container = container;
        this.property = property;
        this.status = status;
        this.readonly = readonly;
        this.itemDefinition = getItemDefinition();

        ItemPath passwordPath = new ItemPath(SchemaConstantsGenerated.C_CREDENTIALS,
                CredentialsType.F_PASSWORD);
        if (passwordPath.equals(container.getPath())
                && PasswordType.F_VALUE.equals(property.getElementName())) {
            displayName = "prismPropertyPanel.name.credentials.password";
        }
    }

    public void revive(PrismContext prismContext) throws SchemaException {
        if (property != null) {
            property.revive(prismContext);
        }
        if (itemDefinition != null) {
            itemDefinition.revive(prismContext);
        }
    }

    protected PrismPropertyDefinition getItemDefinition() {
    	PrismPropertyDefinition propDef = container.getContainerDefinition().findPropertyDefinition(property.getDefinition().getName());
    	if (propDef == null) {
    		propDef = property.getDefinition();
    	}
    	return propDef;
    }
    
    public boolean isVisible() {
        if (property.getDefinition().isOperational()) {
            return false;
        }

        return container.isPropertyVisible(this);
    }
    
    

    ContainerWrapper getContainer() {
        return container;
    }

    @Override
    public String getDisplayName() {
        if (StringUtils.isNotEmpty(displayName)) {
            return displayName;
        }
        return ContainerWrapper.getDisplayNameFromItem(property);
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
    public PrismProperty getItem() {
        return property;
    }

    private List<ValueWrapper> createValues() {
        List<ValueWrapper> values = new ArrayList<ValueWrapper>();

        for (PrismPropertyValue prismValue : (List<PrismPropertyValue>) property.getValues()) {
            values.add(new ValueWrapper(this, prismValue, ValueStatus.NOT_CHANGED));
        }

        int minOccurs = property.getDefinition().getMinOccurs();
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
        PrismPropertyDefinition definition = property.getDefinition();

        ValueWrapper wrapper;
        if (ProtectedStringType.COMPLEX_TYPE.equals(definition.getTypeName())) {
            wrapper = new ValueWrapper(this, new PrismPropertyValue(new ProtectedStringType()),
                    new PrismPropertyValue(new ProtectedStringType()), ValueStatus.ADDED);
        } else if (SchemaConstants.T_POLY_STRING_TYPE.equals(definition.getTypeName())) {
            wrapper = new ValueWrapper(this, new PrismPropertyValue(new PolyString("")),
                    new PrismPropertyValue(new PolyString("")), ValueStatus.ADDED);
        } else if (isUser() && isThisPropertyActivationEnabled()) {
            wrapper = new ValueWrapper(this, new PrismPropertyValue(ActivationStatusType.ENABLED),
                    new PrismPropertyValue(null), ValueStatus.ADDED);
        } else {
            wrapper = new ValueWrapper(this, new PrismPropertyValue(null), ValueStatus.ADDED);
        }

        return wrapper;
    }

    private boolean isUser() {
        ObjectWrapper wrapper = getContainer().getObject();
        PrismObject object = wrapper.getObject();

        return UserType.class.isAssignableFrom(object.getCompileTimeClass());
    }

    private boolean isThisPropertyActivationEnabled() {
        if (!new ItemPath(UserType.F_ACTIVATION).equals(container.getPath())) {
            return false;
        }

        if (!ActivationType.F_ADMINISTRATIVE_STATUS.equals(property.getElementName())) {
            return false;
        }

        if (ContainerStatus.MODIFYING.equals(container.getObject().getStatus())) {
            //when modifying then we don't want to create "true" value for c:activation/c:enabled, only during add
            return false;
        }

        return true;
    }

    boolean hasChanged() {
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

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

}
