/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.prism.wrapper;

import java.io.Serializable;

import com.evolveum.midpoint.web.component.prism.ItemVisibility;

/**
 * @author katka
 *
 */
@FunctionalInterface
public interface ItemVisibilityHandler extends Serializable{

    ItemVisibility isVisible(ItemWrapper wrapper);
}
