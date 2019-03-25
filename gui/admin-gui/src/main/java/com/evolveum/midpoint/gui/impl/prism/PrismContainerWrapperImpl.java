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

import java.io.Serializable;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.prism.component.PrismContainerPanel;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.prism.MutablePrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibilityHandler;
import com.evolveum.midpoint.web.component.prism.PrismContainerHeaderPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

/**
 * @author katka
 *
 */
public class PrismContainerWrapperImpl<C extends Containerable> extends ItemWrapperImpl<PrismContainerValue<C>, PrismContainer<C>, PrismContainerDefinition<C>, PrismContainerValueWrapper<C>> implements PrismContainerWrapper<C>, Serializable{

	private static final long serialVersionUID = 1L;
	
	private boolean showOnTopLevel;
	
	private boolean expanded;
	
	private PrismContainer<C> newContainer;
	private PrismContainer<C> oldContainer;
	
	/**
	 * @param parent
	 * @param item
	 * @param status
	 * @param fullPath
	 * @param prismContext
	 */
	public PrismContainerWrapperImpl(PrismContainerValueWrapper<C> parent, PrismContainer<C> item, ItemStatus status) {
		super(parent, item, status);
		this.newContainer = item;
		this.oldContainer = item.clone();
		this.expanded = !item.isEmpty();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper#setExpanded(boolean)
	 */
	@Override
	public void setExpanded(boolean expanded) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper#isExpanded()
	 */
	@Override
	public boolean isExpanded() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper#setShowOnTopLevel(boolean)
	 */
	@Override
	public void setShowOnTopLevel(boolean setShowOnTopLevel) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper#isShowOnTopLevel()
	 */
	@Override
	public boolean isShowOnTopLevel() {
		// TODO Auto-generated method stub
		return false;
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
	 * @see com.evolveum.midpoint.gui.api.prism.ItemWrapper#getValues()
	 */
	@Override
	public List<PrismContainerValueWrapper<C>> getValues() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismContainerDefinition#getCompileTimeClass()
	 */
	@Override
	public Class<C> getCompileTimeClass() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismContainerDefinition#getComplexTypeDefinition()
	 */
	@Override
	public ComplexTypeDefinition getComplexTypeDefinition() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismContainerDefinition#getDefaultNamespace()
	 */
	@Override
	public String getDefaultNamespace() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismContainerDefinition#getIgnoredNamespaces()
	 */
	@Override
	public List<String> getIgnoredNamespaces() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismContainerDefinition#getDefinitions()
	 */
	@Override
	public List<? extends ItemDefinition> getDefinitions() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismContainerDefinition#getPropertyDefinitions()
	 */
	@Override
	public List<PrismPropertyDefinition> getPropertyDefinitions() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismContainerDefinition#createEmptyDelta(com.evolveum.midpoint.prism.path.ItemPath)
	 */
	@Override
	public ContainerDelta<C> createEmptyDelta(ItemPath path) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismContainerDefinition#clone()
	 */
	@Override
	public PrismContainerDefinition<C> clone() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismContainerDefinition#cloneWithReplacedDefinition(javax.xml.namespace.QName, com.evolveum.midpoint.prism.ItemDefinition)
	 */
	@Override
	public PrismContainerDefinition<C> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismContainerDefinition#replaceDefinition(javax.xml.namespace.QName, com.evolveum.midpoint.prism.ItemDefinition)
	 */
	@Override
	public void replaceDefinition(QName itemName, ItemDefinition newDefinition) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismContainerDefinition#createValue()
	 */
	@Override
	public PrismContainerValue<C> createValue() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismContainerDefinition#canRepresent(javax.xml.namespace.QName)
	 */
	@Override
	public boolean canRepresent(QName type) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismContainerDefinition#toMutable()
	 */
	@Override
	public MutablePrismContainerDefinition<C> toMutable() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.LocalDefinitionStore#findLocalItemDefinition(javax.xml.namespace.QName, java.lang.Class, boolean)
	 */
	@Override
	public <ID extends ItemDefinition> ID findLocalItemDefinition(QName name, Class<ID> clazz, boolean caseInsensitive) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.LocalDefinitionStore#findNamedItemDefinition(javax.xml.namespace.QName, com.evolveum.midpoint.prism.path.ItemPath, java.lang.Class)
	 */
	@Override
	public <ID extends ItemDefinition> ID findNamedItemDefinition(QName firstName, ItemPath rest, Class<ID> clazz) {
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
	 * @see com.evolveum.midpoint.prism.PrismContainerDefinition#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
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

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper#findItem(com.evolveum.midpoint.prism.path.ItemPath)
	 */
	@Override
	public ItemWrapper<?, ?, ?, ?> findItem(ItemPath path) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper#findContainer(com.evolveum.midpoint.prism.path.ItemPath)
	 */
	@Override
	public <T extends Containerable> PrismContainerWrapper<T> findContainer(ItemPath path) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper#findProperty(com.evolveum.midpoint.prism.path.ItemPath)
	 */
	@Override
	public <X> PrismPropertyWrapper<X> findProperty(ItemPath propertyPath) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper#findReference(com.evolveum.midpoint.prism.path.ItemPath)
	 */
	@Override
	public PrismReferenceWrapper findReference(ItemPath path) {
		// TODO Auto-generated method stub
		return null;
	}

	
//	@Override
//	public boolean isVisible() {
//		
//		
//		PrismContainerDefinition<C> def = getItemDefinition();
//
//		if (def.getProcessing() != null && def.getProcessing() != ItemProcessing.AUTO) {
//			return false;		
//		}
//		
//		if (def.isOperational() && (!def.getTypeName().equals(MetadataType.COMPLEX_TYPE))) {
//			return false;
//		}
//		
//		if (def.isDeprecated() && isEmpty()) {
//			return false;
//		}
//		
//		
//
//		return false;
//	}
	

	
}
