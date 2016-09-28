package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.assignment.AssignmentCatalogPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar.
 */
@PageDescriptor(url = {"/self/assignmentShoppingCart"}, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_ASSIGNMENT_SHOP_KART_URL,
                label = "PageAssignmentShoppingKart.auth.requestAssignment.label",
                description = "PageAssignmentShoppingKart.auth.requestAssignment.description")})
public class PageAssignmentShoppingKart extends PageSelf{
    private static final String ID_MAIN_PANEL = "mainPanel";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String DOT_CLASS = PageAssignmentShoppingKart.class.getName() + ".";
    private static final String OPERATION_LOAD_QUESTION_POLICY = DOT_CLASS + "loadRoleCatalogReference";
    private static final Trace LOGGER = TraceManager.getTrace(PageAssignmentShoppingKart.class);

    public PageAssignmentShoppingKart(){
        initLayout();
    }
    private void initLayout(){
        Form mainForm = new org.apache.wicket.markup.html.form.Form(ID_MAIN_FORM);
        add(mainForm);
        AssignmentCatalogPanel panel = new AssignmentCatalogPanel(ID_MAIN_PANEL, new IModel<String>() {
            @Override
            public String getObject() {
                Task task = getPageBase().createAnonymousTask(OPERATION_LOAD_QUESTION_POLICY);
                OperationResult result = task.getResult();

                PrismObject<SystemConfigurationType> config;
                try {
                    config = getPageBase().getModelService().getObject(SystemConfigurationType.class,
                            SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, task, result);
                } catch (ObjectNotFoundException | SchemaException | SecurityViolationException
                        | CommunicationException | ConfigurationException e) {
                    LOGGER.error("Error getting system configuration: {}", e.getMessage(), e);
                    return null;
                }
                if (config != null && config.asObjectable().getRoleManagement() != null &&
                        config.asObjectable().getRoleManagement().getRoleCatalogRef() != null){
                    return config.asObjectable().getRoleManagement().getRoleCatalogRef().getOid();
                }
                return "";
            }

            @Override
            public void setObject(String s) {

            }

            @Override
            public void detach() {

            }
        });
        mainForm.add(panel);

    }

    private PageBase getPageBase(){
        return (PageBase) getPage();
    }
}
