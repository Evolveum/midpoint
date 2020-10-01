/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.visualizer;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.path.ItemPath;

import java.io.Serializable;
import java.util.List;

/**
 * @author mederly
 */
public interface SceneItem extends Serializable {

    Name getName();
    List<? extends SceneItemValue> getNewValues();

    boolean isOperational();

    Item<?,?> getSourceItem();

    /**
     * Item path, relative to the scene root path.
     */
    ItemPath getSourceRelPath();

    boolean isDescriptive();
}
