/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import java.util.ArrayList;
import java.util.EnumSet;

import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.page.admin.configuration.component.PageDebugDownloadBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.RoleMiningExportOperation;

public class ExportMiningPanel extends ConfirmationPanel {

    private static final long serialVersionUID = 1L;
    private static final String ID_KEY_FIELD = "keyField";
    private static final String ID_KEY_GENERATE = "keyGenerateButton";
    private static final String ID_FORM_EXPORT_OPTIONS = "exportOptions";
    private static final String ID_SUBMIT_EXPORT_OBTIONS = "submitExportOptions";
    private static final String ID_APPLICATION_PREFIX = "applicationPrefix";
    private static final String ID_APPLICATION_SUFFIX = "applicationSuffix";
    private static final String ID_BUSINESS_PREFIX = "businessPrefix";
    private static final String ID_BUSINESS_SUFFIX = "businessSuffix";
    private static final String ID_CHECKBOX_ROLE = "roleCheckbox";
    private static final String ID_CHECKBOX_USER = "userCheckbox";
    private static final String ID_CHECKBOX_ORG = "orgCheckbox";
    private static final String ID_CHECKBOX_ZIP = "zipCheckBox";
    protected static final String ID_DROPDOWN_NAME_MODE = "choiceNameMode";
    protected static final String ID_SHOW_ADDITIONAL_OPTIONS = "showAdditionalOptions";
    protected static final String ID_LABEL_EXPORT_OBJECT = "exportObjectLabel";
    protected static final String ID_LABEL_ZIP = "zipCheckBoxLabel";
    protected static final String ID_LABEL_NAME_MODE = "nameModeLabel";
    protected static final String ID_LABEL_APPLICATION = "applicationLabel";
    protected static final String ID_LABEL_BUSINESS = "businessLabel";
    protected static final String ID_LABEL_ENCRYPT = "encryptLabel";
    protected static final String ID_LINK_DOCUMENTATION = "link";

    private TextField<String> applicationPrefixField;
    private String applicationPrefix;
    private TextField<String> applicationSuffixField;
    private String applicationSuffix;
    private TextField<String> businessPrefixField;
    private String businessPrefix;
    private TextField<String> businessSuffixField;
    private String businessSuffix;
    boolean showEmpty = false;
    private String key;
    private boolean edit = false;
    boolean zip = false;
    boolean roleExport = true;
    boolean orgExport = true;
    boolean userExport = true;
    RoleMiningExportOperation.NameModeExport selectedValue;
    protected PageDebugDownloadBehaviour<?> downloadBehaviour;

    public ExportMiningPanel(String id, IModel<String> message, PageDebugDownloadBehaviour<?> roleMiningExport) {
        super(id, message);
        this.downloadBehaviour = roleMiningExport;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        ExternalLink link = new ExternalLink(ID_LINK_DOCUMENTATION, "https://docs.evolveum.com/");
        link.add(AttributeModifier.append("target", "_blank"));
        link.setBody(Model.of("Evolveum Documentation."));

        add(link);
    }

    @Override
    protected void customInitLayout(WebMarkupContainer panel) {
        addEncryptKeyFields(panel);

        addNameModeDropdown(panel);
        addAjaxZipCheckBox(panel);

        addExportOptionObjects(panel);

        Form<?> form = new Form<>(ID_FORM_EXPORT_OPTIONS);
        form.setOutputMarkupId(true);
        form.setOutputMarkupPlaceholderTag(true);
        form.setVisible(showEmpty);
        panel.add(form);

        LabelWithHelpPanel applicationOptionsLabel = getLabelWithHelp(ID_LABEL_APPLICATION,
                createStringResource("roleMiningExportPanel.application.role.label"),
                createStringResource("roleMiningExportPanel.application.role.label.help"));
        form.add(applicationOptionsLabel);

        LabelWithHelpPanel businessOptionsLabel = getLabelWithHelp(ID_LABEL_BUSINESS,
                createStringResource("roleMiningExportPanel.business.role.label"),
                createStringResource("roleMiningExportPanel.business.role.label.help"));
        form.add(businessOptionsLabel);

        AjaxButton showAdditionalOptions = getShowAdditionalOptionsButton(form);
        panel.add(showAdditionalOptions);

        applicationPrefixField = optionsTextField(ID_APPLICATION_PREFIX, Model.of(applicationPrefix));
        form.add(applicationPrefixField);
        applicationSuffixField = optionsTextField(ID_APPLICATION_SUFFIX, Model.of(applicationSuffix));
        form.add(applicationSuffixField);
        businessPrefixField = optionsTextField(ID_BUSINESS_PREFIX, Model.of(businessPrefix));
        form.add(businessPrefixField);
        businessSuffixField = optionsTextField(ID_BUSINESS_SUFFIX, Model.of(businessSuffix));
        form.add(businessSuffixField);

        form.add(getAjaxSubmitButton());

    }

    public LabelWithHelpPanel getLabelWithHelp(String id, IModel<String> labelModel, IModel<String> helpModel) {
        LabelWithHelpPanel label = new LabelWithHelpPanel(id, labelModel) {
            @Override
            protected IModel<String> getHelpModel() {
                return helpModel;
            }
        };
        label.setOutputMarkupId(true);
        return label;
    }

    private void addNameModeDropdown(@NotNull WebMarkupContainer panel) {
        selectedValue = RoleMiningExportOperation.NameModeExport.SEQUENTIAL;

        LabelWithHelpPanel dropdownChoiceLabel = getLabelWithHelp(ID_LABEL_NAME_MODE,
                createStringResource("roleMiningExportPanel.export.name.options.title"),
                createStringResource("roleMiningExportPanel.export.name.options.help"));
        panel.add(dropdownChoiceLabel);

        ChoiceRenderer<RoleMiningExportOperation.NameModeExport> renderer = new ChoiceRenderer<>("displayString");

        DropDownChoice<RoleMiningExportOperation.NameModeExport> dropDownChoice = new DropDownChoice<>(ID_DROPDOWN_NAME_MODE,
                Model.of(selectedValue), new ArrayList<>(EnumSet.allOf(RoleMiningExportOperation.NameModeExport.class)), renderer);
        dropDownChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                selectedValue = dropDownChoice.getModelObject();
            }
        });

        panel.add(dropDownChoice);
    }

    private void addAjaxZipCheckBox(@NotNull WebMarkupContainer panel) {
        LabelWithHelpPanel zipCheckBoxLabel = getLabelWithHelp(ID_LABEL_ZIP,
                createStringResource("roleMiningExportPanel.zip.format.title"),
                createStringResource("roleMiningExportPanel.zip.format.title.help"));
        panel.add(zipCheckBoxLabel);
        AjaxCheckBox ajaxCheckBox = new AjaxCheckBox(ID_CHECKBOX_ZIP, Model.of(zip)) {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                zip = getModelObject();
            }
        };
        panel.add(ajaxCheckBox);

    }

    private void addExportOptionObjects(WebMarkupContainer panel) {

        LabelWithHelpPanel exportOptionsLabel = getLabelWithHelp(ID_LABEL_EXPORT_OBJECT,
                createStringResource("roleMiningExportPanel.export.label"),
                createStringResource("roleMiningExportPanel.export.label.help"));
        panel.add(exportOptionsLabel);

        AjaxCheckBox roleExportSelector = new AjaxCheckBox(ID_CHECKBOX_ROLE, Model.of(true)) {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                roleExport = getModelObject();
            }
        };
        roleExportSelector.setOutputMarkupId(true);
        roleExportSelector.setEnabled(false);

        panel.add(roleExportSelector);
        AjaxCheckBox userExportSelector = new AjaxCheckBox(ID_CHECKBOX_USER, Model.of(true)) {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                userExport = getModelObject();
            }
        };
        userExportSelector.setOutputMarkupId(true);
        userExportSelector.setEnabled(false);
        panel.add(userExportSelector);

        AjaxCheckBox orgExportSelector = new AjaxCheckBox(ID_CHECKBOX_ORG, Model.of(true)) {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                orgExport = getModelObject();
            }
        };
        panel.add(orgExportSelector);
    }

    @NotNull
    private AjaxSubmitButton getAjaxSubmitButton() {
        AjaxSubmitButton ajaxSubmitButton = new AjaxSubmitButton(ID_SUBMIT_EXPORT_OBTIONS) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                if (isEdit()) {
                    setEdit(false);
                    setApplicationPrefix(applicationPrefixField.getModelObject());
                    setApplicationSuffix(applicationSuffixField.getModelObject());
                    setBusinessPrefix(businessPrefixField.getModelObject());
                    setBusinessSuffix(businessSuffixField.getModelObject());
                    this.add(AttributeAppender.replace("value", "Edit options"));
                    this.add(AttributeAppender.replace("class", "btn btn-default btn-sm"));
                } else {
                    setEdit(true);
                    this.add(AttributeAppender.replace("value", "Save options"));
                    this.add(AttributeAppender.replace("class", "btn btn-primary btn-sm"));
                }

                updateOptions(target);
                target.add(this);

            }
        };

        ajaxSubmitButton.setOutputMarkupId(true);
        ajaxSubmitButton.setOutputMarkupPlaceholderTag(true);
        return ajaxSubmitButton;
    }

    @NotNull
    private AjaxButton getShowAdditionalOptionsButton(Form<?> form) {
        AjaxButton showAdditionalOptions = new AjaxButton(ID_SHOW_ADDITIONAL_OPTIONS) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showEmpty = !showEmpty;
                form.setVisible(showEmpty);

                target.add(form);
                target.add(this);
            }

            @Override
            public IModel<?> getBody() {
                return getNameOfAdditionalOptionsButton();
            }
        };

        showAdditionalOptions.setOutputMarkupId(true);
        showAdditionalOptions.add(AttributeAppender.append("style", "cursor: pointer;"));
        return showAdditionalOptions;
    }

    private StringResourceModel getNameOfAdditionalOptionsButton() {
        return createStringResource("roleMiningExportPanel.showAdditionalOptions.button." + !showEmpty);
    }

    public void updateOptions(AjaxRequestTarget ajaxRequestTarget) {
        applicationPrefixField.setEnabled(isEdit());
        ajaxRequestTarget.add(applicationPrefixField);
        applicationSuffixField.setEnabled(isEdit());
        ajaxRequestTarget.add(applicationSuffixField);
        businessPrefixField.setEnabled(isEdit());
        ajaxRequestTarget.add(businessPrefixField);
        businessSuffixField.setEnabled(isEdit());
        ajaxRequestTarget.add(businessSuffixField);
    }

    private TextField<String> optionsTextField(String textFieldId, IModel<String> iModel) {
        TextField<String> textField = new TextField<>(textFieldId, iModel);
        textField.setOutputMarkupId(true);
        textField.setOutputMarkupPlaceholderTag(true);
        textField.setEnabled(isEdit());
        return textField;
    }

    private void addEncryptKeyFields(WebMarkupContainer panel) {

        LabelWithHelpPanel encryptLabel = getLabelWithHelp(ID_LABEL_ENCRYPT,
                createStringResource("roleMiningExportPanel.encryption.key.title"),
                createStringResource("roleMiningExportPanel.encryption.key.title.help"));
        panel.add(encryptLabel);

        TextField<Integer> keyField = new TextField<>(ID_KEY_FIELD);
        keyField.setOutputMarkupId(true);
        keyField.setOutputMarkupPlaceholderTag(true);

        generateRandomKey();

        keyField.setDefaultModel(Model.of(getKey()));
        keyField.setEnabled(false);
        panel.add(keyField);

        AjaxButton ajaxButton = new AjaxButton(ID_KEY_GENERATE,
                createStringResource("roleMiningExportPanel.button.generate.title")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                generateRandomKey();
                keyField.setDefaultModel(Model.of(getKey()));
                ajaxRequestTarget.add(keyField);
            }
        };

        panel.add(ajaxButton);
    }

    private void generateRandomKey() {
        setKey(new RoleMiningExportOperation().generateRandomKey());
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("roleMiningExportPanel.title");
    }

    @Override
    protected IModel<String> createYesLabel() {
        return Model.of("Export");
    }

    @Override
    protected IModel<String> createNoLabel() {
        return Model.of("Cancel");
    }

    @Override
    public void yesPerformed(AjaxRequestTarget target) {
        RoleMiningExportOperation roleMiningExportOperation = new RoleMiningExportOperation();
        roleMiningExportOperation.setKey(getKey());
        roleMiningExportOperation.setApplicationRolePrefix(getApplicationPrefix());
        roleMiningExportOperation.setApplicationRoleSuffix(getApplicationSuffix());
        roleMiningExportOperation.setBusinessRolePrefix(getBusinessPrefix());
        roleMiningExportOperation.setBusinessRoleSuffix(getBusinessSuffix());
        roleMiningExportOperation.setNameModeExport(selectedValue);
        roleMiningExportOperation.setOrgExport(orgExport);

        downloadBehaviour.setRoleMiningExport(roleMiningExportOperation);
        downloadBehaviour.setRoleMiningActive(true);
        downloadBehaviour.setQuery(null);
        downloadBehaviour.setUseZip(isZip());
        downloadBehaviour.initiate(target);
        super.yesPerformed(target);
    }

    @Override
    public int getWidth() {
        return 700;
    }

    @Override
    public int getHeight() {
        return 250;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    private String getApplicationPrefix() {
        return applicationPrefix;
    }

    private void setApplicationPrefix(String applicationPrefix) {
        this.applicationPrefix = applicationPrefix;
    }

    private String getApplicationSuffix() {
        return applicationSuffix;
    }

    private void setApplicationSuffix(String applicationSuffix) {
        this.applicationSuffix = applicationSuffix;
    }

    private String getBusinessPrefix() {
        return businessPrefix;
    }

    private void setBusinessPrefix(String businessPrefix) {
        this.businessPrefix = businessPrefix;
    }

    private String getBusinessSuffix() {
        return businessSuffix;
    }

    private void setBusinessSuffix(String businessSuffix) {
        this.businessSuffix = businessSuffix;
    }

    private boolean isEdit() {
        return edit;
    }

    private void setEdit(boolean edit) {
        this.edit = edit;
    }

    private void setKey(String key) {
        this.key = key;
    }

    private String getKey() {
        return key;
    }

    private boolean isZip() {
        return zip;
    }

}
