/**
 * Copyright (c) 2015-2016 Evolveum
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

import org.apache.commons.collections4.CollectionUtils;

import com.evolveum.midpoint.prism.DefaultReferencableImpl;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;

/**
 * @author katkav
 *
 */
public class AssociationWrapper extends ContainerWrapper<ShadowAssociationType> {

	private static transient Trace LOGGER = TraceManager.getTrace(AssociationWrapper.class);
	
	AssociationWrapper(PrismContainer<ShadowAssociationType> container, ContainerStatus status, ItemPath path) {
		super(container, status, path);	
	}

	private static final long serialVersionUID = 1L;
	
	@Override
	public PrismContainer<ShadowAssociationType> createContainerAddDelta() throws SchemaException {
		if (CollectionUtils.isEmpty(getValues())) {
			return null;
		}
		
		PrismContainer<ShadowAssociationType> shadowAssociation = getItemDefinition().instantiate();
		
		//we know that there is always only one value
		ContainerValueWrapper<ShadowAssociationType> containerValueWrappers = getValues().iterator().next();
		for (ItemWrapper itemWrapper : containerValueWrappers.getItems()) {
			
			if (!(itemWrapper instanceof ReferenceWrapper)) {
				LOGGER.warn("Item in shadow association value wrapper is not an reference. Should not happen.");
				continue;
			}
			
			ReferenceWrapper refWrapper = (ReferenceWrapper) itemWrapper;
			if (!refWrapper.hasChanged()) {
				return null;
			}
			
			PrismReference updatedRef = refWrapper.getUpdatedItem(getItem().getPrismContext());
			
			for (PrismReferenceValue updatedRefValue : updatedRef.getValues()) {
				ShadowAssociationType shadowAssociationType = new ShadowAssociationType();
				shadowAssociationType.setName(refWrapper.getName());
				shadowAssociationType.setShadowRef(ObjectTypeUtil.createObjectRef(updatedRefValue));
				shadowAssociation.add(shadowAssociationType.asPrismContainerValue());
			}
			
 		}
		
		if (shadowAssociation.isEmpty() || shadowAssociation.getValues().isEmpty()) {
			return null;
		}
		return shadowAssociation;
	}
	
	@Override
	public <O extends ObjectType> void collectModifications(ObjectDelta<O> delta) throws SchemaException {
		
		if (CollectionUtils.isEmpty(getValues())) {
			return;
		}
		
		ContainerValueWrapper<ShadowAssociationType> containerValueWrappers = getValues().iterator().next();
		
		for (ItemWrapper itemWrapper : containerValueWrappers.getItems()) {
			
			if (!(itemWrapper instanceof ReferenceWrapper)) {
				LOGGER.warn("Item in shadow association value wrapper is not an reference. Should not happen.");
				continue;
			}
			
			ReferenceWrapper refWrapper = (ReferenceWrapper) itemWrapper;
			if (!refWrapper.hasChanged()) {
				continue;
			}
			
			for (ValueWrapper refValue : refWrapper.getValues()) {
				
				PrismReferenceValue prismRefValue = (PrismReferenceValue) refValue.getValue();
				ShadowAssociationType shadowAssociationType = new ShadowAssociationType();
				shadowAssociationType.setName(refWrapper.getName());
				shadowAssociationType.setShadowRef(ObjectTypeUtil.createObjectRef(prismRefValue));
				switch (refValue.getStatus()) {
					case ADDED:
						if (!refValue.hasValueChanged()) {
							continue;
						}
						delta.addModificationAddContainer(refWrapper.getPath(), shadowAssociationType);
						break;
					case DELETED:
						delta.addModificationDeleteContainer(refWrapper.getPath(), shadowAssociationType);
					default:
						break;
				}

				
				
			}
		}
		
	}
			
	
}

