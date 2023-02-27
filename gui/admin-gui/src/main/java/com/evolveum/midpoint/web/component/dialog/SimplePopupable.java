/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class SimplePopupable<T> extends BasePanel<T> implements Popupable {

    private int width;

    private int height;

    private String withUnit = "px";

    private String heightUnit = "px";

    private IModel<String> title;

    public SimplePopupable(String id, IModel<T> model, int width, int height, IModel<String> title) {
        super(id, model);
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public SimplePopupable(String id, int width, int height, IModel<String> title) {
        super(id);
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

    @Override
    public Component getContent() {
        return this;
    }
}
