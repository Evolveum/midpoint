/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

        AjaxLink tableColumns = new AjaxLink(ID_TABLE_COLUMNS) {

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

        Form form = new Form(ID_FORM);
        popover.add(form);

        AjaxSubmitButton button = new AjaxSubmitButton(ID_BUTTON) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(TableConfigurationPanel.this.get(createComponentPath(ID_POPOVER, ID_FORM, "inputFeedback")));
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
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
                UserProfileStorage.TableId tableId = tablePanel.getTableId();
                if (tableId == null) {
                    return tablePanel.getItemsPerPage();
                }

                return getPageBase().getSessionStorage().getUserProfile().getPagingSize(tableId);
            }

            @Override
            public void setObject(Integer o) {
                Table tablePanel = findParent(Table.class);
                UserProfileStorage.TableId tableId = tablePanel.getTableId();
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
