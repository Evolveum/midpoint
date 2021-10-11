/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;

import java.io.Serializable;

@FunctionalInterface
public interface ItemMandatoryHandler extends Serializable {

    boolean isMandatory(ItemWrapper<?, ?, ?, ?> itemWrapper);

}
