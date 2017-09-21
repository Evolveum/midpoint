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

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;

/**
 * @author semancik
 *
 */
public class AssociationWrapper {
//	extends PropertyWrapper<PrismContainer<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> {
}
//	private static final long serialVersionUID = 1L;
//
//	private static final Trace LOGGER = TraceManager.getTrace(AssociationWrapper.class);
//
//	private RefinedAssociationDefinition assocRDef;
//
//	public AssociationWrapper(ContainerValueWrapper<ShadowAssociationType> container, PrismContainer<ShadowAssociationType> property,
//			boolean readonly, ValueStatus status, RefinedAssociationDefinition assocRDef) {
//		super(container, property, readonly, status);
//		this.assocRDef = assocRDef;
//	}
//
//	@Override
//	public ValueWrapper createAddedValue() {
//		// TODO Auto-generated method stub
//		return super.createAddedValue();
//	}
//	@Override
//	public ValueWrapper<ShadowAssociationType> createAddedValue() {
//		PrismContainer<ShadowAssociationType> container = (PrismContainer<ShadowAssociationType>)getItem();
//		PrismContainerValue<ShadowAssociationType> cval = container.createNewValue();
//        ValueWrapper<ShadowAssociationType> wrapper = new ValueWrapper<>(this, cval, ValueStatus.ADDED);
//
//        return wrapper;
//	}
//
//	@Override
//	public String getDisplayName() {
//		if (assocRDef != null) {
//			String displayName = assocRDef.getDisplayName();
//			if (displayName != null) {
//				return displayName;
//			}
//		}
//		return super.getDisplayName();
//	}
//
//	public RefinedAssociationDefinition getRefinedAssociationDefinition() {
//		return assocRDef;
//	}
//
//	@Override
//	protected String getDebugName() {
//		return "AssociationWrapper";
//	}
//
//}
