/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.experimental;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Select2MultiChoice;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkAction;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.ObjectSimpleAttributeSelectionProvider;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class RoleAnalysisTableSettingPanel extends BasePanel<String> implements Popupable {

    private static final String ID_SELECTOR_ROLE = "selector_role";
    private static final String ID_SELECTOR_USER = "selector_user";
    private static final String ID_SAVE_BUTTON = "save_button";
    private static final String ID_SORT_MODE_SELECTOR = "sortModeSelector";
    private static final String ID_SORT_LABEL = "sortLabel";
    private static final String ID_SELECTOR_USER_HEADER_LABEL = "userHeaderLabel";
    private static final String ID_SELECTOR_ROLE_HEADER_LABEL = "roleHeaderLabel";
    private static final String ID_SELECTOR_TABLE_MODE = "tableModeSelector";
    private static final String ID_TABLE_MODE_LABEL = "tableModeLabel";
    private static final String ID_ACTION_MODE_LABEL = "actionModeLabel";
    private static final String ID_ACTION_MODE_SELECTOR = "actionModeSelector";

    IModel<DisplayValueOption> option;
    RoleAnalysisSortMode sortMode;
    RoleAnalysisChunkMode selectedTableMode;

    RoleAnalysisChunkAction chunkAction;

    boolean isUserExpanded = false;
    boolean isRoleExpanded = false;

    LoadableDetachableModel<RoleAnalysisAttributeDef> userAnalysisAttributeDef;

    LoadableDetachableModel<RoleAnalysisAttributeDef> roleAnalysisAttributeDef;

    public RoleAnalysisTableSettingPanel(
            @NotNull String id,
            @NotNull IModel<String> messageModel,
            @NotNull IModel<DisplayValueOption> option) {
        super(id, messageModel);
        this.option = option;

        //TODO models initialization is not good
        if (option.getObject() == null) {
            option.setObject(new DisplayValueOption());
        } else {
            selectedTableMode = option.getObject().getChunkMode();
            updateBasedExpandedStatus(selectedTableMode);

            RoleAnalysisAttributeDef userAnalysisUserDef = option.getObject().getUserAnalysisUserDef();
            if (userAnalysisUserDef != null) {
                userAnalysisAttributeDef.setObject(userAnalysisUserDef);
            }

            RoleAnalysisAttributeDef roleAnalysisRoleDef = option.getObject().getRoleAnalysisRoleDef();
            if (roleAnalysisRoleDef != null) {
                roleAnalysisAttributeDef.setObject(roleAnalysisRoleDef);
            }
        }
    }

    private void updateBasedExpandedStatus(RoleAnalysisChunkMode selectedTableMode) {
        switch (selectedTableMode) {
            case COMPRESS -> {
                isUserExpanded = false;
                isRoleExpanded = false;
            }
            case EXPAND_USER -> {
                isUserExpanded = true;
                isRoleExpanded = false;
            }
            case EXPAND_ROLE -> {
                isUserExpanded = false;
                isRoleExpanded = true;
            }
            case EXPAND -> {
                isUserExpanded = true;
                isRoleExpanded = true;
            }
        }
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initSortingSetting();

        initActionModeSetting();

        initTableModeSetting();

        initRoleHeaderSelector();

        initUserHeaderSelector();

        initSavaButton();
    }

    private void initSavaButton() {
        AjaxButton saveButton = new AjaxButton(ID_SAVE_BUTTON,
                getPageBase().createStringResource("RoleAnalysisTableSettingPanel.saveButton")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                DisplayValueOption displayOption = option.getObject();
                displayOption.setSortMode(sortMode);
                displayOption.setChunkMode(selectedTableMode);
                displayOption.setChunkAction(chunkAction);
                performAfterFinish(target);
                onClose(target);
            }
        };
        saveButton.setOutputMarkupId(true);
        add(saveButton);
    }

    private void initRoleHeaderSelector() {
        RoleAnalysisProcessModeType processMode = option.getObject().getProcessMode();
        StringResourceModel labelTitle;
        StringResourceModel labelHelp;

        if (processMode != null && processMode.equals(RoleAnalysisProcessModeType.USER)) {
            labelTitle = getPageBase()
                    .createStringResource("RoleAnalysisTableSettingPanel.selector.row.header");
            labelHelp = getPageBase()
                    .createStringResource("RoleAnalysisTableSettingPanel.selector.row.header.help");
        } else {
            labelTitle = getPageBase()
                    .createStringResource("RoleAnalysisTableSettingPanel.selector.column.header");
            labelHelp = getPageBase()
                    .createStringResource("RoleAnalysisTableSettingPanel.selector.column.header.help");
        }

        LabelWithHelpPanel labelWithHelpPanel = new LabelWithHelpPanel(ID_SELECTOR_ROLE_HEADER_LABEL, labelTitle) {
            @Override
            protected IModel<String> getHelpModel() {
                return labelHelp;
            }
        };
        labelWithHelpPanel.setOutputMarkupId(true);
        add(labelWithHelpPanel);

        //TODO decide what to do with this?

        ChoiceProvider<ItemPathType> choiceProvider = new ObjectSimpleAttributeSelectionProvider(RoleType.COMPLEX_TYPE);

        Select2MultiChoice<ItemPathType> multiselect = new Select2MultiChoice<>(ID_SELECTOR_ROLE,
                initSelectedModel(roleAnalysisAttributeDef),
                choiceProvider);

        multiselect.getSettings()
                .setMinimumInputLength(0)
                .setMaximumSelectionLength(1);
        multiselect.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateRoleAnalysisAttributeDefModel(multiselect.getModel().getObject());
            }
        });
        multiselect.setOutputMarkupId(true);
        multiselect.setEnabled(isRoleExpanded);
        add(multiselect);

    }

    private void initUserHeaderSelector() {
        RoleAnalysisProcessModeType processMode = option.getObject().getProcessMode();
        StringResourceModel labelTitle;
        StringResourceModel labelHelp;

        if (processMode != null && processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            labelTitle = getPageBase()
                    .createStringResource("RoleAnalysisTableSettingPanel.selector.row.header");
            labelHelp = getPageBase()
                    .createStringResource("RoleAnalysisTableSettingPanel.selector.row.header.help");
        } else {
            labelTitle = getPageBase()
                    .createStringResource("RoleAnalysisTableSettingPanel.selector.column.header");
            labelHelp = getPageBase()
                    .createStringResource("RoleAnalysisTableSettingPanel.selector.column.header.help");
        }
        LabelWithHelpPanel labelWithHelpPanel = new LabelWithHelpPanel(ID_SELECTOR_USER_HEADER_LABEL, labelTitle) {
            @Override
            protected IModel<String> getHelpModel() {
                return labelHelp;
            }
        };
        labelWithHelpPanel.setOutputMarkupId(true);
        add(labelWithHelpPanel);

        //TODO mark good
        //TODO decide what to do with this?
        ChoiceProvider<ItemPathType> choiceProvider = new ObjectSimpleAttributeSelectionProvider(UserType.COMPLEX_TYPE);

        Select2MultiChoice<ItemPathType> multiselect = new Select2MultiChoice<>(ID_SELECTOR_USER,
                initSelectedModel(userAnalysisAttributeDef),
                choiceProvider);

        multiselect.getSettings()
                .setMinimumInputLength(0)
                .setMaximumSelectionLength(1);
        multiselect.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateUserAnalysisAttributeDefModel(multiselect.getModel().getObject());
            }
        });
        multiselect.setOutputMarkupId(true);
        multiselect.setEnabled(isUserExpanded);
        add(multiselect);
    }

    private void updateUserAnalysisAttributeDefModel(Collection<ItemPathType> selected) {
        PrismObjectDefinition<?> userDefinition = PrismContext.get().getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(UserType.class);

        if (option.getObject() == null) {
            option.setObject(new DisplayValueOption());
        }
        for (ItemPathType pathType : selected) {
            ItemPath path = pathType.getItemPath();
            ItemDefinition<?> itemDefinition = userDefinition.findItemDefinition(path);

            userAnalysisAttributeDef.setObject(new RoleAnalysisAttributeDef(path, itemDefinition, UserType.class));
            option.getObject().setUserAnalysisUserDef(userAnalysisAttributeDef.getObject());
        }
    }

    private void updateRoleAnalysisAttributeDefModel(Collection<ItemPathType> selected) {
        PrismObjectDefinition<?> roleDefinition = PrismContext.get().getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(RoleType.class);

        if (option.getObject() == null) {
            option.setObject(new DisplayValueOption());
        }
        for (ItemPathType pathType : selected) {
            ItemPath path = pathType.getItemPath();
            ItemDefinition<?> itemDefinition = roleDefinition.findItemDefinition(path);

            roleAnalysisAttributeDef.setObject(new RoleAnalysisAttributeDef(path, itemDefinition, RoleType.class));
            option.getObject().setRoleAnalysisRoleDef(roleAnalysisAttributeDef.getObject());
        }
    }

    private LoadableModel<Collection<ItemPathType>> initSelectedModel(LoadableDetachableModel<RoleAnalysisAttributeDef> model) {
        return new LoadableModel<>(false) {

            @Override
            protected Collection<ItemPathType> load() {
                return Collections.singleton(model.getObject()
                        .getPath().toBean());
            }
        };
    }

    public void initSortingSetting() {

        LabelWithHelpPanel labelWithHelpPanel = new LabelWithHelpPanel(ID_SORT_LABEL,
                getPageBase().createStringResource("RoleAnalysisTableSettingPanel.selector.sortMode")) {
            @Override
            protected IModel<String> getHelpModel() {
                return getPageBase().createStringResource("RoleAnalysisTableSettingPanel.selector.sortMode.help");
            }
        };
        labelWithHelpPanel.setOutputMarkupId(true);
        add(labelWithHelpPanel);

        ChoiceRenderer<RoleAnalysisSortMode> renderer = new ChoiceRenderer<>("displayString");

        sortMode = option.getObject().getSortMode();
        IModel<RoleAnalysisSortMode> selectedModeModel = Model.of(sortMode);

        DropDownChoice<RoleAnalysisSortMode> sortModeSelector = new DropDownChoice<>(
                ID_SORT_MODE_SELECTOR, selectedModeModel,
                new ArrayList<>(EnumSet.allOf(RoleAnalysisSortMode.class)), renderer);

        sortModeSelector.setOutputMarkupId(true);
        sortModeSelector.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                sortMode = selectedModeModel.getObject();
            }
        });
        sortModeSelector.setOutputMarkupId(true);
        add(sortModeSelector);
    }

    public void initActionModeSetting() {
        LabelWithHelpPanel labelWithHelpPanel = new LabelWithHelpPanel(ID_ACTION_MODE_LABEL,
                getPageBase().createStringResource("RoleAnalysisTableSettingPanel.selector.actionMode")) {
            @Override
            protected IModel<String> getHelpModel() {
                return getPageBase().createStringResource("RoleAnalysisTableSettingPanel.selector.actionMode.help");
            }
        };
        labelWithHelpPanel.setOutputMarkupId(true);
        add(labelWithHelpPanel);

        ChoiceRenderer<RoleAnalysisChunkAction> renderer = new ChoiceRenderer<>("displayString");
        chunkAction = option.getObject().getChunkAction();
        IModel<RoleAnalysisChunkAction> selectedActionModel = Model.of(chunkAction);

        DropDownChoice<RoleAnalysisChunkAction> chunkActionSelector = new DropDownChoice<>(
                ID_ACTION_MODE_SELECTOR, selectedActionModel,
                new ArrayList<>(EnumSet.allOf(RoleAnalysisChunkAction.class)), renderer);

        chunkActionSelector.setOutputMarkupId(true);
        chunkActionSelector.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                chunkAction = selectedActionModel.getObject();
            }
        });
        chunkActionSelector.setOutputMarkupId(true);
        add(chunkActionSelector);
    }

    public void initTableModeSetting() {

        LabelWithHelpPanel labelWithHelpPanel = new LabelWithHelpPanel(ID_TABLE_MODE_LABEL,
                getPageBase().createStringResource("RoleAnalysisTableSettingPanel.selector.tableMode")) {
            @Override
            protected IModel<String> getHelpModel() {
                return getPageBase().createStringResource("RoleAnalysisTableSettingPanel.selector.tableMode.help");
            }
        };
        labelWithHelpPanel.setOutputMarkupId(true);
        add(labelWithHelpPanel);

        ChoiceRenderer<RoleAnalysisChunkMode> renderer = new ChoiceRenderer<>("displayString");

        IModel<RoleAnalysisChunkMode> selectedModeModel = Model.of(selectedTableMode);

        DropDownChoice<RoleAnalysisChunkMode> tableModeSelector = new DropDownChoice<>(
                ID_SELECTOR_TABLE_MODE, selectedModeModel,
                new ArrayList<>(EnumSet.allOf(RoleAnalysisChunkMode.class)), renderer);

        tableModeSelector.setOutputMarkupId(true);
        tableModeSelector.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                selectedTableMode = selectedModeModel.getObject();
                updateBasedExpandedStatus(selectedTableMode);
                Select2MultiChoice<?> componentUserSelector = (Select2MultiChoice<?>) RoleAnalysisTableSettingPanel
                        .this.get(ID_SELECTOR_ROLE);
                componentUserSelector.setEnabled(isRoleExpanded);
                Select2MultiChoice<?> componentRoleSelector = (Select2MultiChoice<?>) RoleAnalysisTableSettingPanel
                        .this.get(ID_SELECTOR_USER);
                componentRoleSelector.setEnabled(isUserExpanded);

                //TODO decide what to do with this?
//                if (isRoleExpanded) {
//                    componentRoleSelector.setDefaultModel(Model.of(getObjectNameDef()));
//                }
//
//                if (isUserExpanded) {
//                    componentUserSelector.setDefaultModel(Model.of(getObjectNameDef()));
//                }

                target.add(componentUserSelector);
                target.add(componentRoleSelector);
            }
        });
        tableModeSelector.setOutputMarkupId(true);
        add(tableModeSelector);
    }

    public void performAfterFinish(AjaxRequestTarget target) {
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    public boolean isActive() {
        return false;
    }

    public RoleAnalysisProgressIncrement getHandler() {
        return null;
    }

    @Override
    public int getWidth() {
        return 30;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("RoleAnalysisTableSettingPanel.title");
    }

}
