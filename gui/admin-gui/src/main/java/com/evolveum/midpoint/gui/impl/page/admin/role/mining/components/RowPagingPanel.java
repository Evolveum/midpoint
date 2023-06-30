/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import com.evolveum.midpoint.gui.api.page.PageBase;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RowPagingPanel extends Panel {

    private static final String ID_FORM_TABLES = "table_dropdown";

    private static final String ID_DROPDOWN_TABLE = "dropdown_choice";

    private static final String ID_SUBMIT_DROPDOWN = "ajax_submit_link_dropdown";

    private static final List<Integer> SEARCH_ENGINES = List.of(new Integer[] { 100, 200, 400 });
    public Integer selected = 100;

    public RowPagingPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    private @NotNull Form<?> choiceTableForm() {

        DropDownChoice<Integer> listSites = new DropDownChoice<>(
                ID_DROPDOWN_TABLE, new PropertyModel<>(this, "selected"), SEARCH_ENGINES);

        Form<?> formDropdown = new Form<Void>(ID_FORM_TABLES);
        formDropdown.setOutputMarkupId(true);

        AjaxSubmitLink ajaxSubmitDropdown = new AjaxSubmitLink(ID_SUBMIT_DROPDOWN, formDropdown) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                onSubmitDropDown(target, selected);
            }
        };

        formDropdown.add(listSites);
        formDropdown.add(ajaxSubmitDropdown);
        return formDropdown;
    }

    public TextField<?> getBaseFormComponent() {
        return (TextField<?>) get(ID_FORM_TABLES);
    }

    public DropDownChoice<?> getDropDownComponent() {
        return (DropDownChoice<?>) get(((PageBase) getPage()).createComponentPath(ID_FORM_TABLES, ID_DROPDOWN_TABLE));
    }

    public DropDownChoice<?> getSubmitButton() {
        return (DropDownChoice<?>) get(((PageBase) getPage()).createComponentPath(ID_FORM_TABLES, ID_SUBMIT_DROPDOWN));
    }

    public void onSubmitDropDown(AjaxRequestTarget target, Integer selected) {
        System.out.println(selected);
    }
}
