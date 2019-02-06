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
import java.util.List;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Revivable;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibilityHandler;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;

/**
 * @author katka
 *
 */
public interface ItemWrapper<V extends PrismValue, I extends Item<V, ID>, ID extends ItemDefinition<I>> extends Revivable, DebugDumpable, Serializable {

	
	void revive(PrismContext prismContext) throws SchemaException;
	
	String debugDump(int indent);
	
	boolean hasChanged();
	
	List<ValueWrapper<V>> getValues();
	
	boolean isVisible();
	
	void addValue(boolean showEmpty);
	
	void removeValue(ValueWrapper<V> valueWrapper) throws SchemaException;
	
	boolean checkRequired(PageBase pageBase);
	
	ContainerValueWrapper<?> getParent();
	
	Panel createPanel(String id, Form form, ItemVisibilityHandler visibilityHandler);
	
	boolean isShowEmpty();
	
	void setShowEmpty(boolean isShowEmpty, boolean recursive);
}
