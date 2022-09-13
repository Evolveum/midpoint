/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.validator.RangeValidator;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.SearchFormEnterBehavior;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PagingSizePanel extends BasePanel<Integer> {

    private static final long serialVersionUID = 1L;
    private static final String ID_INPUT = "input";
    private static final String ID_SUBMIT = "submit";
    private static final String ID_INCREMENT = "increment";
    private static final String ID_DECREMENT = "decrement";

    private boolean small;

    public PagingSizePanel(String id) {
        super(id, null);

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        checkComponentTag(tag, "div");

        super.onComponentTag(tag);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(OnDomReadyHeaderItem.forScript(createIncrementScript(ID_INCREMENT, 5)));
        response.render(OnDomReadyHeaderItem.forScript(createIncrementScript(ID_DECREMENT, -5)));
    }

    private String createIncrementScript(String buttonId, int increment) {
        return "$(function() { $('#" + get(buttonId).getMarkupId() + "').click( "
                + "function() { "
                + "MidPointTheme.increment('" + get(ID_INPUT).getMarkupId() + "', '"
                + get(ID_INCREMENT).getMarkupId() + "','"
                + get(ID_DECREMENT).getMarkupId() + "', " +
                increment + "); "
                + "}); });";
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "paging-size"));
        add(AttributeAppender.prepend("class",
                () -> StringUtils.joinWith(" ", "input-group", small ? "input-group-sm" : "", "flex-nowrap w-auto")));

        AjaxSubmitLink submit = new AjaxSubmitLink(ID_SUBMIT) {

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getPageBase().getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                onPageSizeChangePerformed(target);
            }
        };
        add(submit);

        TextField input = new TextField(ID_INPUT, getModel());
        input.add(new RangeValidator<>(5, 100));
        input.setLabel(createStringResource("PageSizePopover.title"));
        input.add(new SearchFormEnterBehavior(submit));
        input.setType(Integer.class);
        input.setOutputMarkupId(true);
        add(input);

        WebMarkupContainer increment = new WebMarkupContainer(ID_INCREMENT);
        increment.add(AttributeAppender.append("class", () -> getModelObject() >= 100 ? "disabled" : null));
        increment.setOutputMarkupId(true);
        add(increment);

        WebMarkupContainer decrement = new WebMarkupContainer(ID_DECREMENT);
        decrement.add(AttributeAppender.append("class", () -> getModelObject() <= 5 ? "disabled" : null));
        decrement.setOutputMarkupId(true);
        add(decrement);
    }

    protected void onPageSizeChangePerformed(AjaxRequestTarget target) {

    }

    @Override
    public IModel<Integer> createModel() {
        return new IModel<>() {

            @Override
            public Integer getObject() {
                Table tablePanel = findParent(Table.class);
                UserProfileStorage.TableId tableId = tablePanel.getTableId();
                if (tableId == null || !tablePanel.enableSavePageSize()) {
                    return tablePanel.getItemsPerPage();
                }

                return getPageBase().getSessionStorage().getUserProfile().getPagingSize(tableId);
            }

            @Override
            public void setObject(Integer o) {
                Table tablePanel = findParent(Table.class);
                UserProfileStorage.TableId tableId = tablePanel.getTableId();
                if (tableId == null || !tablePanel.enableSavePageSize()) {
                    tablePanel.setItemsPerPage(o);
                    return;
                }

                getPageBase().getSessionStorage().getUserProfile().setPagingSize(tableId, o);
            }

            @Override
            public void detach() {
            }
        };
    }

    public boolean isSmall() {
        return small;
    }

    public void setSmall(boolean small) {
        this.small = small;
    }
}
