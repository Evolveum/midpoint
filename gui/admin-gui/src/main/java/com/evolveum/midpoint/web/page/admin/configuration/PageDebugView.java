/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.component.OperationPanelPart;
import com.evolveum.midpoint.gui.impl.page.admin.component.OperationsPanel;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.DataLanguagePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.gui.api.component.form.TextArea;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/debug", matchUrlForSecurity = "/admin/config/debug")
        },
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_DEBUG_URL,
                        label = "PageDebugView.auth.debug.label", description = "PageDebugView.auth.debug.description") })
public class PageDebugView extends PageAdminConfiguration {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageDebugView.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECT = DOT_CLASS + "initObjectViewObject";
    private static final String OPERATION_SAVE_OBJECT = DOT_CLASS + "saveObject";
    private static final String ID_PLAIN_TEXTAREA = "plainTextarea";
    private static final String ID_VIEW_BUTTON_PANEL = "viewButtonPanel";

    private static final String ID_FORM = "mainForm";

    private static final String ID_OPERATIONS_PANEL = "operationsPanel";
    private static final String ID_MAIN = "main";
    private static final String ID_OPTIONS = "options";

    private static final Trace LOGGER = TraceManager.getTrace(PageDebugView.class);

    public static final String PARAM_OBJECT_ID = "objectId";
    public static final String PARAM_OBJECT_TYPE = "objectType";
    static final String PARAM_SHOW_ALL_ITEMS = "showAllItems";
    private String dataLanguage = null;

    private IModel<ObjectViewDto<?>> objectViewDtoModel;
    private final DebugViewOptions debugViewConfiguration = new DebugViewOptions();
    private static final int DEFAULT_EDITOR_ROWS_NUMBER = 50;

    public PageDebugView() {
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        if (dataLanguage == null) {
            dataLanguage = determineDataLanguage();
        }
        if (objectViewDtoModel == null) {
            objectViewDtoModel = initObjectViewObject();
        }
        initLayout();
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        if (objectViewDtoModel == null) {
            objectViewDtoModel = initObjectViewObject();
        }
        if (dataLanguage == null) {
            dataLanguage = determineDataLanguage();
        }

        return createStringResource("PageDebugView.title", getName());
    }

    private String getName() {
        if (objectViewDtoModel == null || objectViewDtoModel.getObject() == null) {
            return "";
        }

        return objectViewDtoModel.getObject().getName();
    }

    private LoadableModel<ObjectViewDto<?>> initObjectViewObject() {
        return new LoadableModel<ObjectViewDto<?>>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected ObjectViewDto<?> load() {
                ObjectViewDto<?> objectViewDto = new ObjectViewDto<>();
                StringValue objectOid = getPageParameters().get(PARAM_OBJECT_ID);
                if (objectOid == null || StringUtils.isEmpty(objectOid.toString())) {
                    getSession().error(getString("pageDebugView.message.oidNotDefined"));
                    throw new RestartResponseException(PageDebugList.class);
                }

                Task task = createSimpleTask(OPERATION_LOAD_OBJECT);
                OperationResult result = task.getResult(); //todo is this result != null ?
                try {
                    MidPointApplication application = PageDebugView.this.getMidpointApplication();

                    Class<? extends ObjectType> type = getTypeFromParameters();

                    GetOperationOptionsBuilder optionsBuilder = getSchemaService().getOperationOptionsBuilder()
                            .raw()
                            .resolveNames()
                            .tolerateRawData();
                    if (getPageParameters().get(PARAM_SHOW_ALL_ITEMS).toBoolean(true)) {
                        optionsBuilder = optionsBuilder.retrieve();
                    }
                    PrismObject<? extends ObjectType> object = getModelService().getObject(type, objectOid.toString(), optionsBuilder.build(), task, result);

                    PrismContext context = application.getPrismContext();

                    String lex = context.serializerFor(dataLanguage).serialize(object);
                    objectViewDto = new ObjectViewDto<>(object.getOid(), WebComponentUtil.getName(object), object, lex);

                    result.recomputeStatus();
                } catch (Exception ex) {
                    result.recordFatalError(getString("WebModelUtils.couldntLoadObject"), ex);
                }

                showResult(result, false);

                if (!WebComponentUtil.isSuccessOrHandledErrorOrWarning(result)) {
                    throw new RestartResponseException(PageDebugList.class);
                }
                return objectViewDto;
            }
        };
    }

    private Class<? extends ObjectType> getTypeFromParameters() {
        StringValue objectType = getPageParameters().get(PARAM_OBJECT_TYPE);
        if (objectType != null && StringUtils.isNotBlank(objectType.toString())) {
            return getPrismContext().getSchemaRegistry().determineCompileTimeClass(new QName(SchemaConstantsGenerated.NS_COMMON, objectType.toString()));
        }

        return ObjectType.class;
    }

    private void initLayout() {
        MidpointForm<?> mainForm = new MidpointForm<>(ID_FORM);
        add(mainForm);

        OperationsPanel operationsPanel = new OperationsPanel(ID_OPERATIONS_PANEL);
        mainForm.add(operationsPanel);

        OperationPanelPart main = new OperationPanelPart(ID_MAIN, createStringResource("OperationalButtonsPanel.buttons.main"));
        operationsPanel.add(main);

        OperationPanelPart options = new OperationPanelPart(ID_OPTIONS, createStringResource("pageDebugView.options"));
        operationsPanel.add(options);

        options.add(createOptionCheckbox(DebugViewOptions.ID_ENCRYPT, new PropertyModel<>(debugViewConfiguration, DebugViewOptions.ID_ENCRYPT), "pageDebugView.encrypt", "pageDebugView.encrypt.help"));
        options.add(createOptionCheckbox(DebugViewOptions.ID_VALIDATE_SCHEMA, new PropertyModel<>(debugViewConfiguration, DebugViewOptions.ID_VALIDATE_SCHEMA), "pageDebugView.validateSchema", "pageDebugView.validateSchema.help"));
        options.add(createOptionCheckbox(DebugViewOptions.ID_SAVE_AS_RAW, new PropertyModel<>(debugViewConfiguration, DebugViewOptions.ID_SAVE_AS_RAW), "pageDebugView.saveAsRaw", "pageDebugView.saveAsRaw.help"));
        options.add(createOptionCheckbox(DebugViewOptions.ID_REEVALUATE_SEARCH_FILTERS, new PropertyModel<>(debugViewConfiguration, DebugViewOptions.ID_REEVALUATE_SEARCH_FILTERS), "pageDebugView.reevaluateSearchFilters", "pageDebugView.reevaluateSearchFilters.help"));
        options.add(createOptionCheckbox(DebugViewOptions.ID_SWITCH_TO_PLAINTEXT, new PropertyModel<>(debugViewConfiguration, DebugViewOptions.ID_SWITCH_TO_PLAINTEXT), "pageDebugView.switchToPlainText", "pageDebugView.switchToPlainText.help"));

        TextArea<String> plainTextarea = new TextArea<>(ID_PLAIN_TEXTAREA, new PropertyModel<>(objectViewDtoModel, ObjectViewDto.F_XML));
        plainTextarea.add(new VisibleBehaviour(() -> debugViewConfiguration.switchToPlainText));
        plainTextarea.add(new AttributeModifier("rows", DEFAULT_EDITOR_ROWS_NUMBER));
        plainTextarea.add(new AttributeModifier("style", "min-width:100%; max-width: 100%;"));
        mainForm.add(plainTextarea);

        initAceEditor(mainForm);

        initButtons(main);
        initViewButton(mainForm);
    }

    private CheckBoxPanel createOptionCheckbox(String id, IModel<Boolean> model, String labelKey, String helpKey) {

        return new CheckBoxPanel(id, model, createStringResource(labelKey), createStringResource(helpKey)) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onUpdate(AjaxRequestTarget target) {
                if (DebugViewOptions.ID_SWITCH_TO_PLAINTEXT.equals(id)) {
                    target.add(getMainForm());
                }
            }
        };
    }

    private MidpointForm<?> getMainForm() {
        return (MidpointForm<?>) get(ID_FORM);
    }

    private void initAceEditor(MidpointForm<?> mainForm) {
        AceEditor editor = new AceEditor("aceEditor", new PropertyModel<>(objectViewDtoModel, ObjectViewDto.F_XML));
        editor.setModeForDataLanguage(dataLanguage);
        editor.add(new VisibleBehaviour(() -> !debugViewConfiguration.switchToPlainText));
        mainForm.add(editor);
    }

    private void initViewButton(MidpointForm mainForm) {
        DataLanguagePanel<Objectable> dataLanguagePanel =
                new DataLanguagePanel<Objectable>(ID_VIEW_BUTTON_PANEL, dataLanguage, Objectable.class, PageDebugView.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onLanguageSwitched(AjaxRequestTarget target, int updatedIndex, String updatedLanguage,
                            String objectString) {
                        objectViewDtoModel.getObject().setXml(objectString);
                        dataLanguage = updatedLanguage;
                        target.add(mainForm);
                    }

                    @Override
                    protected String getObjectStringRepresentation() {
                        return objectViewDtoModel.getObject().getXml();
                    }

                    @Override
                    protected boolean isValidateSchema() {
                        return debugViewConfiguration.validateSchema;
                    }
                };
        dataLanguagePanel.setOutputMarkupId(true);
        mainForm.add(dataLanguagePanel);
    }

    private void initButtons(OperationPanelPart set) {
        AjaxSubmitButton saveButton = new AjaxSubmitButton("saveButton",
                createStringResource("pageDebugView.button.save")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }
        };
        set.add(saveButton);

        AjaxButton backButton = new AjaxButton("backButton",
                createStringResource("pageDebugView.button.back")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                redirectBack();
            }
        };
        set.add(backButton);
    }

    public void savePerformed(AjaxRequestTarget target) {
        if (StringUtils.isEmpty(objectViewDtoModel.getObject().getXml())) {
            error(getString("pageDebugView.message.cantSaveEmpty"));
            target.add(getFeedbackPanel());
            return;
        }

        Task task = createSimpleTask(OPERATION_SAVE_OBJECT);
        OperationResult result = task.getResult();
        try {

            PrismObject<? extends ObjectType> oldObject = objectViewDtoModel.getObject().getObject();
            oldObject.revive(getPrismContext());

            Holder<? extends ObjectType> objectHolder = new Holder<>(null);
            validateObject(result, (Holder) objectHolder);

            if (result.isAcceptable()) {
                PrismObject<? extends ObjectType> newObject = objectHolder.getValue().asPrismObject();

                ObjectDelta<? extends ObjectType> delta = oldObject.diff((PrismObject) newObject, EquivalenceStrategy.LITERAL);

                hackShadowDelta(delta, oldObject, newObject);

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Delta to be applied:\n{}", delta.debugDump());
                }

                Collection<ObjectDelta<? extends ObjectType>> deltas = MiscUtil.createCollection(delta);
                ModelExecuteOptions options = ModelExecuteOptions.create();
                if (debugViewConfiguration.saveAsRaw) {
                    options.raw(true);
                }
                if (debugViewConfiguration.reevaluateSearchFilters) {
                    options.reevaluateSearchFilters(true);
                }
                if (!debugViewConfiguration.encrypt) {
                    options.noCrypt(true);
                }

                getModelService().executeChanges(deltas, options, task, result);

                result.computeStatus();
            }
        } catch (Exception ex) {
            result.recordFatalError(getString("WebModelUtils.couldntSaveObject"), ex);
        }

        if (result.isError()) {
            showResult(result);
            target.add(getFeedbackPanel());
        } else {
            showResult(result);
            //to handle returning back to list objects page instead of edit object page
            if (getBreadcrumbs().size() >= 3) {
                redirectBack(3);
            } else {
                redirectBack();
            }
        }
    }

    /**
     * A workaround for MID-9935. There are phantom changes of polystring shadow attributes.
     * Moreover, we are not able to update them in a nice way.
     */
    private void hackShadowDelta(
            ObjectDelta<? extends ObjectType> delta,
            PrismObject<? extends ObjectType> oldObject,
            PrismObject<? extends ObjectType> newObject) {
        if (delta == null
                || !ShadowType.class.equals(delta.getObjectTypeClass())
                || !delta.isModify()
                || oldObject == null // objectOld/New shouldn't be null for modify delta anyway
                || newObject == null) {
            return;
        }
        var iterator = delta.getModifications().iterator();
        while (iterator.hasNext()) {
            var modification = iterator.next();
            if (!ShadowType.F_ATTRIBUTES.equivalent(modification.getParentPath())) {
                continue;
            }
            var definition = modification.getDefinition();
            if (definition == null || !PolyStringType.COMPLEX_TYPE.equals(definition.getTypeName())) {
                continue;
            }
            var path = modification.getPath();
            var oldValues = getOrigValues(oldObject.findProperty(path));
            var newValues = getOrigValues(newObject.findProperty(path));
            if (MiscUtil.unorderedCollectionEquals(oldValues, newValues)) {
                LOGGER.trace("Removing phantom change of polystring attribute {}", path);
                iterator.remove();
            }
        }
    }

    private Collection<String> getOrigValues(PrismProperty<?> property) {
        if (property == null) {
            return List.of();
        } else {
            // Hack - to avoid failing on non-polystring values
            return property.getRealValues().stream()
                    .map(value -> value instanceof PolyString polyString ? polyString.getOrig() : String.valueOf(value))
                    .toList();
        }
    }

    private void validateObject(OperationResult result, Holder<Objectable> objectHolder) {
        parseObject(objectViewDtoModel.getObject().getXml(), objectHolder, dataLanguage, debugViewConfiguration.validateSchema, false, Objectable.class, result);
    }

    static class DebugViewOptions implements Serializable {

        private static final long serialVersionUID = 1L;

        private static final String ID_ENCRYPT = "encrypt";
        private static final String ID_SAVE_AS_RAW = "saveAsRaw";
        private static final String ID_REEVALUATE_SEARCH_FILTERS = "reevaluateSearchFilters";
        private static final String ID_VALIDATE_SCHEMA = "validateSchema";
        private static final String ID_SWITCH_TO_PLAINTEXT = "switchToPlainText";

        private boolean encrypt = true;
        private boolean saveAsRaw = true;
        private boolean reevaluateSearchFilters = false;
        private boolean validateSchema = false;
        private boolean switchToPlainText = false;
    }
}
