package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.ButtonColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.option.OptionContent;
import com.evolveum.midpoint.web.component.option.OptionItem;
import com.evolveum.midpoint.web.component.option.OptionPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.security.MidPointApplication;
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
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PageDebugList extends PageAdminConfiguration {

    private static final String DOT_CLASS = PageDebugList.class.getName() + ".";
    private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
    private static final String OPERATION_DELETE_OBJECTS = DOT_CLASS + "deleteObjects";

    public PageDebugList() {
        initLayout();
    }

    private void initLayout() {
        //listed type
        final IModel<ObjectTypes> choice = new Model<ObjectTypes>();

        List<IColumn<? extends ObjectType>> columns = new ArrayList<IColumn<? extends ObjectType>>();

        IColumn column = new CheckBoxHeaderColumn<ObjectType>();
        columns.add(column);

        column = new LinkColumn<SelectableBean<? extends ObjectType>>(createStringResource("pageDebugList.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<? extends ObjectType>> rowModel) {
                ObjectType object = rowModel.getObject().getValue();
                objectEditPerformed(target, object.getOid());
            }
        };
        columns.add(column);

        column = new ButtonColumn<SelectableBean<? extends ObjectType>>(createStringResource("pageDebugList.operation"),
                createStringResource("pageDebugList.button.delete")) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<? extends ObjectType>> rowModel) {
                ObjectType object = rowModel.getObject().getValue();
                deletePerformed(target, choice, object);
            }
        };
        columns.add(column);

        Form main = new Form("mainForm");
        add(main);

        OptionPanel option = new OptionPanel("option", createStringResource("pageDebugList.optionsTitle"));
        main.add(option);

        OptionItem item = new OptionItem("category", createStringResource("pageDebugList.selectType"));
        option.getBodyContainer().add(item);
        initCategory(item, choice);

        OptionContent content = new OptionContent("optionContent");
        main.add(content);
        TablePanel table = new TablePanel("table", new ObjectDataProvider(PageDebugList.this, UserType.class), columns);
        table.setOutputMarkupId(true);
        content.getBodyContainer().add(table);

        AjaxLinkButton button = new AjaxLinkButton("deleteAll",
                createStringResource("pageDebugList.button.deleteAll")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteAllPerformed(target, choice);
            }
        };
        main.add(button);
    }

    private void initCategory(OptionItem item, final IModel<ObjectTypes> choice) {
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

    private TablePanel getListTable() {
        OptionContent content = (OptionContent) get("mainForm:optionContent");
        return (TablePanel) content.getBodyContainer().get("table");
    }

    private void listObjectsPerformed(AjaxRequestTarget target, IModel<ObjectTypes> selected) {
        TablePanel table = getListTable();

        ObjectTypes type = selected.getObject();
        if (type != null) {
            ObjectDataProvider provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
            provider.setType(type.getClassDefinition());
        }
        target.add(table);
    }

    private void objectEditPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(PageDebugView.PARAM_OBJECT_ID, oid);
        setResponsePage(PageDebugView.class, parameters);
    }

    private List<ObjectType> getSelectedObjects() {
        TablePanel tablePanel = getListTable();
        DataTable table = tablePanel.getDataTable();
        ObjectDataProvider<ObjectType> provider = (ObjectDataProvider<ObjectType>) table.getDataProvider();

        List<ObjectType> selected = new ArrayList<ObjectType>();
        for (SelectableBean<ObjectType> row : provider.getAvailableData()) {
            if (row.isSelected()) {
                selected.add(row.getValue());
            }
        }

        return selected;
    }

    private void deleteAllPerformed(AjaxRequestTarget target, IModel<ObjectTypes> choice) {
        MidPointApplication application = getMidpointApplication();
        RepositoryService repository = application.getRepository();
        ObjectTypes type = choice.getObject();

        OperationResult result = new OperationResult(OPERATION_DELETE_OBJECT);
        for (ObjectType object : getSelectedObjects()) {
            OperationResult subResult = result.createSubresult(OPERATION_DELETE_OBJECT);
            try {
                repository.deleteObject(type.getClassDefinition(), object.getOid(), subResult);
                subResult.recordSuccess();
            } catch (Exception ex) {
                subResult.recordFatalError("Couldn't delete object.", ex);
            }
        }
        result.recomputeStatus();

        showResult(result);
        target.add(getListTable());
        target.add(getFeedbackPanel());
    }

    private void deletePerformed(AjaxRequestTarget target, IModel<ObjectTypes> choice, ObjectType object) {
        MidPointApplication application = getMidpointApplication();
        RepositoryService repository = application.getRepository();

        OperationResult result = new OperationResult(OPERATION_DELETE_OBJECT);
        try {
            ObjectTypes type = choice.getObject();
            repository.deleteObject(type.getClassDefinition(), object.getOid(), result);
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't delete object '" + object.getName() + "'.", ex);
        }

        showResult(result);
        target.add(getListTable());
        target.add(getFeedbackPanel());
    }
}
