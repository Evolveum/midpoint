/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import java.io.Serializable;

/**
 * Common functionality for "focus list" pages or panels - e.g. roles, services; in the future also users and maybe orgs.
 * Intended to be used e.g. for FocusListInlineMenuHelper.
 *
 * TODO find a better place for this interface
 */
@FunctionalInterface
public interface FocusListComponent<F extends FocusType> {

    MainObjectListPanel<F> getObjectListPanel();

}
