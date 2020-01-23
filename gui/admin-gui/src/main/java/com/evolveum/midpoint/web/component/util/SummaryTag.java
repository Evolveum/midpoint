/*
 * Copyright (c) 2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.util;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.Containerable;

/**
 * @author semancik
 */
public abstract class SummaryTag<C extends Containerable> extends Panel {

    private static final String ID_TAG_ICON = "summaryTagIcon";
    private static final String ID_TAG_LABEL = "summaryTagLabel";

    private boolean initialized = false;
    private String cssClass;
    private String iconCssClass;
    private String label;
    private String color = null;
    private boolean hideTag = false;

    public SummaryTag(String id, final IModel<C> model) {
        super(id, model);

        Label tagIcon = new Label(ID_TAG_ICON, "");
        tagIcon.add(new AttributeModifier("class", new SummaryTagModel<String>(model) {
            @Override
            protected String getValue() {
                return getIconCssClass();
            }
        }));
        add(tagIcon);

        add(new Label(ID_TAG_LABEL, new SummaryTagModel<String>(model) {
            @Override
            protected String getValue() {
                return getLabel();
            }
        }));

        add(new AttributeModifier("style", new SummaryTagModel<String>(model) {
            @Override
            protected String getValue() {
                if (getColor() == null) {
                    return null;
                }
                return "color: " + getColor();
            }
        }));

        add(new AttributeModifier("class", new SummaryTagModel<String>(model) {
            @Override
            protected String getValue() {
                return getCssClass();
            }
        }));

        add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                if (!initialized) {
                    initialize(model.getObject());
                }
                return !isHideTag();
            }
        });
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getIconCssClass() {
        return iconCssClass;
    }

    public void setIconCssClass(String iconCssClass) {
        this.iconCssClass = iconCssClass;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isHideTag() {
        return hideTag;
    }

    public void setHideTag(boolean hideTag) {
        this.hideTag = hideTag;
    }

    protected abstract void initialize(C objectWrapper);

    abstract class SummaryTagModel<T> implements IModel<T> {

        IModel<C> objectModel;

        public SummaryTagModel(IModel<C> objectModel) {
            this.objectModel = objectModel;
        }

        @Override
        public T getObject() {
            if (!initialized) {
                initialize(objectModel.getObject());
            }
            return getValue();
        }

        protected abstract T getValue();

    }

}
