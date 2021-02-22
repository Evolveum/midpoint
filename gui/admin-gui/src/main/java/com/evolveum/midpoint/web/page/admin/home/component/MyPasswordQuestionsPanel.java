/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordQuestionsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SecurityQuestionAnswerDTO;

public class MyPasswordQuestionsPanel extends InputPanel {

    public static final String ID_QUESTION = "questionTF";
    public static final String ID_ANSWER = "answerTF";

    public MyPasswordQuestionsPanel(String id, IModel<SecurityQuestionAnswerDTO> model) {
        super(id);
        initLayout(model);
    }

    public void initLayout(IModel<SecurityQuestionAnswerDTO> model) {
        final Label question = new Label(ID_QUESTION, new PropertyModel<String>(model,
                SecurityQuestionAnswerDTO.F_PASSWORD_QUESTION_ITSELF));
        question.setOutputMarkupId(true);
        add(question);

        final TextField<String> answer = new TextField<String>(ID_ANSWER, new PropertyModel(model,
                SecurityQuestionAnswerDTO.F_PASSWORD_QUESTION_ANSWER));
        answer.setOutputMarkupId(true);
        add(answer);
    }

    @Override
    public List<FormComponent> getFormComponents() {
        List<FormComponent> list = new ArrayList<>();
        list.add((FormComponent) get(ID_QUESTION));
        list.add((FormComponent) get(ID_ANSWER));

        return list;
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_ANSWER);
    }
}
