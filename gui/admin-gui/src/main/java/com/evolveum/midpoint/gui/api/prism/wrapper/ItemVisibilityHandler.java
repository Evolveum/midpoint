/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
