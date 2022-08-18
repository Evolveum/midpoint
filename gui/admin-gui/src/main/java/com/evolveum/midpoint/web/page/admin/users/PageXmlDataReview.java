/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.users;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Created by honchar.
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/xmlDataReview")
        },
        action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                label = "PageAdminUsers.auth.usersAll.label",
                description = "PageAdminUsers.auth.usersAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USER_HISTORY_XML_REVIEW_URL,
                label = "PageUser.auth.userHistoryXmlReview.label",
                description = "PageUser.auth.userHistoryXmlReview.description")})
public class PageXmlDataReview extends PageAdmin {

    private static final String ID_ACE_EDITOR_CONTAINER = "aceEditorContainer";
    private static final String ID_ACE_EDITOR_PANEL = "aceEditorPanel";
    private static final String ID_BUTTON_BACK = "back";

    private static final String DOT_CLASS = PageXmlDataReview.class.getName() + ".";
    private static final String OPERATION_RESTRUCT_OBJECT = DOT_CLASS + "restructObject";

    private static final Trace LOGGER = TraceManager.getTrace(PageXmlDataReview.class);

    private static final String OID_PARAMETER_LABEL = "oid";
    private static final String EID_PARAMETER_LABEL = "eventIdentifier";
    private static final String DATE_PARAMETER_LABEL = "date";
    private static final String CLASS_TYPE_PARAMETER_LABEL = "classType";
    private final PrismObject<? extends FocusType> reconstructedObject;

    public PageXmlDataReview(PageParameters pageParameters) {
        super(pageParameters);
        this.reconstructedObject = getReconstructedObject();
        initLayout(getIModelTitle(), getIModelData());
    }

    private void initLayout(IModel<String> title, IModel<String> data){
        WebMarkupContainer container = new WebMarkupContainer(ID_ACE_EDITOR_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        AceEditorPanel aceEditorPanel = new AceEditorPanel(ID_ACE_EDITOR_PANEL, title, data);
        aceEditorPanel.getEditor().setReadonly(true);
        aceEditorPanel.setOutputMarkupId(true);
        container.add(aceEditorPanel);

        AjaxButton back = new AjaxButton(ID_BUTTON_BACK, createStringResource("PageBase.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                redirectBack();
            }

        };
        add(back);
    }

    private IModel<String> getIModelTitle() {
        PolyString name = null;
        if (reconstructedObject != null) {
            name = reconstructedObject.getName();
        }
        return createStringResource("PageXmlDataReview.aceEditorPanelTitle", name, getDatePageParameter());
    }

    private IModel<String> getIModelData() {
        return new IModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                PrismContext context = getPrismContext();
                String xml = "";
                try {
                    if (reconstructedObject != null) {
                        xml = context.xmlSerializer().serialize(reconstructedObject);
                    }
                } catch (Exception ex) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't serialize object", ex);
                }
                return xml;
            }

            @Override
            public void setObject(String s) {

            }

            @Override
            public void detach() {

            }
        };
    }

    private PrismObject<? extends FocusType> getReconstructedObject() {
        OperationResult result = new OperationResult(OPERATION_RESTRUCT_OBJECT);
        try {
            Task task = createSimpleTask(OPERATION_RESTRUCT_OBJECT);
            Class<? extends FocusType> classType = Class.forName(getClassTypePageParameter()).asSubclass(FocusType.class);
            return getModelAuditService().reconstructObject(classType, getObjectIdPageParameter(), getEventIdentifierPageParameter(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(createStringResource("ObjectHistoryTabPanel.message.getReconstructedObject.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't restruct object", ex);
        }
        return null;
    }

    private String getDatePageParameter() {
        return String.valueOf(getPageParameters().get(DATE_PARAMETER_LABEL));
    }

    private String getObjectIdPageParameter() {
        return String.valueOf(getPageParameters().get(OID_PARAMETER_LABEL));
    }

    private String getEventIdentifierPageParameter() {
        return String.valueOf(getPageParameters().get(EID_PARAMETER_LABEL));
    }

    private String getClassTypePageParameter() {
        return String.valueOf(getPageParameters().get(CLASS_TYPE_PARAMETER_LABEL));
    }

}
