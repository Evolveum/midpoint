/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.authentication.CompiledUserProfile;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class FocusTabVisibleBehavior<O extends ObjectType> extends VisibleEnableBehaviour {
    private static final long serialVersionUID = 1L;

    private static final String OPERATION_LOAD_GUI_CONFIGURATION = FocusTabVisibleBehavior.class.getName() + ".loadGuiConfiguration";

    private IModel<PrismObject<O>> objectModel;
    private String uiAuthorizationUrl;
    private PageBase pageBase;

    public FocusTabVisibleBehavior(IModel<PrismObject<O>> objectModel, String uiAuthorizationUrl, PageBase pageBase) {
        this.objectModel = objectModel;
        this.uiAuthorizationUrl = uiAuthorizationUrl;
        this.pageBase = pageBase;
    }

    private ModelInteractionService getModelInteractionService() {
        return ((MidPointApplication) MidPointApplication.get()).getModelInteractionService();
    }

    private TaskManager getTaskManager() {
        return ((MidPointApplication) MidPointApplication.get()).getTaskManager();
    }

    @Override
    public boolean isVisible() {
        PrismObject<O> object = objectModel.getObject();
        if (object == null) {
            return true;
        }

        Task task = WebModelServiceUtils.createSimpleTask(OPERATION_LOAD_GUI_CONFIGURATION,
                SecurityUtils.getPrincipalUser().getUser().asPrismObject(), getTaskManager());
        OperationResult result = task.getResult();

        CompiledUserProfile config;
        try {
            config = getModelInteractionService().getCompiledUserProfile(task, result);
        } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
            throw new SystemException("Cannot load GUI configuration: " + e.getMessage(), e);
        }

        // find all object form definitions for specified type, if there is none we'll show all default tabs
        List<ObjectFormType> forms = findObjectForm(config, object);
        if (forms.isEmpty()) {
            return true;
        }

        // we'll try to find includeDefault, if there is includeDefault=true, we can return true (all tabs visible)
        for (ObjectFormType form : forms) {
            if (BooleanUtils.isTrue(form.isIncludeDefaultForms())) {
                return true;
            }
        }

        for (ObjectFormType form : forms) {
            FormSpecificationType spec = form.getFormSpecification();
            if (spec == null || StringUtils.isEmpty(spec.getPanelUri())) {
                continue;
            }

            if (ObjectUtils.equals(uiAuthorizationUrl, spec.getPanelUri())) {
                return true;
            }
        }

        return false;
    }

    private List<ObjectFormType> findObjectForm(CompiledUserProfile config, PrismObject<O> object) {
        List<ObjectFormType> result = new ArrayList<>();

        if (config == null || config.getObjectForms() == null) {
            return result;
        }

        ObjectFormsType forms = config.getObjectForms();
        List<ObjectFormType> list = forms.getObjectForm();
        if (list.isEmpty()) {
            return result;
        }

        for (ObjectFormType form : list) {
            if (isApplicable(form, object)) {
                result.add(form);
            }
        }

        return result;
    }

    private boolean isApplicable(ObjectFormType form, PrismObject<O> object) {
        QName objectType = object.getDefinition().getTypeName();
        if (!QNameUtil.match(objectType, form.getType())) {
            return false;
        }
        RoleRelationObjectSpecificationType roleRelation = form.getRoleRelation();
        if (roleRelation != null) {
            List<QName> subjectRelations = roleRelation.getSubjectRelation();
            if (!pageBase.hasSubjectRoleRelation(object.getOid(), subjectRelations)) {
                return false;
            }
        }
        // TODO: roleRelation
        return true;
    }
}
