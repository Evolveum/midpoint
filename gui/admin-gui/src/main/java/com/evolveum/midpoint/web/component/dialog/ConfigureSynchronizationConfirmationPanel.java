/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

public abstract class ConfigureSynchronizationConfirmationPanel extends ConfirmationPanel {

    @Serial private static final long serialVersionUID = 1L;
    private static final String ID_CONFIGURE = "configure";

    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDefinitionWrapperModel;
    private IModel<String> infoMessage = null;

    public ConfigureSynchronizationConfirmationPanel(
            String id,
            IModel<String> message,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDefinitionWrapperModel) {
        super(id, message);
//        infoMessage = createInfoMessageModel();
        this.resourceObjectTypeDefinitionWrapperModel = resourceObjectTypeDefinitionWrapperModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        MessagePanel<?> infoMessagePanel = new MessagePanel<>("infoMessage", MessagePanel.MessagePanelType.INFO, infoMessage);
        infoMessagePanel.setOutputMarkupId(true);
        infoMessagePanel.add(new VisibleBehaviour(() -> infoMessage != null));
        add(infoMessagePanel);
    }

    @Override
    protected void customInitLayout(@NotNull WebMarkupContainer panel) {
        AjaxButton previewRecommended = new AjaxButton(ID_CONFIGURE,
                new StringResourceModel("ConfigureSynchronizationConfirmationPanel.useDefault", this, null)) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                try {
                    onPreviewRecommendedPerformed(target);
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        previewRecommended.setOutputMarkupId(true);
        previewRecommended.add(new VisibleBehaviour((this::isConfigurationTaskVisible)));
        panel.add(previewRecommended);
    }

    protected void onPreviewRecommendedPerformed(AjaxRequestTarget target) throws SchemaException {
        setDefaultSynchronizationValues(target);
        navigateToSynchronizationConfigWizard(target);
    }

    @Contract("_, _ -> new")
    public static <C extends Containerable> @NotNull IModel<PrismContainerWrapper<C>> createSynchronizationItemContainerModel(
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model, @Nullable ItemPath path) {
        return PrismContainerWrapperModel.fromContainerValueWrapper(model, path);
    }

    //TODO wrong way of setting default values, should be done using container wrapper model? Check it.
    public void setDefaultSynchronizationValues(
            @NotNull AjaxRequestTarget target) {
        var wrapper = getResourceObjectTypeDefinitionWrapperModel();
        IModel<PrismContainerWrapper<SynchronizationReactionType>> containerModel = createSynchronizationItemContainerModel(
                wrapper, ItemPath.create(
                        ResourceObjectTypeDefinitionType.F_SYNCHRONIZATION, SynchronizationReactionsType.F_REACTION));

        PrismContainerWrapper<SynchronizationReactionType> container = containerModel.getObject();
        addDefaultSynchronizationReactions(container, target);
    }

    @Override
    public void yesPerformed(AjaxRequestTarget target) {
        navigateToSynchronizationConfigWizard(target);
    }

    protected abstract void navigateToSynchronizationConfigWizard(AjaxRequestTarget target);

    @Override
    protected IModel<String> createYesLabel() {
        return createStringResource("ConfigureSynchronizationConfirmationPanel.customize");
    }

    @Override
    protected IModel<String> createNoLabel() {
        return createStringResource("ConfigureSynchronizationConfirmationPanel.cancel");
    }

    protected PrismObject<TaskType> createTask(AjaxRequestTarget target) {
        return null;
    }

    protected IModel<String> createInfoMessageModel() {
        return createStringResource("ConfigureSynchronizationConfirmationPanel.infoMessage");
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("CorrelationWizardPanelWizardPanel.synchronizationDialog.missing.message");
    }

    public boolean isConfigurationTaskVisible() {
        return true;
    }

    @Override
    public int getWidth() {
        return 800;
    }

    private IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getResourceObjectTypeDefinitionWrapperModel() {
        return resourceObjectTypeDefinitionWrapperModel;
    }

    /**
     * Creates a default set of synchronization reactions for a resource suggested object type.
     * <p>
     * The following simple reactions are configured:
     * <ul>
     *   <li>{@link SynchronizationSituationType#LINKED} → synchronize the object.</li>
     *   <li>{@link SynchronizationSituationType#UNLINKED} → link the object.</li>
     *   <li>{@link SynchronizationSituationType#UNMATCHED} → add a new focus object.</li>
     * </ul>
     * These defaults are meant as a starting point for basic synchronization scenarios.
     */
    private void addDefaultSynchronizationReactions(PrismContainerWrapper<SynchronizationReactionType> container, @NotNull AjaxRequestTarget target) {
        PrismContainer<SynchronizationReactionType> item = container.getItem();

        var newValue = item.createNewValue();
        @NotNull SynchronizationReactionType bean = newValue.asContainerable();
        bean.name("linked-synchronize")
                .situation(SynchronizationSituationType.LINKED)
                .beginActions()
                .beginSynchronize()
                .synchronize(true)
                .end();

        WebPrismUtil.createNewValueWrapper(container, newValue, getPageBase(), target);

        newValue = item.createNewValue();
        bean = newValue.asContainerable();
        bean.name("unlinked-link")
                .situation(SynchronizationSituationType.UNLINKED)
                .beginActions()
                .beginLink()
                .synchronize(true)
                .end();
        WebPrismUtil.createNewValueWrapper(container, newValue, getPageBase(), target);

        newValue = item.createNewValue();
        bean = newValue.asContainerable();
        bean.name("unmatched-add-focus")
                .situation(SynchronizationSituationType.UNMATCHED)
                .beginActions()
                .beginAddFocus()
                .synchronize(true)
                .end();
        WebPrismUtil.createNewValueWrapper(container, newValue, getPageBase(), target);
    }
}
