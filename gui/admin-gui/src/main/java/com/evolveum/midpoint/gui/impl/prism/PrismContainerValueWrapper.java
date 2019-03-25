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

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author katka
 *
 */
public interface PrismContainerValueWrapper<C extends Containerable> extends PrismValueWrapper<C>{
	
	String getDisplayName();
	String getHelpText();
	
	boolean isExpanded();
	
	void setExpanded(boolean expanded);
	
	boolean hasMetadata();
	boolean isShowMetadata();
	
	void setShowMetadata(boolean showMetadata);
	
	boolean isSorted();
	void setSorted(boolean sorted);
	
	List<PrismContainerDefinition<C>> getChildContainers();
	
	ValueStatus getStatus();
	void setStatus(ValueStatus status);
	
	List<PrismContainerWrapper<C>> getContainers();
	
	List<? extends ItemWrapper<?,?,?,?>> getNonContainers();
	
	PrismContainerWrapper<C> getParent();
	
	List<? extends ItemWrapper<?,?,?,?>> getItems();
	
	PrismContainerValue<C> getNewValue();
	
	<T extends Containerable> PrismContainerWrapper<T> findContainer(ItemPath path);
	<X> PrismPropertyWrapper<X> findProperty(ItemPath propertyPath);
	PrismReferenceWrapper findReference(ItemPath path);
	
}
