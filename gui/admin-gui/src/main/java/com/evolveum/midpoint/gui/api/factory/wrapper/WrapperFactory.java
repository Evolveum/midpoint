/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

//    T createBuilder();
//
//    Class<T> getBuilderClass();
}
