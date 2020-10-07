/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.column;

import com.evolveum.midpoint.prism.Containerable;

/**
 * @author skublik
 */
public class LinkPrismPropertyColumn<C extends Containerable, T> { //extends PrismPropertyColumn<C, T> implements IExportableColumn<PrismContainerValueWrapper<C>, String>{


//    private static final long serialVersionUID = 1L;
//    private static final String ID_LABEL = "label";
//
//    public LinkPrismPropertyColumn(IModel<PrismContainerWrapper<C>> mainModel, ItemPath itemName, PageBase pageBase) {
//        super(mainModel, itemName, pageBase, false);
//    }
//
//
//    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> rowModel) {
//    }
//
//    @Override
//    protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
//        AjaxLinkPanel linkPanel = new AjaxLinkPanel(ID_LABEL,
//                new ItemRealValueModel(rowModel)) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                LinkPrismPropertyColumn.this.onClick(target, (IModel<PrismContainerValueWrapper<C>>) rowModel);
//            }
//
//            @Override
//            public boolean isEnabled() {
//                return !isReadOnly();
//            }
//        };
//        item.add(linkPanel);
//    }
//
//    class LinkPrismPropertyColumnPane

//    @Override
//    protected void populate(ListItem<PrismPropertyValueWrapper<T>> item, IModel<?> rowModel) {
//        AjaxLinkPanel linkPanel = new AjaxLinkPanel(ID_LABEL,
//                new ItemRealValueModel(item.getModel())) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                LinkPrismPropertyColumn.this.onClick(target, (IModel<PrismContainerValueWrapper<C>>) rowModel);
//            }
//
//            @Override
//            public boolean isEnabled() {
//                return !isReadOnly();
//            }
//        };
//        item.add(linkPanel);
//    }


//        PropertyOrReferenceWrapperFromContainerModel property = new PropertyOrReferenceWrapperFromContainerModel<>(rowModel.getObject(), getqNameOfItem());
//        cellItem.add(new StaticItemWrapperColumnPanel(componentId, property, new Form("form"), null) {
//            @Override
//            protected Component getDisplayComponent(String componentId, IModel model) {
//                return new AjaxLinkPanel(componentId, model) {
//                    private static final long serialVersionUID = 1L;
//
//                    @Override
//                    public void onClick(AjaxRequestTarget target) {
//                        LinkPrismPropertyColumn.this.onClick(target, rowModel);
//                    }
//
//                    @Override
//                    public boolean isEnabled() {
//                        return LinkPrismPropertyColumn.this.isEnabled(rowModel);
//                    }
//                };
//            }
//
//            @Override
//            protected IModel populatePropertyItem(ValueWrapperOld object) {
//                return LinkPrismPropertyColumn.this.populatePropertyItem(object);
//            }
//
//            @Override
//            protected IModel populateReferenceItem(ValueWrapperOld object) {
//                return LinkPrismPropertyColumn.this.populateReferenceItem(object);
//            }
//        });Pri



}


