package com.evolveum.midpoint.prism.delta;

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.util.MiscUtil;

public class ContainerDelta extends ItemDelta<PrismContainerValue> {

	public ContainerDelta(PrismContainerDefinition itemDefinition) {
		super(itemDefinition);
	}

	public ContainerDelta(PropertyPath propertyPath, PrismContainerDefinition itemDefinition) {
		super(propertyPath, itemDefinition);
	}

	public ContainerDelta(PropertyPath parentPath, QName name, PrismContainerDefinition itemDefinition) {
		super(parentPath, name, itemDefinition);
	}

	public ContainerDelta(QName name, PrismContainerDefinition itemDefinition) {
		super(name, itemDefinition);
	}

	/**
     * Returns all values regardless of whether they are added or removed or replaced.
     * Useful for iterating over all the changed values.
     */
    public <T> Collection<PrismContainerValue<T>> getValues(Class<T> type) {
        checkConsistence();
        if (valuesToReplace != null) {
            return (Collection) valuesToReplace;
        }
        return (Collection) MiscUtil.union(valuesToAdd, valuesToDelete);
    }
	
}
