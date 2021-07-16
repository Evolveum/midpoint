/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.extensions.ajax.markup.html.modal.theme.DefaultTheme;

/**
 * @author Viliam Repan (lazyman)
 * @author katkav
 */
public class MainPopupDialog extends ModalDialog {
    private static final long serialVersionUID = 1L;

    public MainPopupDialog(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        setOutputMarkupId(true);
        add(new DefaultTheme());
        trapFocus();
        setOutputMarkupPlaceholderTag(true);
    }

    public Component getDialogComponent() {
        return get("overlay").get("dialog");
    }

    public String generateWidthHeightParameter(String width, String widthUnit, String height, String heightUnit) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(width)) {
            sb.append("width: " + width);
            sb.append(StringUtils.isEmpty(widthUnit) ? "px" : widthUnit);
            sb.append("; ");
        }
//        if (StringUtils.isNotEmpty(height)) {
//            sb.append("height: " + height);
//            sb.append(StringUtils.isEmpty(heightUnit) ? "px" : heightUnit);
//            sb.append("; ");
//        }
        return sb.toString();
    }
}
