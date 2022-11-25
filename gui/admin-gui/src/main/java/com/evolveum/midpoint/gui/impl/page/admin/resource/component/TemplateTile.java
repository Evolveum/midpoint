/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.impl.component.tile.Tile;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class TemplateTile<T extends Serializable> extends Tile<T> {

    private String description;

    private List<DisplayType> tags = new ArrayList<>();

    public TemplateTile(String icon, String title, T templateObject) {
        super(icon, title);
        setValue(templateObject);
    }

    public String getDescription() {
        return description;
    }

    public TemplateTile description(String description) {
        this.description = description;
        return this;
    }

    public List<DisplayType> getTags() {
        return tags;
    }

    public TemplateTile addTag(DisplayType display) {
        tags.add(display);
        return this;
    }

    @Override
    public int compareTo(@NotNull Tile o) {
        int comparison = this.getTitle().compareTo(o.getTitle());
        if (comparison == 0 && o instanceof TemplateTile) {
            if (this.getTags().isEmpty() && ((TemplateTile)o).getTags().isEmpty()) {
                return 0;
            }

            PolyStringType label1 = this.getTags().isEmpty() ? null : this.getTags().get(0).getLabel();
            PolyStringType label2 = ((TemplateTile)o).getTags().isEmpty() ? null : ((DisplayType)((TemplateTile)o).getTags().get(0)).getLabel();

            if (label1 == null) {
                return -1;
            }

            if (label2 == null) {
                return 1;
            }

            return label1.getOrig().compareTo(label2.getOrig());
        }
        return comparison;
    }
}
