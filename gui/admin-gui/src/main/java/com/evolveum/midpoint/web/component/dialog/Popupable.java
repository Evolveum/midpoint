/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Popupable {

    String ID_TITLE = "title";

    String ID_CONTENT = "content";

    String ID_FOOTER = "footer";

    int getWidth();
    int getHeight();
    String getWidthUnit();
    String getHeightUnit();
    IModel<String> getTitle();
    Component getContent();

    default IModel<String> getTitleIconClass() {
        return null;
    }

    @NotNull
    default Component getFooter() {
        return new WebMarkupContainer(ID_FOOTER);
    }

    @Nullable
    default Component getTitleComponent() {
        return null;
    }

    default String getCssClassForDialog() {
        return "";
    }

}
