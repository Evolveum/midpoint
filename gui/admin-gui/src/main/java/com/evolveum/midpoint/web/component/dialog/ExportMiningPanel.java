/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import static com.evolveum.midpoint.model.api.expr.MidpointFunctions.LOGGER;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.RoleMiningExportUtils;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyWrapperModel;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AxiomQueryWrapper;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.PageDebugDownloadBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.RoleMiningExportOperation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class ExportMiningPanel extends BasePanel<String> implements Popupable {

    private static final long serialVersionUID = 1L;

    private static final String ID_INTRODUCTION_TEXT = "introduction";
    private static final String ID_EXPORT_BUTTON = "export";
    private static final String ID_CANCEL_BUTTON = "cancel";
    private static final String ID_PANEL = "panel";
    private static final String ID_WARNING_FEEDBACK = "warningFeedback";
    private static final String ID_KEY_FIELD = "keyField";
    private static final String ID_KEY_GENERATE = "keyGenerateButton";
    private static final String ID_FORM_EXPORT_OPTIONS = "exportOptions";
    private static final String ID_SUBMIT_EXPORT_BUTTON = "submitExportOptions";
    private static final String ID_APPLICATION_PREFIX = "applicationPrefix";
    private static final String ID_APPLICATION_SUFFIX = "applicationSuffix";
    private static final String ID_BUSINESS_PREFIX = "businessPrefix";
    private static final String ID_BUSINESS_SUFFIX = "businessSuffix";
    private static final String ID_CHECKBOX_ROLE = "roleCheckbox";
    private static final String ID_CHECKBOX_USER = "userCheckbox";
    private static final String ID_CHECKBOX_ORG = "orgCheckbox";
    private static final String ID_CHECKBOX_ZIP = "zipCheckBox";
    protected static final String ID_DROPDOWN_NAME_MODE = "choiceNameMode";
    protected static final String ID_DROPDOWN_SECURITY_MODE = "choiceSecurity";
    protected static final String ID_DROPDOWN_ARCHETYPE_APPLICATION = "archetypeApplicationDropdown";
    protected static final String ID_DROPDOWN_ARCHETYPE_BUSINESS = "archetypeBusinessDropdown";
    protected static final String ID_SHOW_ADDITIONAL_OPTIONS = "showAdditionalOptions";
    protected static final String ID_LABEL_EXPORT_OBJECT = "exportObjectLabel";
    protected static final String ID_LABEL_ZIP = "zipCheckBoxLabel";
    protected static final String ID_LABEL_NAME_MODE = "nameModeLabel";
    protected static final String ID_LABEL_SECURITY_MODE = "securityModeLabel";
    protected static final String ID_LABEL_APPLICATION = "applicationLabel";
    protected static final String ID_LABEL_BUSINESS = "businessLabel";
    protected static final String ID_LABEL_ENCRYPT = "encryptLabel";
    protected static final String ID_LINK_DOCUMENTATION = "link";
    protected static final String ID_SUFFIX_ACE_EDITOR = "EditorMidPointScript";
    protected static final String ID_SUFFIX_ACE_SUBMIT_BUTTON = "SubmitButton";

    private static final String APPLICATION_ROLE_ARCHETYPE_OID = "00000000-0000-0000-0000-000000000328";
    private static final String BUSINESS_ROLE_ARCHETYPE_OID = "00000000-0000-0000-0000-000000000321";
    private static final String DELIMITER = ",";

    private boolean editOptions = false;
    boolean isZip = false;
    boolean orgExport = true;
    RoleMiningExportUtils.NameMode nameModeSelected;
    RoleMiningExportUtils.SecurityMode securityModeSelected;
    private String encryptKey;
    private String applicationPrefix;
    private String applicationSuffix;
    private String businessPrefix;
    private String businessSuffix;
    String applicationRoleArchetypeOid;
    String businessRoleArchetypeOid;
    ObjectFilter roleQuery;
    ObjectFilter userQuery;
    ObjectFilter orgQuery;
    OperationResult result;
    protected PageDebugDownloadBehaviour<?> downloadBehaviour;

    public ExportMiningPanel(String id, IModel<String> message, PageDebugDownloadBehaviour<?> roleMiningExport) {
        super(id, message);
        this.downloadBehaviour = roleMiningExport;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Label label = new Label(ID_INTRODUCTION_TEXT, getModel());
        label.setEscapeModelStrings(true);
        add(label);

        ExternalLink externalLink = new ExternalLink(ID_LINK_DOCUMENTATION,
                "https://docs.evolveum.com/midpoint/reference/roles-policies/mining/anonymous-data-export/");
        externalLink.add(AttributeModifier.append("target", "_blank"));
        externalLink.setBody(createStringResource("roleMiningExportPanel.documentation.link"));

        add(externalLink);

        Fragment fragment = new Fragment(Popupable.ID_FOOTER, ID_PANEL, this);
        initLayout(fragment);
        add(fragment);

    }

    protected void initLayout(WebMarkupContainer container) {

        securitySectionPanel(container);

        nameModeSelectorPanel(container);

        zipCheckerPanel(container);

        exportObjectsOptionsPanel(container);

        categoryIdentifierPanel(container);

        objectFiltersPanel(container);

        confirmationButtonsPanel(container);

    }

    private void confirmationButtonsPanel(@NotNull WebMarkupContainer panel) {
        AjaxButton exportButton = new AjaxButton(ID_EXPORT_BUTTON, createYesLabel()) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                exportPerformed(target);
            }
        };
        panel.add(exportButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON, createNoLabel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
        panel.add(cancelButton);
    }

    private void securitySectionPanel(@NotNull WebMarkupContainer panel) {
        securityModeSelected = RoleMiningExportUtils.SecurityMode.ADVANCED;

        LabelWithHelpPanel encryptLabel = getInfoLabelComponent(ID_LABEL_ENCRYPT,
                createStringResource("roleMiningExportPanel.encryption.key.title"),
                createStringResource("roleMiningExportPanel.encryption.key.title.help"));
        panel.add(encryptLabel);

        TextField<Integer> keyField = new TextField<>(ID_KEY_FIELD);
        keyField.setOutputMarkupId(true);
        keyField.setOutputMarkupPlaceholderTag(true);
        generateRandomKey(securityModeSelected);
        keyField.setDefaultModel(Model.of(getEncryptKey()));
        keyField.setEnabled(false);
        panel.add(keyField);

        AjaxButton generateKeyButton = new AjaxButton(ID_KEY_GENERATE,
                createStringResource("roleMiningExportPanel.button.generate.title")) {
            @Override
            public void onClick(@NotNull AjaxRequestTarget ajaxRequestTarget) {
                generateRandomKey(securityModeSelected);
                keyField.setDefaultModel(Model.of(getEncryptKey()));
                ajaxRequestTarget.add(keyField);
            }
        };
        panel.add(generateKeyButton);

        LabelWithHelpPanel securityModeLabel = getInfoLabelComponent(ID_LABEL_SECURITY_MODE,
                createStringResource("roleMiningExportPanel.export.security.options.title"),
                createStringResource("roleMiningExportPanel.export.security.options.help"));
        panel.add(securityModeLabel);

        ChoiceRenderer<RoleMiningExportUtils.SecurityMode> renderer = new ChoiceRenderer<>("displayString");
        DropDownChoice<RoleMiningExportUtils.SecurityMode> securityModeChoice = new DropDownChoice<>(
                ID_DROPDOWN_SECURITY_MODE, Model.of(securityModeSelected),
                new ArrayList<>(EnumSet.allOf(RoleMiningExportUtils.SecurityMode.class)), renderer);
        securityModeChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                securityModeSelected = securityModeChoice.getModelObject();
                generateRandomKey(securityModeSelected);
                keyField.setDefaultModel(Model.of(getEncryptKey()));
                target.add(keyField);
            }
        });
        panel.add(securityModeChoice);
    }

    private void nameModeSelectorPanel(@NotNull WebMarkupContainer panel) {
        nameModeSelected = RoleMiningExportUtils.NameMode.SEQUENTIAL;

        LabelWithHelpPanel dropdownChoiceLabel = getInfoLabelComponent(ID_LABEL_NAME_MODE,
                createStringResource("roleMiningExportPanel.export.name.options.title"),
                createStringResource("roleMiningExportPanel.export.name.options.help"));
        panel.add(dropdownChoiceLabel);

        ChoiceRenderer<RoleMiningExportUtils.NameMode> renderer = new ChoiceRenderer<>("displayString");

        DropDownChoice<RoleMiningExportUtils.NameMode> dropDownChoice = new DropDownChoice<>(
                ID_DROPDOWN_NAME_MODE, Model.of(nameModeSelected),
                new ArrayList<>(EnumSet.allOf(RoleMiningExportUtils.NameMode.class)), renderer);
        dropDownChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                nameModeSelected = dropDownChoice.getModelObject();
            }
        });

        panel.add(dropDownChoice);
    }

    private void zipCheckerPanel(@NotNull WebMarkupContainer panel) {
        LabelWithHelpPanel zipCheckBoxLabel = getInfoLabelComponent(ID_LABEL_ZIP,
                createStringResource("roleMiningExportPanel.zip.format.title"),
                createStringResource("roleMiningExportPanel.zip.format.title.help"));
        panel.add(zipCheckBoxLabel);
        AjaxCheckBox ajaxCheckBox = new AjaxCheckBox(ID_CHECKBOX_ZIP, Model.of(isZip)) {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                isZip = getModelObject();
            }
        };
        panel.add(ajaxCheckBox);
    }

    private void exportObjectsOptionsPanel(@NotNull WebMarkupContainer panel) {
        LabelWithHelpPanel exportOptionsLabel = getInfoLabelComponent(ID_LABEL_EXPORT_OBJECT,
                createStringResource("roleMiningExportPanel.export.label"),
                createStringResource("roleMiningExportPanel.export.label.help"));
        panel.add(exportOptionsLabel);

        CheckBox roleExportSelector = new CheckBox(ID_CHECKBOX_ROLE, Model.of(true));
        roleExportSelector.setOutputMarkupId(true);
        roleExportSelector.setEnabled(false);
        panel.add(roleExportSelector);

        CheckBox userExportSelector = new CheckBox(ID_CHECKBOX_USER, Model.of(true));
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

    private void categoryIdentifierPanel(@NotNull WebMarkupContainer panel) {
        Form<?> form = new Form<>(ID_FORM_EXPORT_OPTIONS);
        form.setOutputMarkupId(true);
        form.setOutputMarkupPlaceholderTag(true);
        form.setVisible(false);
        panel.add(form);

        LabelWithHelpPanel applicationOptionsLabel = getInfoLabelComponent(ID_LABEL_APPLICATION,
                createStringResource("roleMiningExportPanel.application.role.label"),
                createStringResource("roleMiningExportPanel.application.role.label.help"));
        form.add(applicationOptionsLabel);

        LabelWithHelpPanel businessOptionsLabel = getInfoLabelComponent(ID_LABEL_BUSINESS,
                createStringResource("roleMiningExportPanel.business.role.label"),
                createStringResource("roleMiningExportPanel.business.role.label.help"));
        form.add(businessOptionsLabel);

        AjaxButton showAdditionalOptions = showAdditionalOptionsButton(form);
        panel.add(showAdditionalOptions);

        TextField<String> applicationPrefixField = optionsTextField(ID_APPLICATION_PREFIX, Model.of(applicationPrefix));
        form.add(applicationPrefixField);
        TextField<String> applicationSuffixField = optionsTextField(ID_APPLICATION_SUFFIX, Model.of(applicationSuffix));
        form.add(applicationSuffixField);
        TextField<String> businessPrefixField = optionsTextField(ID_BUSINESS_PREFIX, Model.of(businessPrefix));
        form.add(businessPrefixField);
        TextField<String> businessSuffixField = optionsTextField(ID_BUSINESS_SUFFIX, Model.of(businessSuffix));
        form.add(businessSuffixField);

        HashMap<String, String> archetypeMap = getArchetypeObjectsList();
        IModel<String> selectedArchetypeModelApplication = Model.of("");
        IModel<String> selectedArchetypeModelBusiness = Model.of("");

        List<String> archetypeListString = getChoiceArchetypeList(archetypeMap,
                selectedArchetypeModelApplication, selectedArchetypeModelBusiness);

        AutoCompleteTextField<String> autoCompleteApplicationRoleField = applicationRoleAutocompleteField(panel, archetypeMap,
                selectedArchetypeModelApplication, archetypeListString);
        form.add(autoCompleteApplicationRoleField);

        AutoCompleteTextField<String> autoCompleteBusinessRoleField = businessRoleAutocompleteField(panel, archetypeMap,
                selectedArchetypeModelBusiness, archetypeListString);
        form.add(autoCompleteBusinessRoleField);

        AjaxSubmitButton ajaxSubmitButton = new AjaxSubmitButton(ID_SUBMIT_EXPORT_BUTTON) {
            @Override
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget) {
                if (isEditOptions()) {
                    setEditOptions(false);
                    setApplicationPrefix(applicationPrefixField.getModelObject());
                    setApplicationSuffix(applicationSuffixField.getModelObject());
                    setBusinessPrefix(businessPrefixField.getModelObject());
                    setBusinessSuffix(businessSuffixField.getModelObject());
                    this.add(AttributeAppender.replace("value",
                            createStringResource("roleMiningExportPanel.edit.options")));
                    this.add(AttributeAppender.replace("class", "btn btn-default btn-sm"));
                } else {
                    setEditOptions(true);
                    this.add(AttributeAppender.replace("value",
                            createStringResource("roleMiningExportPanel.save.options")));
                    this.add(AttributeAppender.replace("class", "btn btn-primary btn-sm"));
                }

                ajaxRequestTarget.add(autoCompleteApplicationRoleField);
                ajaxRequestTarget.add(autoCompleteBusinessRoleField);
                ajaxRequestTarget.add(applicationPrefixField);
                ajaxRequestTarget.add(applicationSuffixField);
                ajaxRequestTarget.add(businessPrefixField);
                ajaxRequestTarget.add(businessSuffixField);
                ajaxRequestTarget.add(this);
            }
        };

        ajaxSubmitButton.setOutputMarkupId(true);
        ajaxSubmitButton.setOutputMarkupPlaceholderTag(true);
        form.add(ajaxSubmitButton);
    }

    private void showResultFeedback(OperationResult result, @NotNull Component resultPanel, @NotNull AjaxRequestTarget target) {
        this.result = result;
        resultPanel.setVisible(true);
        target.add(resultPanel);
    }

    private void objectFiltersPanel(@NotNull WebMarkupContainer panel) {

        OperationResultPanel operationResultPanel = new OperationResultPanel(ID_WARNING_FEEDBACK,
                new LoadableModel<>() {
                    @Override
                    protected OpResult load() {
                        if (result == null) {
                            result = new OperationResult(getString(("roleMiningExportPanel.operation.query.parse")));
                        }
                        return OpResult.getOpResult(getPageBase(), result);
                    }
                }) {
            @Override
            public void close(@NotNull AjaxRequestTarget target, boolean parent) {
                target.add(this.setVisible(false));
            }
        };
        operationResultPanel.setOutputMarkupPlaceholderTag(true);
        operationResultPanel.setOutputMarkupId(true);
        operationResultPanel.setVisible(false);
        panel.add(operationResultPanel);

        initFilter("roleFilter", panel, RoleType.class, operationResultPanel);
        initFilter("userFilter", panel, UserType.class, operationResultPanel);
        initFilter("orgFilter", panel, OrgType.class, operationResultPanel);
    }

    private void initFilter(String formId, @NotNull WebMarkupContainer panel, @NotNull Class<?> objectType,
            OperationResultPanel operationResultPanel) {

        Form<?> filterForm = new Form<>(formId);
        filterForm.setOutputMarkupId(true);
        filterForm.setOutputMarkupPlaceholderTag(true);
        filterForm.setVisible(false);

        NonEmptyModel<AxiomQueryWrapper> filterModel = new NonEmptyWrapperModel<>(
                new Model<>(new AxiomQueryWrapper(null)));

        TextField<String> queryDslField = new TextField<>(formId + ID_SUFFIX_ACE_EDITOR,
                new PropertyModel<>(filterModel, AxiomQueryWrapper.F_DSL_QUERY));
        queryDslField.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        queryDslField.add(AttributeAppender.append("placeholder",
                createStringResource("roleMiningExportPanel.insertAxiomQuery")));
        filterForm.add(queryDslField);
        panel.add(filterForm);

        AjaxSubmitButton filterSubmitButton = new AjaxSubmitButton(formId + ID_SUFFIX_ACE_SUBMIT_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                if (filterForm.isVisible()) {
                    if (filterModel.getObject().getDslQuery() != null
                            && !filterModel.getObject().getDslQuery().isEmpty()) {
                        String midPointQuery = filterModel.getObject().getDslQuery();

                        try {
                            if (objectType.equals(RoleType.class)) {
                                roleQuery = getPrismContext().createQueryParser().parseFilter(objectType, midPointQuery);
                            } else if (objectType.equals(UserType.class)) {
                                userQuery = getPrismContext().createQueryParser().parseFilter(objectType, midPointQuery);
                            } else {
                                orgQuery = getPrismContext().createQueryParser().parseFilter(objectType, midPointQuery);
                            }
                            filterForm.setVisible(false);
                            this.add(AttributeAppender.replace("class", "ml-4 btn btn-success btn-sm"));
                        } catch (CommonException | RuntimeException e) {
                            LoggingUtils.logUnexpectedException(LOGGER, getString(
                                    "roleMiningExportPanel.message.couldNotExecuteQuery"), e);
                            OperationResult result = new OperationResult(getString(("roleMiningExportPanel.operation.query.parse")));

                            result.setMessage(getString("roleMiningExportPanel.result.failed.filter"));
                            result.recordFatalError(getString("roleMiningExportPanel.message.couldNotExecuteQuery"), e);

                            showResultFeedback(result, operationResultPanel, target);
                            this.add(AttributeAppender.replace("class", "ml-4 btn btn-danger btn-sm"));
                            target.add(this);
                        }
                    } else {
                        filterForm.setVisible(false);
                        this.add(AttributeAppender.replace("class", "ml-4 btn btn-default btn-sm"));
                    }
                } else {
                    operationResultPanel.setVisible(false);
                    target.add(operationResultPanel);
                    filterForm.setVisible(true);
                    this.add(AttributeAppender.replace("class", "ml-4 btn btn-primary btn-sm"));
                }
                target.add(filterForm);
                target.add(this);
            }

            @Override
            public IModel<?> getBody() {
                if (filterForm.isVisible()) {
                    return createStringResource("roleMiningExportPanel.save");
                } else {
                    return createStringResource("roleMiningExportPanel.filter.options");
                }
            }
        };

        filterSubmitButton.setOutputMarkupId(true);
        filterSubmitButton.add(AttributeAppender.append("style", "cursor: pointer;"));
        panel.add(filterSubmitButton);
    }

    @NotNull
    private AutoCompleteTextField<String> applicationRoleAutocompleteField(@NotNull WebMarkupContainer panel,
            HashMap<String, String> archetypeMap, IModel<String> selectedArchetypeModelApplication,
            List<String> archetypeListString) {

        AutoCompleteTextField<String> autoCompleteApplicationRoleField = new AutoCompleteTextField<>(
                ID_DROPDOWN_ARCHETYPE_APPLICATION, selectedArchetypeModelApplication) {
            @Override
            protected @NotNull Iterator<String> getChoices(String input) {
                List<String> filteredList = archetypeListString.stream()
                        .filter(archetype -> archetype.toLowerCase().contains(input.toLowerCase()))
                        .collect(Collectors.toList());
                return filteredList.iterator();
            }
        };

        autoCompleteApplicationRoleField.setRequired(true);
        autoCompleteApplicationRoleField.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                selectedArchetypeModelApplication.setObject(autoCompleteApplicationRoleField.getModelObject());
                applicationRoleArchetypeOid = archetypeMap.get(autoCompleteApplicationRoleField.getModelObject());
            }
        });

        autoCompleteApplicationRoleField.setOutputMarkupId(true);
        autoCompleteApplicationRoleField.add(new EnableBehaviour(this::isEditOptions));
        panel.add(autoCompleteApplicationRoleField);
        return autoCompleteApplicationRoleField;
    }

    @NotNull
    private AutoCompleteTextField<String> businessRoleAutocompleteField(@NotNull WebMarkupContainer panel,
            HashMap<String, String> archetypeMap, IModel<String> selectedArchetypeModelBusiness,
            List<String> archetypeListString) {

        AutoCompleteTextField<String> autoCompleteBusinessRoleField = new AutoCompleteTextField<>(
                ID_DROPDOWN_ARCHETYPE_BUSINESS, selectedArchetypeModelBusiness) {
            @Override
            protected @NotNull Iterator<String> getChoices(String input) {
                List<String> filteredList = archetypeListString.stream()
                        .filter(archetype -> archetype.toLowerCase().contains(input.toLowerCase()))
                        .collect(Collectors.toList());
                return filteredList.iterator();
            }
        };

        autoCompleteBusinessRoleField.setRequired(true);
        autoCompleteBusinessRoleField.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                selectedArchetypeModelBusiness.setObject(autoCompleteBusinessRoleField.getModelObject());
                businessRoleArchetypeOid = archetypeMap.get(autoCompleteBusinessRoleField.getModelObject());
            }
        });

        autoCompleteBusinessRoleField.setOutputMarkupId(true);
        autoCompleteBusinessRoleField.add(new EnableBehaviour(this::isEditOptions));
        panel.add(autoCompleteBusinessRoleField);
        return autoCompleteBusinessRoleField;
    }

    @NotNull
    private AjaxButton showAdditionalOptionsButton(Form<?> form) {
        AjaxButton showAdditionalOptions = new AjaxButton(ID_SHOW_ADDITIONAL_OPTIONS) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                form.setVisible(!form.isVisible());
                target.add(form);
                target.add(this);
            }

            @Override
            public IModel<?> getBody() {
                return getNameOfAdditionalOptionsButton(form.isVisible());
            }
        };

        showAdditionalOptions.setOutputMarkupId(true);
        showAdditionalOptions.add(AttributeAppender.append("style", "cursor: pointer;"));
        return showAdditionalOptions;
    }

    private @NotNull TextField<String> optionsTextField(String textFieldId, IModel<String> iModel) {
        TextField<String> textField = new TextField<>(textFieldId, iModel);
        textField.setOutputMarkupId(true);
        textField.setOutputMarkupPlaceholderTag(true);
        textField.add(new EnableBehaviour(this::isEditOptions));
        return textField;
    }

    @NotNull
    private List<String> getChoiceArchetypeList(@NotNull HashMap<String, String> archetypeMap,
            IModel<String> selectedArchetypeModelApplication, IModel<String> selectedArchetypeModelBusiness) {

        List<String> archetypeListString = new ArrayList<>();
        for (HashMap.Entry<String, String> entry : archetypeMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("Application role")
                    || entry.getValue().equals(APPLICATION_ROLE_ARCHETYPE_OID)) {
                selectedArchetypeModelApplication.setObject(entry.getKey());
                applicationRoleArchetypeOid = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase("Business role")
                    || entry.getValue().equals(BUSINESS_ROLE_ARCHETYPE_OID)) {
                selectedArchetypeModelBusiness.setObject(entry.getKey());
                businessRoleArchetypeOid = entry.getValue();
            }
            archetypeListString.add(entry.getKey());
        }
        return archetypeListString;
    }

    private @NotNull LabelWithHelpPanel getInfoLabelComponent(String id, IModel<String> labelModel, IModel<String> helpModel) {
        LabelWithHelpPanel label = new LabelWithHelpPanel(id, labelModel) {
            @Override
            protected IModel<String> getHelpModel() {
                return helpModel;
            }
        };
        label.setOutputMarkupId(true);
        return label;
    }

    public void exportPerformed(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
        RoleMiningExportOperation roleMiningExportOperation = new RoleMiningExportOperation();
        roleMiningExportOperation.setKey(encryptKey);
        roleMiningExportOperation.setNameModeExport(nameModeSelected);
        roleMiningExportOperation.setOrgExport(orgExport);
        roleMiningExportOperation.setSecurityLevel(securityModeSelected);
        roleMiningExportOperation.setQueryParameters(roleQuery, orgQuery, userQuery);

        if (applicationRoleArchetypeOid == null || applicationRoleArchetypeOid.isEmpty()) {
            applicationRoleArchetypeOid = APPLICATION_ROLE_ARCHETYPE_OID;
        }

        if (businessRoleArchetypeOid == null || businessRoleArchetypeOid.isEmpty()) {
            businessRoleArchetypeOid = BUSINESS_ROLE_ARCHETYPE_OID;
        }

        roleMiningExportOperation.setApplicationRoleIdentifiers(applicationRoleArchetypeOid,
                getApplicationPrefix(), getApplicationSuffix());
        roleMiningExportOperation.setBusinessRoleIdentifier(businessRoleArchetypeOid,
                getBusinessPrefix(), getBusinessSuffix());

        downloadBehaviour.setRoleMiningExport(roleMiningExportOperation);
        downloadBehaviour.setRoleMiningActive(true);
        downloadBehaviour.setQuery(null);
        downloadBehaviour.setUseZip(isZip());
        downloadBehaviour.initiate(target);
    }

    public HashMap<String, String> getArchetypeObjectsList() {
        return new HashMap<>();
    }

    private StringResourceModel getNameOfAdditionalOptionsButton(boolean visible) {
        return createStringResource("roleMiningExportPanel.showAdditionalOptions.button." + !visible);
    }

    private void generateRandomKey(RoleMiningExportUtils.SecurityMode securityMode) {
        setEncryptKey(RoleMiningExportUtils.updateEncryptKey(securityMode));
    }

    protected List<String> getApplicationPrefix() {
        if (applicationPrefix == null || applicationPrefix.isEmpty()) {
            return new ArrayList<>();
        }
        String[] separatePrefixes = applicationPrefix.split(DELIMITER);
        return new ArrayList<>(Arrays.asList(separatePrefixes));
    }

    private void setApplicationPrefix(String applicationPrefix) {
        this.applicationPrefix = applicationPrefix;
    }

    protected List<String> getApplicationSuffix() {
        if (applicationSuffix == null || applicationSuffix.isEmpty()) {
            return new ArrayList<>();
        }
        String[] separateSuffixes = applicationSuffix.split(DELIMITER);
        return new ArrayList<>(Arrays.asList(separateSuffixes));
    }

    private void setApplicationSuffix(String applicationSuffix) {
        this.applicationSuffix = applicationSuffix;
    }

    protected List<String> getBusinessPrefix() {
        if (businessPrefix == null || businessPrefix.isEmpty()) {
            return new ArrayList<>();
        }
        String[] separatePrefixes = businessPrefix.split(DELIMITER);
        return new ArrayList<>(Arrays.asList(separatePrefixes));
    }

    private void setBusinessPrefix(String businessPrefix) {
        this.businessPrefix = businessPrefix;
    }

    protected List<String> getBusinessSuffix() {
        if (businessSuffix == null || businessSuffix.isEmpty()) {
            return new ArrayList<>();
        }
        String[] separateSuffixes = businessSuffix.split(DELIMITER);
        return new ArrayList<>(Arrays.asList(separateSuffixes));
    }

    private void setBusinessSuffix(String businessSuffix) {
        this.businessSuffix = businessSuffix;
    }

    private boolean isEditOptions() {
        return editOptions;
    }

    private void setEditOptions(boolean editOptions) {
        this.editOptions = editOptions;
    }

    private void setEncryptKey(String encryptKey) {
        this.encryptKey = encryptKey;
    }

    private String getEncryptKey() {
        return encryptKey;
    }

    private boolean isZip() {
        return isZip;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("roleMiningExportPanel.title");
    }

    protected IModel<String> createYesLabel() {
        return createStringResource("roleMiningExportPanel.export");
    }

    protected IModel<String> createNoLabel() {
        return createStringResource("roleMiningExportPanel.cancel");
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

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public @NotNull Component getFooter() {
        return get(getPageBase().createComponentPath(Popupable.ID_FOOTER));
    }

}
