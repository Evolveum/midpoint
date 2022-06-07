/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.search.Popover;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.SearchFormEnterBehavior;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.validator.RangeValidator;

/**
 * @author Viliam Repan (lazyman)
 */
public class TableConfigurationPanel extends BasePanel {

    private static final String ID_COG = "cog";
    private static final String ID_POPOVER = "popover";
    private static final String ID_FORM = "form";
    private static final String ID_INPUT = "input";
    private static final String ID_BUTTON = "button";

    public TableConfigurationPanel(String id) {
        super(id);
        setRenderBodyOnly(true);
        initLayout();
    }

    protected void initLayout() {
        Popover popover = initPopoverLayout();

        AjaxLink cog = new AjaxLink<>(ID_COG) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                popover.toggle(target);
            }
        };
        cog.setOutputMarkupId(true);
        add(cog);

        popover.setReference(cog);
    }

    private Popover initPopoverLayout() {
        Popover popover = new Popover(ID_POPOVER);
        popover.setOutputMarkupId(true);
        popover.setReference(popover);
        add(popover);

        Form<?> form = new MidpointForm<>(ID_FORM);
        popover.add(form);

        AjaxSubmitButton button = new AjaxSubmitButton(ID_BUTTON) {

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(TableConfigurationPanel.this.get(createComponentPath(ID_POPOVER, ID_FORM, "inputFeedback")));
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                pageSizeChanged(target);
            }
        };
        form.add(button);

        TextField<?> input = new TextField<>(ID_INPUT, createInputModel());
        input.add(new RangeValidator<>(5, 100));
        input.setLabel(createStringResource("PageSizePopover.title"));
        input.add(new SearchFormEnterBehavior(button));
        input.setType(Integer.class);
        input.setOutputMarkupId(true);

        FeedbackPanel feedback = new FeedbackPanel("inputFeedback", new ComponentFeedbackMessageFilter(input));
        feedback.setOutputMarkupId(true);
        form.add(feedback);
        form.add(input);

        return popover;
    }

    private IModel<Integer> createInputModel() {
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

    protected void pageSizeChanged(AjaxRequestTarget target) {
    }
}
