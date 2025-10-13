/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.prism.wrapper;

import java.io.Serializable;

/**
 * @author honchar
 *
 */
@FunctionalInterface
public interface ItemEditabilityHandler extends Serializable {

        boolean isEditable(ItemWrapper wrapper);

}
