/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.menu;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RichHyperlinkType;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by Kate on 19.09.2016.
 */
public class AdditionalMenuItem extends MainMenuItem {

    private String targetUrl;

    public AdditionalMenuItem(RichHyperlinkType link, Class<? extends PageBase> page) {
        super(link.getLabel(), link.getIcon() == null ? BaseMenuItem.DEFAULT_ICON : link.getIcon().getCssClass(), page);

        this.targetUrl = link.getTargetUrl();
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    @Override
    protected boolean isNotEmpty() {
        return super.isNotEmpty() || StringUtils.isNotEmpty(targetUrl);
    }
}
