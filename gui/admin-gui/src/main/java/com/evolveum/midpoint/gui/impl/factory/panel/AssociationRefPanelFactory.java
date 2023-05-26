/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.input.validator.NotNullValidator;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;

/**
 * @author katka
 */
@Component
public class AssociationRefPanelFactory extends ItemPathPanelFactory {

    private static final long serialVersionUID = 1L;

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return super.match(wrapper)
                && wrapper.getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                    ResourceType.F_SCHEMA_HANDLING,
                    SchemaHandlingType.F_OBJECT_TYPE,
                    ResourceObjectTypeDefinitionType.F_ASSOCIATION,
                    ResourceObjectAssociationType.F_REF));
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<ItemPathType> panelCtx) {
        IModel<String> model = new IModel<>() {
            @Override
            public String getObject() {
                return GuiDisplayNameUtil.getDisplayName(panelCtx.getRealValueModel().getObject());
            }

            @Override
            public void setObject(String object) {
                if (StringUtils.isEmpty(object)) {
                    return;
                }

                QName qName;
                if (object.contains(":")) {
                    int index = object.indexOf(":");
                    qName = new QName(null, object.substring(index + 1), object.substring(0, index));
                } else {
                    qName = new QName(object);
                }

                ItemPathType itemPathType = new ItemPathType(ItemName.fromQName(qName));
                panelCtx.getRealValueModel().setObject(itemPathType);
            }
        };

        TextPanel<String> panel = new TextPanel<>(panelCtx.getComponentId(), model, String.class, false);
        panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        return panel;
    }

    @Override
    public Integer getOrder() {
        return 10000;
    }
}
