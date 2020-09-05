/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ObjectTabVisibleBehavior<O extends ObjectType> extends VisibleEnableBehaviour {
    private static final long serialVersionUID = 1L;

    private final IModel<PrismObject<O>> objectModel;
    private final String uiAuthorizationUrl;
    private final PageBase pageBase;

    public ObjectTabVisibleBehavior(IModel<PrismObject<O>> objectModel, String uiAuthorizationUrl, PageBase pageBase) {
        this.objectModel = objectModel;
        this.uiAuthorizationUrl = uiAuthorizationUrl;
        this.pageBase = pageBase;
    }

    @Override
    public boolean isVisible() {
        PrismObject<O> object = objectModel.getObject();
        if (object == null) {
            return true;
        }

        CompiledGuiProfile config = pageBase.getCompiledGuiProfile();

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

            if (Objects.equals(uiAuthorizationUrl, spec.getPanelUri())) {
                return true;
            }
        }

        return false;
    }

    private List<ObjectFormType> findObjectForm(CompiledGuiProfile config, PrismObject<O> object) {
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
            return pageBase.hasSubjectRoleRelation(object.getOid(), subjectRelations);
        }
        // TODO: roleRelation
        return true;
    }
}
