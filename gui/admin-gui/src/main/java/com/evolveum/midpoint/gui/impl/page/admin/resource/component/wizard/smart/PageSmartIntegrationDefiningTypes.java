/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.smart;

import java.io.Serial;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/smartIntegrationDefiningTypes",
                        matchUrlForSecurity = "/admin/config/smartIntegrationDefiningTypes")
        },
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION) })
public class PageSmartIntegrationDefiningTypes extends PageAdminConfiguration {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageSmartIntegrationDefiningTypes.class);

    private static final String PARAM_RESOURCE_OID = "resourceOid";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_OBJECT_CLASSES = "objectClasses";
    private static final String ID_OBJECT_TYPES_SUGGESTIONS = "objectTypesSuggestions";
    private static final String ID_OBJECT_CLASS_TO_DEFINE_TYPES_FOR = "objectClassToDefineTypesFor";
    private static final String ID_DEFINE_TYPES = "defineTypes";
    private static final String ID_DEFINE_ASSOCIATIONS = "defineAssociations";
    private static final String ID_EXPLORE_OBJECT_TYPES_SUGGESTION = "exploreObjectTypesSuggestion";
    private static final String ID_OBJECT_TYPES_SUGGESTION_TO_EXPLORE = "objectTypesSuggestionToExplore";

    private static final String CLASS_DOT = PageSmartIntegrationDefiningTypes.class.getName() + ".";
    private static final String OP_DEFINE_TYPES = CLASS_DOT + "defineTypes";
    private static final String OP_DETERMINE_STATUS = CLASS_DOT + "determineStatus";

    private final IModel<ResourceStatus> statusModel = LoadableModel.create(() -> loadStatus(), true);
    private final IModel<String> objectClassToDefineTypesForModel =
            LoadableModel.create(() -> statusModel.getObject().getSuggestedObjectClassName(), true);
    private final IModel<String> objectTypesSuggestionToExploreModel =
            LoadableModel.create(() -> statusModel.getObject().getObjectTypesSuggestionToExplore(), true);

    private @NotNull ResourceStatus loadStatus() {
        var task = createSimpleTask(OP_DETERMINE_STATUS);
        var result = task.getResult();
        var resourceOid = getResourceOid();
        var smart = getSmartIntegrationService();
        try {
            var resource = getModelService().getObject(ResourceType.class, resourceOid, null, task, result);
            RealResourceStatus status = new RealResourceStatus(resource);
            status.initialize(smart, task, result);
            return status;
        } catch (Throwable t) {
            result.recordException(t);
            LoggingUtils.logException(LOGGER, "Error loading status for resource {}", t, resourceOid);
            return new ResourceStatus.ErrorStatus("Error loading status: " + t.getMessage());
        } finally {
            result.close();
        }
    }

    private @NotNull String getResourceOid() {
        return MiscUtil.stateNonNull(
                getPageParameters().get(PARAM_RESOURCE_OID).toString(), "no resource OID provided");
    }

    public static void navigateTo(PageBase sourcePage, String resourceOid) {
        sourcePage.navigateToNext(
                PageSmartIntegrationDefiningTypes.class,
                new PageParameters()
                        .add(PARAM_RESOURCE_OID, resourceOid));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        MidpointForm<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        AceEditor objectClassesEditor =
                new AceEditor(ID_OBJECT_CLASSES, new PropertyModel<>(statusModel, RealResourceStatus.F_OBJECT_CLASSES_TEXT));
        objectClassesEditor.setReadonly(true);
        objectClassesEditor.setHeight(400);
        objectClassesEditor.setResizeToMaxHeight(false);
        mainForm.add(objectClassesEditor);

        AceEditor objectTypesSuggestionsEditor = new AceEditor(
                ID_OBJECT_TYPES_SUGGESTIONS, new PropertyModel<>(statusModel, RealResourceStatus.F_OBJECT_TYPES_SUGGESTIONS_TEXT));
        objectTypesSuggestionsEditor.setReadonly(true);
        objectTypesSuggestionsEditor.setHeight(400);
        objectTypesSuggestionsEditor.setResizeToMaxHeight(false);
        mainForm.add(objectTypesSuggestionsEditor);

        mainForm.add(new AjaxSubmitButton(ID_DEFINE_TYPES) {
            @Override
            public void onSubmit(AjaxRequestTarget target) {
                onDefineTypesPerformed(target);
            }
        });

        mainForm.add(new TextField<>(ID_OBJECT_CLASS_TO_DEFINE_TYPES_FOR, objectClassToDefineTypesForModel));

        mainForm.add(new AjaxSubmitButton(ID_EXPLORE_OBJECT_TYPES_SUGGESTION) {
            @Override
            public void onSubmit(AjaxRequestTarget target) {
                var status = statusModel.getObject();
                var suggestionToken = objectTypesSuggestionToExploreModel.getObject();
                if (status instanceof RealResourceStatus realStatus) {
                    PageSmartIntegrationTypesSuggestion.navigateTo(
                            PageSmartIntegrationDefiningTypes.this,
                            realStatus.getResource(),
                            suggestionToken != null ?
                                    realStatus.getObjectTypesSuggestion(suggestionToken) : null);
                }
            }
        });

        mainForm.add(new TextField<>(ID_OBJECT_TYPES_SUGGESTION_TO_EXPLORE, objectTypesSuggestionToExploreModel));

        mainForm.add(new AjaxSubmitButton(ID_DEFINE_ASSOCIATIONS) {
            @Override
            public void onSubmit(AjaxRequestTarget target) {
                // TODO
            }
        });

    }

    private void onDefineTypesPerformed(AjaxRequestTarget target) {
        taskAwareExecutor(target, OP_DEFINE_TYPES)
                .runVoid((task, result) -> {
                    var status = statusModel.getObject();
                    var objectClassLocalName = objectClassToDefineTypesForModel.getObject();
                    if (StringUtils.isBlank(objectClassLocalName)) {
                        throw new IllegalArgumentException("Object class name cannot be blank");
                    }
                    var objectClassName = new QName(NS_RI, objectClassLocalName);
                    status.checkObjectClassName(objectClassName);
                    var oid = getSmartIntegrationService().submitSuggestObjectTypesOperation(
                            getResourceOid(), objectClassName, task, result);
                    result.setBackgroundTaskOid(oid);
                });
    }
}
