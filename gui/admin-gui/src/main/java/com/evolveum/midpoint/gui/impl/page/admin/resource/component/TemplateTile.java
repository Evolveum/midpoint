/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.impl.component.tile.Tile;

import org.jetbrains.annotations.NotNull;

/**
 * @author lskublik
 */
public class TemplateTile<O> extends Tile {

    private final O templateObject;

    private String description;

    private String tag;

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

    public String getTag() {
        return tag;
    }

    public TemplateTile tag(String tag) {
        this.tag = tag;
        return this;
    }

    @Override
    public int compareTo(@NotNull Tile o) {
        int comparison = this.getTitle().compareTo(o.getTitle());
        if (comparison == 0 && o instanceof TemplateTile) {
            return this.getTag().compareTo(((TemplateTile)o).getTag());
        }
        return comparison;
    }
}
