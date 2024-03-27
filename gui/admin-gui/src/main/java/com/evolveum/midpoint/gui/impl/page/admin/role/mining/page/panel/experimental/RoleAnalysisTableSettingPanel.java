/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.experimental;

import java.io.Serial;
import java.util.*;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
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

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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

    LoadableDetachableModel<DisplayValueOption> option;
    RoleAnalysisSortMode sortMode;
    RoleAnalysisChunkMode selectedTableMode;

    boolean isPathEnable = true;
    LoadableDetachableModel<ItemPathDto> itemPathRoleDto = new LoadableDetachableModel<>() {
        @Serial private static final long serialVersionUID = 1L;

        @Override
        protected @NotNull ItemPathDto load() {
            ItemPath itemPath = ItemPath.create(RoleType.F_NAME);
            return new ItemPathDto(itemPath, null, RoleType.COMPLEX_TYPE);
        }
    };

    LoadableDetachableModel<ItemPathDto> itemPathUserDto = new LoadableDetachableModel<>() {
        @Serial private static final long serialVersionUID = 1L;

        @Override
        protected @NotNull ItemPathDto load() {
            ItemPath itemPath = ItemPath.create(UserType.F_NAME);
            return new ItemPathDto(itemPath, null, UserType.COMPLEX_TYPE);
        }
    };

    public RoleAnalysisTableSettingPanel(String id,
            IModel<String> messageModel,
            LoadableDetachableModel<DisplayValueOption> option) {
        super(id, messageModel);
        this.option = option;

        RoleAnalysisChunkMode chunkMode = option.getObject().getChunkMode();
        if (chunkMode.equals(RoleAnalysisChunkMode.COMPRESS)) {
            isPathEnable = false;
        }

        ItemPath userItemValuePath = option.getObject().getUserItemValuePath();
        if (userItemValuePath == null || !isPathEnable) {
            userItemValuePath = ItemPath.create(UserType.F_NAME);
        }
        ItemPath roleItemValuePath = option.getObject().getRoleItemValuePath();
        if (roleItemValuePath == null || !isPathEnable) {
            roleItemValuePath = ItemPath.create(RoleType.F_NAME);
        }

        itemPathUserDto.setObject(new ItemPathDto(userItemValuePath, null, UserType.COMPLEX_TYPE));
        itemPathRoleDto.setObject(new ItemPathDto(roleItemValuePath, null, RoleType.COMPLEX_TYPE));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initSortingSetting();

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
                option.getObject().setSortMode(sortMode);
                option.getObject().setChunkMode(selectedTableMode);
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

        ItemPathPanel componentsRolePath = new ItemPathPanel(ID_SELECTOR_ROLE, itemPathRoleDto) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(ItemPathDto itemPathDto) {
                if (option.getObject() == null) {
                    option.setObject(new DisplayValueOption());
                }
                option.getObject().setRoleItemValuePath(new ItemPathType(itemPathDto.toItemPath()).getItemPath());
            }

            @Override
            protected QName getRequaredObjectType() {
                return RoleType.COMPLEX_TYPE;
            }

            @Override
            protected boolean isResetButtonVisible() {
                return isPathEnable;
            }

            @Override
            public boolean collectItems(Collection<? extends ItemDefinition> definitions,
                    String input, Map<String,
                    ItemDefinition<?>> toSelect) {
                if (definitions == null) {
                    return true;
                }

                for (ItemDefinition<?> def : definitions) {
                    if (isEligibleItemDefinition(def)) {
                        putDefinition(def, input, toSelect);
                    }
                }

                return true;
            }

            @Override
            protected boolean setDefaultItemPath() {
                return super.setDefaultItemPath();
            }

            @Override
            public boolean isPlusButtonVisible() {
                return false;
            }

            @Override
            public boolean isMinusButtonVisible() {
                return false;
            }

            @Override
            protected boolean isNamespaceEnable() {
                return false;
            }

        };

        componentsRolePath.setOutputMarkupId(true);

        componentsRolePath.setEnabled(isPathEnable);

        add(componentsRolePath);
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

        ItemPathPanel componentsUserPath = new ItemPathPanel(ID_SELECTOR_USER, itemPathUserDto) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(ItemPathDto itemPathDto) {
                if (option.getObject() == null) {
                    option.setObject(new DisplayValueOption());
                }
                option.getObject().setUserItemValuePath(new ItemPathType(itemPathDto.toItemPath()).getItemPath());
            }

            @Override
            protected QName getRequaredObjectType() {
                return UserType.COMPLEX_TYPE;
            }

            @Override
            protected boolean isResetButtonVisible() {
                return isPathEnable;
            }

            @Override
            public boolean collectItems(
                    Collection<? extends ItemDefinition> definitions,
                    String input,
                    Map<String, ItemDefinition<?>> toSelect) {
                if (definitions == null) {
                    return true;
                }

                for (ItemDefinition<?> def : definitions) {
                    if (isEligibleItemDefinition(def)) {
                        putDefinition(def, input, toSelect);
                    }
                }

                return true;
            }

            @Override
            public boolean isPlusButtonVisible() {
                return false;
            }

            @Override
            public boolean isMinusButtonVisible() {
                return false;
            }

            @Override
            protected boolean isNamespaceEnable() {
                return false;
            }

        };
        componentsUserPath.setOutputMarkupId(true);

        componentsUserPath.setEnabled(isPathEnable);

        add(componentsUserPath);
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

        selectedTableMode = option.getObject().getChunkMode();
        IModel<RoleAnalysisChunkMode> selectedModeModel = Model.of(selectedTableMode);

        DropDownChoice<RoleAnalysisChunkMode> tableModeSelector = new DropDownChoice<>(
                ID_SELECTOR_TABLE_MODE, selectedModeModel,
                new ArrayList<>(EnumSet.allOf(RoleAnalysisChunkMode.class)), renderer);

        tableModeSelector.setOutputMarkupId(true);
        tableModeSelector.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                RoleAnalysisChunkMode selectedMode = selectedModeModel.getObject();
                isPathEnable = !selectedMode.equals(RoleAnalysisChunkMode.COMPRESS);
                selectedTableMode = selectedMode;
                ItemPathPanel componentUserSelector = (ItemPathPanel) RoleAnalysisTableSettingPanel.this.get(ID_SELECTOR_ROLE);
                componentUserSelector.setEnabled(isPathEnable);
                ItemPathPanel componentRoleSelector = (ItemPathPanel) RoleAnalysisTableSettingPanel.this.get(ID_SELECTOR_USER);
                componentRoleSelector.setEnabled(isPathEnable);

                if (isPathEnable) {
                    ItemPath userItemValuePath = option.getObject().getUserItemValuePath();
                    if (userItemValuePath == null) {
                        userItemValuePath = ItemPath.create(UserType.F_NAME);
                    }
                    itemPathUserDto.setObject(new ItemPathDto(userItemValuePath, null, UserType.COMPLEX_TYPE));
                    componentUserSelector.setDefaultModel(itemPathUserDto);

                    ItemPath roleItemValuePath = option.getObject().getRoleItemValuePath();
                    if (roleItemValuePath == null) {
                        roleItemValuePath = ItemPath.create(RoleType.F_NAME);
                    }
                    itemPathRoleDto.setObject(new ItemPathDto(roleItemValuePath, null, RoleType.COMPLEX_TYPE));
                    componentRoleSelector.setDefaultModel(itemPathRoleDto);
                }

                target.add(componentUserSelector);
                target.add(componentRoleSelector);
            }
        });
        tableModeSelector.setOutputMarkupId(true);
        add(tableModeSelector);
    }

    public void putDefinition(ItemDefinition<?> def, String input, Map<String, ItemDefinition<?>> toSelect) {
        if (StringUtils.isBlank(input)) {
            toSelect.put(def.getItemName().getLocalPart(), def);
        } else {
            if (def.getItemName().getLocalPart().startsWith(input)) {
                toSelect.put(def.getItemName().getLocalPart(), def);
            }
        }
    }

    private boolean isEligibleItemDefinition(@NotNull ItemDefinition<?> def) {
        return def.isSingleValue() && (isPolyStringOrString(def) || isReference(def));
    }

    private boolean isPolyStringOrString(@NotNull ItemDefinition<?> def) {
        return def.getTypeName().getLocalPart().equals(PolyStringType.COMPLEX_TYPE.getLocalPart())
                || def.getTypeName().getLocalPart().equals("string")
                || def.getTypeName().getLocalPart().equals("PolyString");
    }

    private boolean isReference(@NotNull ItemDefinition<?> def) {
        return def.getTypeName().getLocalPart().equals(ObjectReferenceType.COMPLEX_TYPE.getLocalPart());
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
