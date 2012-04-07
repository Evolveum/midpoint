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
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
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
        List<IColumn<? extends ObjectType>> columns = new ArrayList<IColumn<? extends ObjectType>>();

        IColumn column = new CheckBoxColumn<ObjectType>() {

            @Override
            public void onUpdateHeader(AjaxRequestTarget target) {
                TablePanel table = getListTable();
                DataTable data = table.getDataTable();
                
                System.out.println("alllll");
                //todo implement
            }

            @Override
            public void onUpdateRow(AjaxRequestTarget target, IModel<Selectable<ObjectType>> rowModel) {
                //todo implement
            }
        };
        columns.add(column);

        column = new LinkColumn<Selectable<? extends ObjectType>>(createStringResource("pageDebugList.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<Selectable<? extends ObjectType>> rowModel) {
                ObjectType object = rowModel.getObject().getValue();
                objectEditPerformed(target, object.getOid());
            }
        };
        columns.add(column);

        column = new ButtonColumn<Selectable<? extends ObjectType>>(createStringResource("pageDebugList.operation"),
                createStringResource("pageDebugList.button.delete")) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<Selectable<? extends ObjectType>> rowModel) {
                ObjectType object = rowModel.getObject().getValue();
                deletePerformed(target, object.getOid());
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
        TablePanel table = new TablePanel("table", UserType.class, columns);
        table.setOutputMarkupId(true);
        content.getBodyContainer().add(table);

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

        final IModel<ObjectTypes> choice = new Model<ObjectTypes>();
        ListChoice listChoice = new ListChoice("choice", choice, createChoiceModel(renderer), renderer, 5) {

            @Override
            protected CharSequence getDefaultChoice(String selectedValue) {
                return "";
            }
        };
        listChoice.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                //it's here just to update model
            }
        });
        item.getBodyContainer().add(listChoice);

        AjaxLinkButton button = new AjaxLinkButton("listButton",
                createStringResource("pageDebugList.button.listObjects")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                listObjectsPerformed(target, choice);
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
    
    private TablePanel getListTable() {
        OptionContent content = (OptionContent) get("mainForm:optionContent");
        return (TablePanel) content.getBodyContainer().get("table");
    }

    private void listObjectsPerformed(AjaxRequestTarget target, IModel<ObjectTypes> selected) {
        TablePanel table = getListTable();

        ObjectTypes type = selected.getObject();
        if (type != null) {
            table.setType(type.getClassDefinition());
        }
        target.add(table);
    }

    private void objectEditPerformed(AjaxRequestTarget target, String oid) {
        //todo implement
    }

    private void deletePerformed(AjaxRequestTarget target, String oid) {
        //todo implement
    }
}
