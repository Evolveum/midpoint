/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.prism.wrapper;

import java.io.Serializable;

@FunctionalInterface
public interface ItemMandatoryHandler extends Serializable {

    boolean isMandatory(ItemWrapper<?, ?> itemWrapper);

}
