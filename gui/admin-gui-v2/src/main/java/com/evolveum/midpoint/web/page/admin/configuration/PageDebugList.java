package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.ButtonColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.option.OptionContent;
import com.evolveum.midpoint.web.component.option.OptionItem;
import com.evolveum.midpoint.web.component.option.OptionPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PageDebugList extends PageAdminConfiguration {

    public PageDebugList() {
        initLayout();
    }

    private void initLayout() {
        List<IColumn<TaskType>> columns = new ArrayList<IColumn<TaskType>>();

        IColumn column = new CheckBoxColumn<TaskType>() {

            @Override
            public void onUpdateHeader(AjaxRequestTarget target) {
                //todo implement
            }

            @Override
            public void onUpdateRow(AjaxRequestTarget target, IModel<Selectable<TaskType>> rowModel) {
                //todo implement
            }
        };
        columns.add(column);

        column = new LinkColumn<Selectable<TaskType>>(createStringResource("pageDebugList.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<Selectable<TaskType>> rowModel) {
                TaskType role = rowModel.getObject().getValue();
                objectEditPerformed(target, role.getOid());
            }
        };
        columns.add(column);

        column = new ButtonColumn<Selectable<TaskType>>(createStringResource("pageDebugList.operation"),
                createStringResource("pageDebugList.button.delete")) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<Selectable<TaskType>> rowModel) {
                TaskType role = rowModel.getObject().getValue();
                deletePerformed(target, role.getOid());
            }
        };
        columns.add(column);

        Form main = new Form("mainForm");
        add(main);

        OptionPanel option = new OptionPanel("option", createStringResource("pageDebugList.optionsTitle"));
        main.add(option);

        OptionItem item = new OptionItem("category", createStringResource("pageDebugList.selectType"));
        option.getBodyContainer().add(item);
        initCategory(item);

        OptionContent content = new OptionContent("optionContent");
        main.add(content);
        content.getBodyContainer().add(new TablePanel<TaskType>("table", TaskType.class, columns));

        AjaxLinkButton button = new AjaxLinkButton("deleteAll",
                createStringResource("pageDebugList.button.deleteAll")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteAllPerformed(target);
            }
        };
        main.add(button);
    }

    private void initCategory(OptionItem item) {
        IChoiceRenderer<ObjectTypes> renderer = new IChoiceRenderer<ObjectTypes>() {

            @Override
            public Object getDisplayValue(ObjectTypes object) {
                return new StringResourceModel(object.getLocalizationKey(),
                        (PageBase) PageDebugList.this, null).getString();
            }

            @Override
            public String getIdValue(ObjectTypes object, int index) {
                return object.getClassDefinition().getSimpleName();
            }
        };

        ListChoice choice = new ListChoice("choice", new Model(), createChoiceModel(renderer), renderer);
        choice.setMaxRows(5);

        item.getBodyContainer().add(choice);

        AjaxLinkButton button = new AjaxLinkButton("listButton",
                createStringResource("pageDebugList.button.listObjects")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                listObjectsPerformed(target);
            }
        };
        item.getBodyContainer().add(button);
    }

    private IModel<List<ObjectTypes>> createChoiceModel(final IChoiceRenderer<ObjectTypes> renderer) {
        return new LoadableModel<List<ObjectTypes>>(false) {

            @Override
            protected List<ObjectTypes> load() {
                List<ObjectTypes> choices = new ArrayList<ObjectTypes>();
                Collections.addAll(choices, ObjectTypes.values());
                Collections.sort(choices, new Comparator<ObjectTypes>() {

                    @Override
                    public int compare(ObjectTypes o1, ObjectTypes o2) {
                        String str1 = (String) renderer.getDisplayValue(o1);
                        String str2 = (String) renderer.getDisplayValue(o2);
                        return String.CASE_INSENSITIVE_ORDER.compare(str1, str2);
                    }
                });

                return choices;
            }
        };
    }
    
    private void deleteAllPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void listObjectsPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void objectEditPerformed(AjaxRequestTarget target, String oid) {
        //todo implement
    }

    private void deletePerformed(AjaxRequestTarget target, String oid) {
        //todo implement
    }
}
