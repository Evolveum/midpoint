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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.prism.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.impl.prism.ObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Revivable;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author katka
 *
 */
public interface ItemWrapper<V extends PrismValue, I extends Item<V, ID>, ID extends ItemDefinition<I>, VW extends PrismValueWrapper> extends ItemDefinition<I>, Revivable, DebugDumpable, Serializable {

	
	String debugDump(int indent);
	
	boolean isVisible(ItemVisibilityHandler visibilityHandler);
	
	boolean checkRequired(PageBase pageBase);
	
	PrismContainerValueWrapper<?> getParent();
	
	boolean isShowEmpty();
	
	void setShowEmpty(boolean isShowEmpty, boolean recursive);
	
	
	ItemPath getPath();
	
	//NEW
	
	boolean isReadOnly();
	
	void setReadOnly(boolean readOnly);
	
	ExpressionType getFormComponentValidator();
	
	List<VW> getValues();
	
	boolean isStripe();
	void setStripe(boolean stripe);
	
	I getItem();
	
	boolean isColumn();
	void setColumn(boolean column);
	
	<D extends ItemDelta<V, ID>> void applyDelta(D delta) throws SchemaException;

	<D extends ItemDelta<V, ID>> Collection<D> getDelta() throws SchemaException;

	<O extends ObjectType> ItemStatus findObjectStatus();

	<OW extends PrismObjectWrapper<O>, O extends ObjectType> OW findObjectWrapper(ItemWrapper parent);
	
	
}
