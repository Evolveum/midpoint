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
package com.evolveum.midpoint.gui.api.prism;

import java.util.List;

import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author katka
 *
 */
public interface PrismContainerWrapper<C extends Containerable> extends ItemWrapper<PrismContainerValue<C>, PrismContainer<C>, PrismContainerDefinition<C>, PrismContainerValueWrapper<C>>, PrismContainerDefinition<C>{

	void setExpanded(boolean expanded);
	
	boolean isExpanded();
	
	void setShowOnTopLevel(boolean setShowOnTopLevel);
	
	boolean isShowOnTopLevel();
	
	ItemStatus getStatus();

	void setVirtual(boolean virtual);
	boolean isVirtual();
	
//	ItemWrapper<?, ?, ?,?> findItem(ItemPath path);
	
	<T extends Containerable> PrismContainerWrapper<T> findContainer(ItemPath path) throws SchemaException;
	<X> PrismPropertyWrapper<X> findProperty(ItemPath propertyPath) throws SchemaException;
	<R extends Referencable> PrismReferenceWrapper<R> findReference(ItemPath path) throws SchemaException;
	<T extends Containerable> PrismContainerValueWrapper<T> findContainerValue(ItemPath path) throws SchemaException;
	<IW extends ItemWrapper> IW findItem(ItemPath path, Class<IW> type) throws SchemaException;
		
}


