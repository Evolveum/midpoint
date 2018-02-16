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

package com.evolveum.midpoint.web.component.prism;

import java.io.Serializable;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Revivable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import org.jetbrains.annotations.Nullable;

/**
 * @author lazyman
 */
public interface ItemWrapper<I extends Item, ID extends ItemDefinition, V> extends Revivable, DebugDumpable, Serializable {

	QName getName();

	/**
	 * Returns (localized) display name.
	 * First invocation of this method will look for proper display name. Subsequent invocations will return
	 * the determined name.
	 */
    String getDisplayName();

    /**
     * Display name override. Setting custom (localized) display name.
     */
    void setDisplayName(String name);

    I getItem();
    
    ItemPath getPath();

    /**
     * Item definition.
     * The definition defines how the item will be displayed (type, read-only, read-write or
     * not displayed at all). This behavior can be overriden by readonly and visible flags.
     */
    ID getItemDefinition();

    /**
     * Read only flag. This is an override of the default behavior given by the definition.
     * If set to TRUE then it overrides the value from the definition.
     */
    boolean isReadonly();

	boolean isEmpty();

    boolean hasChanged();

    List<V> getValues();

    /**
     * Visibility flag. This is NOT an override, it defines whether the item
     * should be displayed or not.
     */
	boolean isVisible();

    /**
     * Used to display the form elements with stripe in every other line.
     */
	boolean isStripe();
	
	boolean isDeprecated();
	
	String getDeprecatedSince();

    void setStripe(boolean isStripe);

//    ContainerValueWrapper getContainerValue();

    void addValue(boolean showEmpty);

	boolean checkRequired(PageBase pageBase);

	// are required fields enforced by wicket?
	default boolean isEnforceRequiredFields() {
	    ContainerWrapper cw = getParent();
	    return cw == null || cw.isEnforceRequiredFields();
    }

    @Nullable
	ContainerWrapper getParent();
	
	boolean isShowEmpty();
	
	void setShowEmpty(boolean isShowEmpty, boolean recursive);
}
