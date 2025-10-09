/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.factory.wrapper;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;

/**
 * @author katka
 *
 */
public interface WrapperFactory {

    boolean match(ItemDefinition<?> def);

    default <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        return match(def);
    }

    void register();

    int getOrder();

}
