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
package com.evolveum.midpoint.gui.impl.factory;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.impl.prism.ItemWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
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
public class PrismReferenceWrapperImpl<R extends Referencable> extends ItemWrapperImpl<PrismReferenceValue, PrismReference, PrismReferenceDefinition, PrismReferenceValueWrapper<R>> implements PrismReferenceWrapper<R> {

	/**
	 * @param parent
	 * @param item
	 * @param status
	 * @param fullPath
	 * @param prismContext
	 */
	public PrismReferenceWrapperImpl(PrismContainerValueWrapper<?> parent, PrismReference item, ItemStatus status) {
		super(parent, item, status);
	}

	private static final long serialVersionUID = 1L;
	
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismReferenceDefinition#getTargetTypeName()
	 */
	@Override
	public QName getTargetTypeName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismReferenceDefinition#getCompositeObjectElementName()
	 */
	@Override
	public QName getCompositeObjectElementName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismReferenceDefinition#isComposite()
	 */
	@Override
	public boolean isComposite() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismReferenceDefinition#clone()
	 */
	@Override
	public PrismReferenceDefinition clone() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.prism.ItemWrapperImpl#instantiate()
	 */
	@Override
	public PrismReference instantiate() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.prism.ItemWrapperImpl#instantiate(javax.xml.namespace.QName)
	 */
	@Override
	public PrismReference instantiate(QName name) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.factory.PrismReferenceWrapper#getFilter()
	 */
	@Override
	public ObjectFilter getFilter() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.factory.PrismReferenceWrapper#getTargetTypes()
	 */
	@Override
	public List<QName> getTargetTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.ItemWrapper#setReadOnly()
	 */
	@Override
	public void setReadOnly() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.ItemWrapper#isStripe()
	 */
	@Override
	public boolean isStripe() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Itemable#getElementName()
	 */
	@Override
	public ItemName getElementName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Itemable#getDefinition()
	 */
	@Override
	public ItemDefinition getDefinition() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Itemable#getPath()
	 */
	@Override
	public ItemPath getPath() {
		// TODO Auto-generated method stub
		return null;
	}
}
