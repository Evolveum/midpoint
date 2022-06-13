/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.impl.component.tile.Tile;

/**
 * @author lskublik
 */
public class TemplateTile<O> extends Tile {

    private final O templateObject;

    private String description;

    public TemplateTile(String icon, String title, O templateObject) {
        super(icon, title);
        this.templateObject = templateObject;
    }

    public O getTemplateObject() {
        return templateObject;
    }

    public String getDescription() {
        return description;
    }

    public TemplateTile description(String description) {
        this.description = description;
        return this;
    }
}
