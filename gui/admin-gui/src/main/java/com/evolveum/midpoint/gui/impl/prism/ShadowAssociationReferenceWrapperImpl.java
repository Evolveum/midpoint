/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.gui.impl.prism;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author skublik
 *
 */
public class ShadowAssociationReferenceWrapperImpl<R extends Referencable> extends PrismReferenceWrapperImpl<R> {

	private static final long serialVersionUID = 1L;
	
	private String displayName;
	
	public ShadowAssociationReferenceWrapperImpl(PrismContainerValueWrapper<?> parent, PrismReference item,
			ItemStatus status) {
		super(parent, item, status);
	}

	
	@Override
	public String getDisplayName() {
		if(displayName != null) {
			return displayName;
		}
		return super.getDisplayName();
	}
	
	@Override
	public QName getTargetTypeName() {
		return ShadowType.COMPLEX_TYPE;
	}
	
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
}
