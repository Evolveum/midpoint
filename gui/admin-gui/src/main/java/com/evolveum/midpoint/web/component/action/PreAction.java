/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.action;

import com.evolveum.midpoint.gui.api.page.PageBase;

import com.evolveum.midpoint.prism.Containerable;

import org.apache.wicket.ajax.AjaxRequestTarget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface PreAction<C extends Containerable, AGA extends AbstractGuiAction<C>> {

    Map<String, Object> actionResultParametersMap = new HashMap<>();

    public void executePreActionAndMainAction(AGA mainAction, List<C> objectsToProcess, PageBase pageBase, AjaxRequestTarget target);

    default void addActionResultParameter(String key, Object value) {
        actionResultParametersMap.put(key, value);
    }

    default Map<String, Object> getActionResultParametersMap() {
        return actionResultParametersMap;
    }
}
