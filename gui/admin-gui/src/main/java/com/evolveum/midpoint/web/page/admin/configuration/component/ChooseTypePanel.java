/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.component;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageBase;

import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author shood
 *
 *  TODO use a better name (ChooseObjectPanel ? ObjectChoosePanel ?)
 *  Distinguish between chooser panels that reside on "main page" and
 *  the one that resides in the popup window (ObjectSelectionPanel).
 */
public class ChooseTypePanel<T extends ObjectType> extends BasePanel<ObjectViewDto<T>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ChooseTypePanel.class);

    private static final String ID_OBJECT_NAME = "name";
    private static final String ID_LINK_CHOOSE = "choose";
    private static final String ID_LINK_REMOVE = "remove";

    public ChooseTypePanel(String id, IModel<ObjectViewDto<T>> model) {
        super(id, model);
        initLayout();
    }

    public ChooseTypePanel(String id, ObjectReferenceType ref) {
        super(id, Model.of(new ObjectViewDto<>(ref != null ? ref.getOid() : null, ref != null ? WebComponentUtil
                .getOrigStringFromPoly(ref.getTargetName()) : null)));
        initLayout();
    }

    protected void initLayout() {
        final TextField<String> name = new TextField<>(ID_OBJECT_NAME, new PropertyModel<>(getModel(), ObjectViewDto.F_NAME));
        name.setOutputMarkupId(true);

        AjaxLink<String> choose = new AjaxLink<>(ID_LINK_CHOOSE) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                changeOptionPerformed(target);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setChannel(new AjaxChannel("blocking", AjaxChannel.Type.ACTIVE));
            }
        };
        choose.setOutputMarkupId(true);

        AjaxLink<String> remove = new AjaxLink<>(ID_LINK_REMOVE) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                setToDefault(target);
                target.add(name);
            }
        };
        remove.setOutputMarkupId(true);

        add(name);
        add(choose);
        add(remove);
    }

    protected ObjectQuery getChooseQuery() {
        return null;
    }

    private void choosePerformed(AjaxRequestTarget target, T object) {
        getPageBase().hideMainPopup(target);
        ObjectViewDto<T> o = getModel().getObject();

        o.setName(WebComponentUtil.getName(object));
        o.setOid(object.getOid());
        o.setObject((PrismObject) object.asPrismObject().clone());

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Choose operation performed: {} ({})", o.getName(), o.getOid());
        }

        target.add(getObjectNameComponent());
        executeCustomAction(target, object);
    }

    protected void executeCustomAction(AjaxRequestTarget target, T object) {

    }

    private void changeOptionPerformed(AjaxRequestTarget target) {
        Class<T> type = getObjectTypeClass();
        List<QName> supportedTypes = new ArrayList<>();
        supportedTypes.add(WebComponentUtil.classToQName(getPageBase().getPrismContext(), type));
        ObjectBrowserPanel<T> objectBrowserPanel = new ObjectBrowserPanel<T>(getPageBase().getMainPopupBodyId(),
                type, supportedTypes, false, getPageBase(), getChooseQuery() != null ? getChooseQuery().getFilter() : null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSelectPerformed(AjaxRequestTarget target, T focus) {
                choosePerformed(target, focus);
            }
        };
        objectBrowserPanel.setOutputMarkupId(true);

        getPageBase().showMainPopup(objectBrowserPanel, target);
    }

    private void setToDefault(AjaxRequestTarget target) {
        ObjectViewDto<T> dto = new ObjectViewDto<>();
        dto.setType(getObjectTypeClass());
        getModel().setObject(dto);
        executeCustomRemoveAction(target);
    }

    protected void executeCustomRemoveAction(AjaxRequestTarget target) {

    }

    protected AttributeAppender getInputStyleClass() {
        return AttributeAppender.append("class", "col-md-4");
    }

    public Class<T> getObjectTypeClass() {
        return ChooseTypePanel.this.getModelObject().getType();
    }

    public void setPanelEnabled(boolean isEnabled) {
        get(ID_LINK_CHOOSE).setEnabled(isEnabled);
        get(ID_LINK_REMOVE).setEnabled(isEnabled);
    }

    protected TextField<?> getObjectNameComponent() {
        return (TextField<?>) get(((PageBase) getPage()).createComponentPath(ID_OBJECT_NAME));
    }
}
