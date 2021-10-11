/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.visualizer;

import java.io.Serializable;

/**
 * Name of a scene or a scene item.
 *
 * TODO reconsider this structure
 *
 * @author mederly
 */
public interface Name extends Serializable {

    String getSimpleName();
    String getDisplayName();
    String getId();
    String getDescription();
    boolean namesAreResourceKeys();

}
