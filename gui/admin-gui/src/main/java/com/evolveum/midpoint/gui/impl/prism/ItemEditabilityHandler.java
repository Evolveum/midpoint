/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

/**
 * @author honchar
 *
 */
@FunctionalInterface
public interface ItemEditabilityHandler extends Serializable {

        boolean isEditable(ItemWrapper wrapper);

}
