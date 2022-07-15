/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class SimplePopupable implements Popupable {

    private int width;

    private int height;

    private String withUnit = "px";

    private String heightUnit = "px";

    private IModel<String> title;

    public SimplePopupable(int width, int height, IModel<String> title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public String getWidthUnit() {
        return withUnit;
    }

    @Override
    public String getHeightUnit() {
        return heightUnit;
    }

    @Override
    public IModel<String> getTitle() {
        return title;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setWithUnit(String withUnit) {
        this.withUnit = withUnit;
    }

    public void setHeightUnit(String heightUnit) {
        this.heightUnit = heightUnit;
    }

    public void setTitle(IModel<String> title) {
        this.title = title;
    }
}
