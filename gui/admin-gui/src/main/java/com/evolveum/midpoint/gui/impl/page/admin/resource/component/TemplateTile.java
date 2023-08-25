/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;

import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class TemplateTile<T extends Serializable> extends Tile<T> {

    private List<DisplayType> tags = new ArrayList<>();

    public TemplateTile(T templateObject) {
        this(null, null, templateObject);
    }

    public TemplateTile(String icon, String title, T templateObject) {
        super(icon, title);
        setValue(templateObject);
    }

    public TemplateTile<T> description(String description) {
        setDescription(description);
        return this;
    }

    public TemplateTile<T> selected(boolean selected) {
        setSelected(selected);
        return this;
    }

    public List<DisplayType> getTags() {
        return tags;
    }

    public TemplateTile<T> addTag(DisplayType display) {
        tags.add(display);
        return this;
    }

    public TemplateTile<T> addTags(List<DisplayType> tags) {
        this.tags.addAll(tags);
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

    public static <O extends ObjectType> TemplateTile<SelectableBean<O>> createTileFromObject(SelectableBean<O> object, PageBase pageBase) {
        O obj = object.getValue();
        PrismObject prism = obj != null ? obj.asPrismObject() : null;
        String icon = IconAndStylesUtil.createDefaultColoredIcon(prism.getValue().getTypeName());

        String description = object.getValue().getDescription();
        if (obj instanceof UserType) {
            DisplayType displayType = GuiDisplayTypeUtil.getArchetypePolicyDisplayType(obj, pageBase);
            if (displayType != null && displayType.getLabel() != null) {
                description = WebComponentUtil.getTranslatedPolyString(displayType.getLabel());
            }
        }

        TemplateTile<SelectableBean<O>> t = new TemplateTile<>(
                icon, WebComponentUtil.getDisplayNameOrName(prism), object)
                .description(description);
        t.setSelected(object.isSelected());

        return t;
    }

    @Override
    public TemplateTile<T> clone() {
        return new TemplateTile<>(getIcon(), getTitle(), getValue())
                .description(getDescription())
                .selected(isSelected())
                .addTags(getTags());
    }
}
