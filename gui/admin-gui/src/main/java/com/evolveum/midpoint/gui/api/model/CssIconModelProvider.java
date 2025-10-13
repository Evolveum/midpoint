/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.model;

import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
@FunctionalInterface
public interface CssIconModelProvider {

    IModel<String> getCssIconModel();
}
