/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.prism.wrapper;

import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

/**
 * @author katka
 *
 */
public interface PrismPropertyWrapper<T> extends ItemWrapper<PrismProperty<T>, PrismPropertyValueWrapper<T>>, PrismPropertyDefinition<T> {


    LookupTableType getPredefinedValues();
    void setPredefinedValues(LookupTableType lookupTableType);

}
