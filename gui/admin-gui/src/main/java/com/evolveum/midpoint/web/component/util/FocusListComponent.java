/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
