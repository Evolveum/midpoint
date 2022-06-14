/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.dialog;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

public interface Popupable {

    String ID_CONTENT = "content";

    String ID_FOOTER = "footer";

    int getWidth();
    int getHeight();
    String getWidthUnit();
    String getHeightUnit();
    IModel<String> getTitle();
    Component getContent();

    default Component getFooter() {
        return null;
    }
}
