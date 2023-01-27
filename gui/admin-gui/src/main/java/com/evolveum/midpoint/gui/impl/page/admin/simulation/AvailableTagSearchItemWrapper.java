/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.List;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.ChoicesSearchItemWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DisplayableValue;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AvailableTagSearchItemWrapper extends ChoicesSearchItemWrapper<String> {

    public AvailableTagSearchItemWrapper(ItemPath path, List<DisplayableValue<String>> availableValues) {
        super(path, availableValues);
    }
}
