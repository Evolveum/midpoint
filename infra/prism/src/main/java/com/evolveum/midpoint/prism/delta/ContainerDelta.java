package com.evolveum.midpoint.prism.delta;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PropertyPath;

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

	
	
}
