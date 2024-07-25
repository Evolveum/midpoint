/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

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
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiParameterType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConfirmationPanelWithComment extends ConfirmationPanel implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";

    IModel<List<ItemWrapper<?, ?>>> model;

    public ConfirmationPanelWithComment(String id, IModel<String> messageModel, List<GuiParameterType> parameters, List<? extends Containerable> objectsToProcess) {
        super(id, messageModel);

        this.model = () -> loadAttributesModel(parameters, objectsToProcess);
    }

    private List<ItemWrapper<?, ?>> loadAttributesModel(List<GuiParameterType> parameters, List<? extends Containerable> objectsToProcess) {
        Containerable object = objectsToProcess.get(0);
        PrismContainerWrapperFactory<Containerable> cwf =getPageBase().getRegistry().findContainerWrapperFactory(object.asPrismContainerValue().getDefinition());
        Task task = getPageBase().createSimpleTask("createWrapper");
        WrapperContext ctx = new WrapperContext(task, task.getResult());
        ctx.setCreateIfEmpty(true);

        PrismContainerWrapper<Containerable> wrapper;
        try {
            wrapper = cwf.createWrapper(null, object.asPrismContainerValue().getContainer(), ItemStatus.ADDED, ctx);
        } catch (SchemaException e) {
            throw new RuntimeException(e);//todo proper handling

        }
        List<ItemWrapper<?, ?>> itemsToShow = new ArrayList<>();
        for (GuiParameterType parameter : parameters) {
            try {
                List<PrismContainerValueWrapper<Containerable>> values = wrapper.getValues();
                for (PrismContainerValueWrapper<Containerable> value : values) {
                    ItemWrapper iw = value.findItem(parameter.getPath().getItemPath(), ItemWrapper.class);
                    if (iw != null) {
                        itemsToShow.add(iw);
                    }
                }
            } catch (SchemaException e) {
//                    throw new RuntimeException(e);
                //TODO log error
                continue;
            }
        }

        return itemsToShow;
    }

    @Override
    protected void initLayout() {
        super.initLayout();


        ListView<ItemWrapper<?, ?>> properties = new ListView<>(ID_ITEMS, model) {

            @Override
            protected void populateItem(ListItem<ItemWrapper<?, ?>> item) {
                populateNonContainer(item);
            }
        };
        properties.setOutputMarkupId(true);
        add(properties);
    }

    private void populateNonContainer(ListItem<? extends ItemWrapper<?, ?>> item) {
        item.setOutputMarkupId(true);

        ItemPanelSettings settings = new ItemPanelSettingsBuilder().visibilityHandler((wrapper) -> ItemVisibility.AUTO).build();
        ItemPanel propertyPanel = WebPrismUtil.createVerticalPropertyPanel(ID_ITEM, item.getModel(), settings);

        item.add(propertyPanel);
    }

    public void yesPerformed(AjaxRequestTarget target) {
        Collection<ItemDelta<?, ?>> deltas = new ArrayList<>();
        for (ItemWrapper<?, ?> iw : model.getObject()) {
            try {
                Collection<ItemDelta<?, ?>> computedDeltas = iw.getDelta();
                if (computedDeltas != null) {
                    deltas.addAll(computedDeltas);
                }
            } catch (SchemaException e) {
                throw new RuntimeException(e);
            }
        }
        yesPerformedWithComment(target, deltas);
    }

    protected void yesPerformedWithComment(AjaxRequestTarget target, Collection<ItemDelta<?, ?>> deltas) {
        // to be overridden
    }

}
