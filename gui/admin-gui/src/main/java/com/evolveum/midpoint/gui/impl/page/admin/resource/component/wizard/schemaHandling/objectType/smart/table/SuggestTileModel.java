/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ObjectClassWrapper;

import com.evolveum.midpoint.web.component.util.SelectableBean;

import org.apache.cxf.common.util.StringUtils;
import org.jetbrains.annotations.NotNull;

public class SuggestTileModel<S> extends Tile<SelectableBean<ObjectClassWrapper>> {

    String icon;
    String name;
    String description;
    String count;

    public SuggestTileModel(String icon, String title) {
        super(icon, title);
    }

    public SuggestTileModel(
            @NotNull SelectableBean<ObjectClassWrapper> objectClassWrapper) {
        setValue(objectClassWrapper);
        this.icon = GuiStyleConstants.CLASS_ICON_OUTLIER;
        this.name = extractName(objectClassWrapper);
        this.description = "Description for this object class is not ready yet, but it will be available soon."; // TODO
        this.count = null;
    }


    private String extractName(@NotNull SelectableBean<ObjectClassWrapper> wrapper) {
        String rawName = wrapper.getValue().getObjectClassNameAsString();
        return StringUtils.capitalize(rawName);
    }

        @Override
        public String getIcon () {
            return icon;
        }

        @Override
        public void setIcon (String icon){
            this.icon = icon;
        }

        public String getName () {
            return name;
        }

        public void setName (String name){
            this.name = name;
        }

        public String getDescription () {
            return description;
        }

        public void setDescription (String description){
            this.description = description;
        }

        public String getCount() {
            return count;
        }

        public void setCount(String count) {
            this.count = count;
        }
    }
