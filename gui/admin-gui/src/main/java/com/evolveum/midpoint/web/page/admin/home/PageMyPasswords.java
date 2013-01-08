/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.home;

import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.input.PasswordPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.home.dto.MyPasswordsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageMyPasswords extends PageAdminHome {

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_ACCOUNTS = "accounts";
    private static final String ID_PASSWORD_PANEL = "passwordPanel";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_BACK = "back";
    private static final String ID_SAVE = "save";

    private IModel<MyPasswordsDto> model;

    public PageMyPasswords() {
        model = new LoadableModel<MyPasswordsDto>(false) {

            @Override
            protected MyPasswordsDto load() {
                return loadPageModel();
            }
        };

        initLayout();
    }

    private MyPasswordsDto loadPageModel() {
        MyPasswordsDto dto = new MyPasswordsDto();
        dto.getAccounts().add(new PasswordAccountDto("1", "name", true));
        //todo implement
        return dto;
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        List<IColumn<PasswordAccountDto>> columns = initColumns();
        ListDataProvider<PasswordAccountDto> provider = new ListDataProvider<PasswordAccountDto>(this,
                new PropertyModel<List<PasswordAccountDto>>(model, MyPasswordsDto.F_ACCOUNTS));
        TablePanel accounts = new TablePanel(ID_ACCOUNTS, provider, columns);
        accounts.setShowPaging(false);
        mainForm.add(accounts);

        PasswordPanel passwordPanel = new PasswordPanel(ID_PASSWORD_PANEL, new Model<String>());
        mainForm.add(passwordPanel);

        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        feedback.setFilter(new ComponentFeedbackMessageFilter(passwordPanel.getBaseFormComponent()));
        feedback.setOutputMarkupId(true);
        mainForm.add(feedback);

        initButtons(mainForm);
    }

    private List<IColumn<PasswordAccountDto>> initColumns() {
        List<IColumn<PasswordAccountDto>> columns = new ArrayList<IColumn<PasswordAccountDto>>();

        IColumn column = new CheckBoxHeaderColumn<UserType>();
        columns.add(column);

        column = new PropertyColumn(createStringResource("PageMyPasswords.name"), PasswordAccountDto.F_DISPLAY_NAME);
        columns.add(column);

        CheckBoxColumn enabled = new CheckBoxColumn(createStringResource("PageMyPasswords.enabled"),
                PasswordAccountDto.F_ENABLED);
        enabled.setEnabled(false);
        columns.add(enabled);

        return columns;
    }

    private void initButtons(Form mainForm) {
        AjaxSubmitLinkButton save = new AjaxSubmitLinkButton(ID_SAVE, ButtonType.POSITIVE,
                createStringResource("PageMyPasswords.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(save);

        AjaxLinkButton back = new AjaxLinkButton(ID_BACK, createStringResource("PageMyPasswords.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(back);
    }

    private void savePerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        setResponsePage(PageDashboard.class);
    }
}
