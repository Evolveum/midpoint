/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.refactored;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceFeatureType;

public abstract class SpecialSearchItemWrapper<F extends UserInterfaceFeatureType> extends AbstractSearchItemWrapper {

    F featureItem;

    public SpecialSearchItemWrapper(F featureItem) {
        this.featureItem = featureItem;
    }

    public Class<AbstractSearchItemPanel> getSearchItemPanelClass() {
        return null;
    }

    public String getName() {
        return "";
    }

    public String getHelp() {
        return "";
    }

    public String getTitle() {
        return "";
    }
}
