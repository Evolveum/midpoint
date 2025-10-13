/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.prism.wrapper;

import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;

/**
 * @author katka
 *
 */
public interface PrismPropertyWrapper<T> extends ItemWrapper<PrismProperty<T>, PrismPropertyValueWrapper<T>>, PrismPropertyDefinition<T> {

    String getPredefinedValuesOid();
    void setPredefinedValuesOid(String oid);
}
