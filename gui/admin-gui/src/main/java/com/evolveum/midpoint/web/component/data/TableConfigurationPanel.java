/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.SearchFormEnterBehavior;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.validator.RangeValidator;

/**
 * @author Viliam Repan (lazyman)
 */
public class TableConfigurationPanel extends BasePanel {

    private static final String ID_COG_BUTTON = "cogButton";
    private static final String ID_PAGE_SIZE = "pageSize";
    private static final String ID_TABLE_COLUMNS = "tableColumns";
    private static final String ID_POPOVER = "popover";
    private static final String ID_FORM = "form";
    private static final String ID_INPUT = "input";
    private static final String ID_BUTTON = "button";

//    private static final String ID_FEEDBACK = "feedback";

    public TableConfigurationPanel(String id) {
        super(id);
        setRenderBodyOnly(true);
        initLayout();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        StringBuilder sb = new StringBuilder();
        sb.append("initPageSizePopover('");
        sb.append(get(createComponentPath(ID_COG_BUTTON, ID_PAGE_SIZE)).getMarkupId());
        sb.append("','").append(get(ID_POPOVER).getMarkupId());
        sb.append("','").append(get(ID_COG_BUTTON).getMarkupId());
        sb.append("');");

        response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
    }

//    @Override
    protected void initLayout() {
        WebMarkupContainer cogButton = new WebMarkupContainer(ID_COG_BUTTON);
        cogButton.setOutputMarkupId(true);
        add(cogButton);

        WebMarkupContainer pageSize = new WebMarkupContainer(ID_PAGE_SIZE);
        pageSize.setOutputMarkupId(true);
        cogButton.add(pageSize);

        AjaxLink<Void> tableColumns = new AjaxLink<Void>(ID_TABLE_COLUMNS) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                tableColumnsPerformed(target);
            }
        };
        cogButton.add(tableColumns);
        tableColumns.setVisible(false); //todo implement [lazyman]

        initPopoverLayout();
    }

    private void initPopoverLayout() {
        WebMarkupContainer popover = new WebMarkupContainer(ID_POPOVER);
        popover.setOutputMarkupId(true);
        add(popover);

        Form form = new com.evolveum.midpoint.web.component.form.Form(ID_FORM);
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

        TextField input = new TextField(ID_INPUT, createInputModel());
        input.add(new RangeValidator(5, 100));
        input.setLabel(createStringResource("PageSizePopover.title"));
        input.add(new SearchFormEnterBehavior(button));
        input.setType(Integer.class);
        input.setOutputMarkupId(true);

        FeedbackPanel feedback = new FeedbackPanel("inputFeedback", new ComponentFeedbackMessageFilter(input));
        feedback.setOutputMarkupId(true);
        form.add(feedback);
        form.add(input);
    }

    private void tableColumnsPerformed(AjaxRequestTarget target) {
        //todo implement table columns support [lazyman]
    }

    private IModel<Integer> createInputModel() {
        return new IModel<Integer>() {

            @Override
            public Integer getObject() {
                Table tablePanel = findParent(Table.class);
                UserProfileStorage.TableId tableId = tablePanel.getTableIdKey();
                if (tableId == null) {
                    return tablePanel.getItemsPerPage();
                }

                return getPageBase().getSessionStorage().getUserProfile().getPagingSize(tableId);
            }

            @Override
            public void setObject(Integer o) {
                Table tablePanel = findParent(Table.class);
                UserProfileStorage.TableId tableId = tablePanel.getTableIdKey();
                if (tableId == null) {
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

    protected void onPageSizeChangedError(AjaxRequestTarget target) {

    }
}
