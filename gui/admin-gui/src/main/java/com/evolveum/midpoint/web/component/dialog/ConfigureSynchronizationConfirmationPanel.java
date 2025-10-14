/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

/**
 * Confirmation dialog shown when synchronization configuration is missing for a resource object type.
 * Allows the user to automatically create default synchronization reactions or open the configuration wizard.
 */
public class ConfigureSynchronizationConfirmationPanel extends ConfirmationPanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONFIGURE = "configure";

    ItemPath synchronizationContainerPath;
    IModel<ResourceDetailsModel> resourceDetailsModel;

    public ConfigureSynchronizationConfirmationPanel(
            String id,
            IModel<String> message,
            ItemPath synchronizationContainerPath,
            IModel<ResourceDetailsModel> resourceDetailsModel) {
        super(id, message);
        this.synchronizationContainerPath = synchronizationContainerPath;
        this.resourceDetailsModel = resourceDetailsModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initPreviewRecommendedButton();
    }

    private void initPreviewRecommendedButton() {
        WebMarkupContainer footer = (WebMarkupContainer) getFooter();
        AjaxButton previewRecommended = new AjaxButton(ID_CONFIGURE,
                createStringResource("ConfigureSynchronizationConfirmationPanel.useDefault")) {

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
        footer.add(previewRecommended);
    }

    protected void onPreviewRecommendedPerformed(AjaxRequestTarget target) throws SchemaException {
        performSynchronizationConfiguration(target);
    }

    @Override
    public void yesPerformed(AjaxRequestTarget target) {
        performSynchronizationConfiguration(target);
    }

    protected void performSynchronizationConfiguration(AjaxRequestTarget target) {
        var synchronizationWrapper = createDefaultSynchronizationWrapper(target);
        navigateToSynchronizationWizard(target, synchronizationWrapper);
    }

    private void navigateToSynchronizationWizard(AjaxRequestTarget target,
            LoadableModel<PrismContainerValueWrapper<SynchronizationReactionsType>> synchronizationWrapper) {
        getAssignmentHolderDetailsModel().getPageResource().showSynchronizationWizard(synchronizationWrapper, target);
    }

    @Override
    protected IModel<String> createYesLabel() {
        return createStringResource("ConfigureSynchronizationConfirmationPanel.customize");
    }

    @Override
    protected IModel<String> createNoLabel() {
        return createStringResource("ConfigureSynchronizationConfirmationPanel.cancel");
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("CorrelationWizardPanelWizardPanel.synchronizationDialog.missing.message");
    }

    @Override
    public int getWidth() {
        return 800;
    }

    /**
     * Creates a lazy model that loads the synchronization container value wrapper,
     * initializing it with a default set of synchronization reactions if none exist.
     */
    protected @NotNull LoadableModel<PrismContainerValueWrapper<SynchronizationReactionsType>> createDefaultSynchronizationWrapper(
            @NotNull AjaxRequestTarget target) {
        return new LoadableModel<>(false) {
            @Override
            protected PrismContainerValueWrapper<SynchronizationReactionsType> load() {
                try {
                    var objectWrapper = getAssignmentHolderDetailsModel().getObjectWrapper();
                    PrismContainerWrapper<SynchronizationReactionsType> container = objectWrapper
                            .findContainer(getSynchronizationContainerPath());
                    initDefaultSynchronizationReaction(container, target);
                    return container.getValue();
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }

            }
        };
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
    private void initDefaultSynchronizationReaction(
            @NotNull PrismContainerWrapper<SynchronizationReactionsType> reactionsContainer,
            @NotNull AjaxRequestTarget target) throws SchemaException {
        PrismContainerWrapper<SynchronizationReactionType> reactionContainer = reactionsContainer
                .findContainer(SynchronizationReactionsType.F_REACTION);
        PrismContainer<SynchronizationReactionType> reactionItem = reactionContainer.getItem();

        var linkedSynchronize = reactionItem.createNewValue();
        linkedSynchronize.asContainerable().name("linked-synchronize")
                .situation(SynchronizationSituationType.LINKED)
                .beginActions()
                .beginSynchronize()
                .synchronize(true)
                .end();

        WebPrismUtil.createNewValueWrapper(reactionContainer, linkedSynchronize, getPageBase(), target);

        var unlinkedLink = reactionItem.createNewValue();
        unlinkedLink.asContainerable().name("unlinked-link")
                .situation(SynchronizationSituationType.UNLINKED)
                .beginActions()
                .beginLink()
                .synchronize(true)
                .end();
        WebPrismUtil.createNewValueWrapper(reactionContainer, unlinkedLink, getPageBase(), target);

        var unmatchedAddFocus = reactionItem.createNewValue();
        unmatchedAddFocus.asContainerable().name("unmatched-add-focus")
                .situation(SynchronizationSituationType.UNMATCHED)
                .beginActions()
                .beginAddFocus()
                .synchronize(true)
                .end();
        WebPrismUtil.createNewValueWrapper(reactionContainer, unmatchedAddFocus, getPageBase(), target);
    }

    private ItemPath getSynchronizationContainerPath() {
        return synchronizationContainerPath;
    }

    private ResourceDetailsModel getAssignmentHolderDetailsModel() {
        return resourceDetailsModel.getObject();
    }
}
