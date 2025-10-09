/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.self;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.assignment.SelfConsentPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;


@PageDescriptor(
        urls = {
                @Url(mountUrl = "/self/consents")
                },
        action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_CONSENTS_URL,
                label = "PageSelfCredentials.auth.consents.label",
                description = "PageSelfCredentials.auth.consents.description")
        }, experimental = true)
@Experimental
public class PageSelfConsents extends PageBase{

    private static final long serialVersionUID = 1L;

    private LoadableModel<List<AssignmentType>> consentModel;
    private static final String DOT_CLASS = PageSelfConsents.class.getSimpleName() + ".";
    private static final String OPERATION_LOAD_USER =  DOT_CLASS + "loadUserSelf";

    private static final String ID_CONSENTS = "consents";

    public PageSelfConsents() {

        consentModel = new LoadableModel<List<AssignmentType>>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<AssignmentType> load() {
                MidPointPrincipal principal = AuthUtil.getPrincipalUser();

                if (principal == null) {
                    return null;
                }

                Task task = createSimpleTask(OPERATION_LOAD_USER);
                OperationResult result = task.getResult();
                PrismObject<UserType> userSelf = WebModelServiceUtils.loadObject(UserType.class, principal.getOid(), PageSelfConsents.this, task, result);
                PrismContainer<AssignmentType> assignmentContainer = userSelf.findContainer(UserType.F_ASSIGNMENT);
                if (assignmentContainer == null || assignmentContainer.isEmpty()) {
                    return new ArrayList<>();
                }

                Collection<AssignmentType> assignments = assignmentContainer.getRealValues();
                return assignments.stream()
                        .filter(a -> a.getTargetRef()!= null && QNameUtil.match(a.getTargetRef().getRelation(), SchemaConstants.ORG_CONSENT))
                        .collect(Collectors.toList());
            }
        };

        initLayout();
    }

    private void initLayout() {

        RepeatingView consents = new RepeatingView(ID_CONSENTS);
        consents.setOutputMarkupId(true);
        for (AssignmentType assignmentType : consentModel.getObject()) {
            SelfConsentPanel consentPanel = new SelfConsentPanel(consents.newChildId(), Model.of(assignmentType), this);
            consentPanel.setOutputMarkupId(true);
            consents.add(consentPanel);
        }

        add(consents);

    }

}
