/*
 * Copyright (c) 2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.util;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.Containerable;

// todo cleanup needed
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

        add(AttributeAppender.append("class", "summary-tag d-flex gap-1 border rounded bg-light px-2 m-1"));

        Label tagIcon = new Label(ID_TAG_ICON, "");
        tagIcon.add(new AttributeModifier("class", new SummaryTagModel<String>(model) {
            @Override
            protected String getValue() {
                return getIconCssClass();
            }
        }));
        add(tagIcon);

        IModel<String> labelModel = new SummaryTagModel<String>(model) {
            @Override
            protected String getValue() {
                return getLabel();
            }
        };
        Label tagLabel = new Label(ID_TAG_LABEL, labelModel);
        tagLabel.add(AttributeAppender.append("title", labelModel));
        add(tagLabel);

        add(new AttributeModifier("style", new SummaryTagModel<String>(model) {
            @Override
            protected String getValue() {
                if (getColor() == null) {
                    return null;
                }
                return "color: " + getColor() + " !important;";
            }
        }));

        add(AttributeAppender.append("class", new SummaryTagModel<String>(model) {
            @Override
            protected String getValue() {
                return getCssClass();
            }
        }));

        add(new VisibleBehaviour(() -> {
            if (!initialized) {
                initialize(model.getObject());
            }
            return !isHideTag();
        }));
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
