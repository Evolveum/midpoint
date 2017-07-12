/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
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
public class FocusTabVisibleBehavior<T extends ObjectType> extends VisibleEnableBehaviour {

    private static final String OPERATION_LOAD_GUI_CONFIGURATION = FocusTabVisibleBehavior.class.getName() + ".loadGuiConfiguration";

    private IModel<PrismObject<T>> objectModel;
    private String uiAuthorizationUrl;

    public FocusTabVisibleBehavior(IModel<PrismObject<T>> objectModel, String uiAuthorizationUrl) {
        this.objectModel = objectModel;
        this.uiAuthorizationUrl = uiAuthorizationUrl;
    }

    private ModelInteractionService getModelInteractionService() {
        return ((MidPointApplication) MidPointApplication.get()).getModelInteractionService();
    }

    private TaskManager getTaskManager() {
        return ((MidPointApplication) MidPointApplication.get()).getTaskManager();
    }

    @Override
    public boolean isVisible() {
        PrismObject obj = objectModel.getObject();
        if (obj == null) {
            return true;
        }

        QName type = obj.getDefinition().getTypeName();

        Task task = WebModelServiceUtils.createSimpleTask(OPERATION_LOAD_GUI_CONFIGURATION,
                SecurityUtils.getPrincipalUser().getUser().asPrismObject(), getTaskManager());
        OperationResult result = task.getResult();

        AdminGuiConfigurationType config;
        try {
            config = getModelInteractionService().getAdminGuiConfiguration(task, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            throw new SystemException("Cannot load GUI configuration: " + e.getMessage(), e);
        }

        // find all object form definitions for specified type, if there is none we'll show all default tabs
        List<ObjectFormType> forms = findObjectForm(config, type);
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

    private List<ObjectFormType> findObjectForm(AdminGuiConfigurationType config, QName type) {
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
            if (type.equals(form.getType())) {
                result.add(form);
            }
        }

        return result;
    }
}
