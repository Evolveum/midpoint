/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component;

import java.io.Serial;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.Strings;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Panel for form input hints. It shows short version of the hint and allows to expand it to full text if needed.
 *
 * @author Viliam Repan (lazyman)
 */
public class HelpTextPanel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTENT = "content";
    private static final String ID_MORE = "more";

    private boolean alwaysShowAll = false;

    private int shortHelpLength = 100;

    private final IModel<Boolean> showAll = Model.of(false);

    public HelpTextPanel(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    public boolean isAlwaysShowAll() {
        return alwaysShowAll;
    }

    public void setAlwaysShowAll(boolean alwaysShowAll) {
        this.alwaysShowAll = alwaysShowAll;
    }

    public int getShortHelpLength() {
        return shortHelpLength;
    }

    public void setShortHelpLength(int shortHelpLength) {
        this.shortHelpLength = shortHelpLength;
    }

    private void initLayout() {
        setOutputMarkupId(true);

        add(AttributeAppender.append("class", "form-text text-muted text-help"));
        add(new VisibleBehaviour(() -> StringUtils.isNotBlank(getModelObject())));

        Label content = new Label(ID_CONTENT, getContentModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
                if (isShowAll()) {
                    CharSequence body = Strings.toMultilineMarkup(getDefaultModelObjectAsString());
                    replaceComponentTagBody(markupStream, openTag, body);
                } else {
                    super.onComponentTagBody(markupStream, openTag);
                }
            }
        };
        content.setRenderBodyOnly(true);
        add(content);

        AjaxButton more = new AjaxButton(ID_MORE, createMoreModel()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onMoreClicked(target);
            }
        };
        more.add(new VisibleBehaviour(() -> !alwaysShowAll || hasLongHelp()));
        add(more);
    }

    private boolean hasLongHelp() {
        String content = getModelObject();
        return content != null && content.length() > shortHelpLength;
    }

    private void onMoreClicked(AjaxRequestTarget target) {
        showAll.setObject(!showAll.getObject());

        target.add(this);
    }

    private IModel<String> createMoreModel() {
        return () -> {
            String key = showAll.getObject() ? "HelpTextPanel.less" : "HelpTextPanel.more";
            return getString(key);
        };
    }

    private boolean isShowAll() {
        return alwaysShowAll || showAll.getObject();
    }

    private IModel<String> getContentModel() {
        return new LoadableDetachableModel<>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                String content = getModelObject();
                if (content == null) {
                    return null;
                }

                if (isShowAll()) {
                    return content.trim();
                }

                return StringUtils.abbreviate(content.trim(), "...", shortHelpLength);
            }
        };
    }
}
