/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
