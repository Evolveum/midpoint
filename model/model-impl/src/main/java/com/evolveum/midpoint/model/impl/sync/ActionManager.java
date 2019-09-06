/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import java.util.List;
import java.util.Map;


/**
 * @author Vilo Repan
 */
public interface ActionManager<T extends Action> {

    void setActionMapping(Map<String, Class<T>> actionMap);

    Action getActionInstance(String uri);

    List<String> getAvailableActions();
}
