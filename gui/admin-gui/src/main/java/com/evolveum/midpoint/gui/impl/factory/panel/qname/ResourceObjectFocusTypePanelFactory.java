/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.qname;

import java.io.Serializable;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.validator.ResourceObjectFocusTypeValidator;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.input.DropDownChoiceSuggestPanel;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.AbstractInputGuiComponentFactory;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author katkav
 */
@Component
public class ResourceObjectFocusTypePanelFactory extends AbstractInputGuiComponentFactory<QName> implements Serializable {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(@NotNull IW wrapper, VW valueWrapper) {

        if (wrapper.getPath() == null) {
            return false;
        }

        return ItemPath.create(
                        ResourceType.F_SCHEMA_HANDLING,
                        SchemaHandlingType.F_OBJECT_TYPE,
                        ResourceObjectTypeDefinitionType.F_FOCUS,
                        ResourceObjectFocusSpecificationType.F_TYPE)
                .equivalent(wrapper.getPath().namedSegmentsOnly());
    }

    protected List<QName> getTypesList(PrismPropertyPanelContext<QName> panelCtx) {
        return ObjectTypeListUtil.createFocusTypeList();
    }

    @Override
    public void configure(PrismPropertyPanelContext<QName> panelCtx, org.apache.wicket.Component component) {
        super.configure(panelCtx, component);
        InputPanel panel = (InputPanel) component;
        panel.getValidatableComponent().add(new ResourceObjectFocusTypeValidator(panelCtx.getItemWrapperModel()));
    }

    @Override
    public Integer getOrder() {
        return 9999;
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<QName> panelCtx) {
        List<QName> types = getTypesList(panelCtx);
        WebComponentUtil.sortObjectTypeList(types);

        DropDownChoiceSuggestPanel<QName> typePanel =
                new DropDownChoiceSuggestPanel<>(
                        panelCtx.getComponentId(),
                        panelCtx.getRealValueModel(),
                        Model.ofList(types),
                        new QNameObjectTypeChoiceRenderer(),
                        true) {

                    @Override
                    protected void onSuggestAction(@NotNull AjaxRequestTarget target) {
                        DropDownChoice<QName> baseFormComponent = getBaseFormComponent();
                        executeSuggestFocusTypeOperation(
                                getPageBase(),
                                baseFormComponent,
                                panelCtx,
                                target);
                    }
                };

        typePanel.setOutputMarkupId(true);

        return typePanel;
    }

    //NOTE: If we decide to submit suggestion in the task, we should implement more complex panel with logicDto provider.
    private void executeSuggestFocusTypeOperation(
            @NotNull PageBase pageBase,
            @NotNull DropDownChoice<QName> baseFormComponent,
            @NotNull PrismPropertyPanelContext<QName> panelCtx,
            @NotNull AjaxRequestTarget target) {
        var itemWrapper = panelCtx.getItemWrapperModel().getObject();

        ResourceObjectTypeDefinitionType objectTypeDef =
                (ResourceObjectTypeDefinitionType) itemWrapper
                        .getParent().getParent().getParent()
                        .getRealValue();

        ResourceType resource =
                (ResourceType) itemWrapper
                        .getParent().getParent().getParent()
                        .getParent().getParent().getParent().getParent()
                        .getRealValue();

        final String resourceOid = resource != null ? resource.getOid() : null;
        final ResourceObjectTypeIdentification identification =
                ResourceObjectTypeIdentification.of(objectTypeDef.getKind(), objectTypeDef.getIntent());

        Task task = pageBase.createSimpleTask("Suggest focus type");
        OperationResult result = task.getResult();

        try {
            SmartIntegrationService sis = pageBase.getSmartIntegrationService();
            QName suggestion = sis.suggestFocusType(resourceOid, identification, task, result);
            if (suggestion != null) {
                baseFormComponent.setModelObject(suggestion);
                target.add(baseFormComponent);
            } else {
                result.recordWarning("No suitable type suggestion was found.");
            }
        } catch (SchemaException | ExpressionEvaluationException | SecurityViolationException
                | CommunicationException | ConfigurationException | ObjectNotFoundException e) {
            result.recordFatalError("Couldn't suggest focus type: " + e.getMessage(), e);
        }

        pageBase.showResult(result);
    }
}
