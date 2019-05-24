/*
 * Copyright (c) 2010-2018 Evolveum
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

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;

/**
 * @author katka
 *
 */
public class PrismReferenceWrapperImpl<R extends Referencable> extends ItemWrapperImpl<PrismReferenceValue, PrismReference, PrismReferenceDefinition, PrismReferenceValueWrapperImpl<R>> implements PrismReferenceWrapper<R> {

	
	private ObjectFilter filter;
	
	public PrismReferenceWrapperImpl(PrismContainerValueWrapper<?> parent, PrismReference item, ItemStatus status) {
		super(parent, item, status);
	}

	private static final long serialVersionUID = 1L;
	
	@Override
	public QName getTargetTypeName() {
		return getItemDefinition().getTargetTypeName();
	}

	@Override
	public QName getCompositeObjectElementName() {
		return getItemDefinition().getCompositeObjectElementName();
	}

	@Override
	public boolean isComposite() {
		return getItemDefinition().isComposite();
	}

	@Override
	public PrismReferenceDefinition clone() {
		return getItemDefinition().clone();
	}

	@Override
	public PrismReference instantiate() {
		return getItemDefinition().instantiate();
	}
	
	@Override
	public PrismReference instantiate(QName name) {
		return getItemDefinition().instantiate(name);
	}

	@Override
	public ObjectFilter getFilter() {
		return filter;
	}
	
	@Override
	public void setFilter(ObjectFilter filter) {
		this.filter = filter;
	}
	
	@Override
	public List<QName> getTargetTypes() {
		return WebComponentUtil.createSupportedTargetTypeList(getTargetTypeName());
	}


	@Override
	protected boolean isEmpty() {
		if (super.isEmpty()) return true;
		List<PrismReferenceValue> pVals = getItem().getValues();
		boolean allEmpty = true;
		for (PrismReferenceValue pVal : pVals) {
			if (!pVal.isEmpty()) {
				allEmpty = false;
				break;
			}
		}
		
		return allEmpty;
	}

}
