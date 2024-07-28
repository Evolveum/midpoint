/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import java.awt.*;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerPanel;
import com.evolveum.midpoint.prism.PrismContainer;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.factory.wrapper.PrismContainerWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormDefaultContainerablePanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

public class ConfirmationPanelWithComment extends ConfirmationPanel implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";

    LoadableModel<PrismContainerValueWrapper<Containerable>> model;

    private IModel<GuiConfirmationActionType> confirmationModel;
    private List<Containerable> objectsToProcess;

    public ConfirmationPanelWithComment(String id,
            IModel<String> messageModel,
            IModel<GuiConfirmationActionType> confirmationModel,
            List<Containerable> objectsToProcess) {
        super(id, messageModel);
        this.confirmationModel = confirmationModel;
        this.objectsToProcess = objectsToProcess;

        this.model = new LoadableModel<>(false) {
            @Override
            protected PrismContainerValueWrapper<Containerable> load() {
                return loadWrapper();
            }
        };
    }

    @Override
    protected void initLayout() {
        super.initLayout();

        ListView<VirtualContainersSpecificationType> items = new ListView<>(ID_ITEMS, () -> confirmationModel.getObject().getContainer()) {
            @Override
            protected void populateItem(ListItem<VirtualContainersSpecificationType> item) {

                IModel<PrismContainerWrapper<Containerable>> virtualContainerModel = createVirtualContainerModel(model, item.getModelObject());

                ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                        .visibilityHandler((wrapper) -> ItemVisibility.AUTO)
                        .mandatoryHandler((wrapper) -> isMandatoryItem(item.getModelObject(), wrapper))
                        .build();

                VerticalFormPrismContainerPanel<Containerable> panel = new VerticalFormPrismContainerPanel<>(ID_ITEM, virtualContainerModel, settings);
                item.add(panel);

                item.add(panel);
            }
        };
        add(items);


    }

    //TODO copied from single container model
    protected IModel<PrismContainerWrapper<Containerable>> createVirtualContainerModel(IModel<PrismContainerValueWrapper<Containerable>> model, VirtualContainersSpecificationType virtualContainer) {
        if (virtualContainer.getPath() != null) {
            return createContainerModel(model, virtualContainer.getPath().getItemPath());
        }
        if (StringUtils.isBlank(virtualContainer.getIdentifier())) {
            getSession().error(getString("SingleContainerPanel.empty.identifier", virtualContainer));
            return null;
        }
        return PrismContainerWrapperModel.fromContainerValueWrapper(model, virtualContainer.getIdentifier());
    }

    //TODO copied from single container model
    public <C extends Containerable> IModel<PrismContainerWrapper<C>> createContainerModel(IModel<PrismContainerValueWrapper<Containerable>> model, ItemPath path) {
        return PrismContainerWrapperModel.fromContainerValueWrapper(model, path);
    }

    private PrismContainerValueWrapper<Containerable> loadWrapper() {
        //TODO type
        Containerable objectToProcess = objectsToProcess.get(0);
        PrismContainerDefinition<Containerable> definition = objectToProcess.asPrismContainerValue().getDefinition();
        PrismContainerWrapperFactory<Containerable> cwf = getPageBase().getRegistry().findContainerWrapperFactory(definition);
        Task task = getPageBase().createSimpleTask("createWrapper");
        WrapperContext ctx = new WrapperContext(task, task.getResult());
        ctx.setCreateIfEmpty(true);
        ctx.setShowEmpty(true);
        ctx.forceCreateVirtualContainer(confirmationModel.getObject().getContainer());

        PrismContainerValueWrapper<Containerable> wrapper;
        try {
            wrapper = cwf.createValueWrapper(null, objectToProcess.cloneWithoutId().asPrismContainerValue(), ValueStatus.ADDED, ctx);
        } catch (SchemaException e) {
            throw new RuntimeException(e);//todo proper handling

        }
        return wrapper;
    }

    public void yesPerformed(AjaxRequestTarget target) {
            try {
                PrismContainerValueWrapper<Containerable> iw = model.getObject();
                Collection<ItemDelta<?, ?>> computedDeltas = iw.getDeltas();
                yesPerformedWithComment(target, computedDeltas);
            } catch (SchemaException e) {
                throw new RuntimeException(e);
            }
    }

    protected void yesPerformedWithComment(AjaxRequestTarget target, Collection<ItemDelta<?, ?>> deltas) {
        // to be overridden
    }

    private boolean isMandatoryItem(VirtualContainersSpecificationType container, ItemWrapper<?, ?> wrapper) {
        GuiConfirmationActionType confirmation = confirmationModel.getObject();
        if (confirmation == null) {
            return false;
        }
        VirtualContainerItemSpecificationType virtItem = container.getItem()
                .stream()
                .filter(item -> item.getPath() != null && wrapper.getPath().equivalent(item.getPath().getItemPath()))
                .findFirst()
                .orElse(null);
        if (virtItem == null) {
            return false;
        }
        return Boolean.TRUE.equals(virtItem.isMandatory());
    }
}
