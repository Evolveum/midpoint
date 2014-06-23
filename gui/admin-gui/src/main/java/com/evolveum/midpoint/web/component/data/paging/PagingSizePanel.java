/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.data.paging;

import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.SearchFormEnterBehavior;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;

/**
 *  @author shood
 * */
public class PagingSizePanel extends SimplePanel {

    private static final String ID_FORM_PAGING = "pagingForm";
    private static final String ID_PAGING_SIZE = "pagingSize";
    private static final String ID_SET_PAGING_BUTTON = "setPagingButton";

    private IModel<Integer> pagingModel;

    public PagingSizePanel(String id, final UserProfileStorage.TableId tableId){
        super(id);

        pagingModel = new LoadableModel<Integer>() {

            @Override
            protected Integer load() {
                return getPageBase().getSessionStorage().getUserProfile().getPagingSize(tableId);
            }
        };

        Form pagingForm = new Form(ID_FORM_PAGING);
        pagingForm.setOutputMarkupId(true);
        add(pagingForm);

        initLayout(pagingForm);
    }

    public Integer getPagingSize(){
        return pagingModel.getObject();
    }

    private void initLayout(Form form){
        AjaxSubmitButton setPagingButton = new AjaxSubmitButton(ID_SET_PAGING_BUTTON,
                createStringResource("PagingSizePanel.button.set")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form){
                pagingSizeChangePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form){
                target.add(getFeedbackPanel());
            }
        };
        form.add(setPagingButton);

        final TextField<Integer> pagingText = new TextField<Integer>(ID_PAGING_SIZE, pagingModel);
        pagingText.setType(Integer.class);
        pagingText.add(AttributeModifier.replace("placeholder", createStringResource("PagingSizePanel.label.pagingSize")));
        pagingText.add(new SearchFormEnterBehavior(setPagingButton));
        form.add(pagingText);
    }

    private Component getFeedbackPanel() {
        return getPageBase().getFeedbackPanel();
    }

    protected void pagingSizeChangePerformed(AjaxRequestTarget target){}


}