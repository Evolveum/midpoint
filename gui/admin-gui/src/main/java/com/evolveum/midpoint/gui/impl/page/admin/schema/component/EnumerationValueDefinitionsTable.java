/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.component.dialog.OnePanelPopupPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardTable;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnumerationValueDefinitionsTable extends AbstractWizardTable<EnumerationValueTypeDefinitionType, EnumerationTypeDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(EnumerationValueDefinitionsTable.class);

    private enum Type {
        PROPERTY,
        REFERENCE,
        CONTAINER
    }

    public EnumerationValueDefinitionsTable(
            String id, IModel<PrismContainerValueWrapper<EnumerationTypeDefinitionType>> valueModel,
            ContainerPanelConfigurationType config) {
        super(id, valueModel, config, EnumerationValueTypeDefinitionType.class);
    }

    @Override
    protected IModel<PrismContainerWrapper<EnumerationValueTypeDefinitionType>> getContainerModel() {
        return PrismContainerWrapperModel.fromContainerValueWrapper(
                getValueModel(),
                EnumerationTypeDefinitionType.F_VALUES);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return null;
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<EnumerationValueTypeDefinitionType>, String> createCheckboxColumn() {
        return null;
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<EnumerationValueTypeDefinitionType>, String>> createDefaultColumns() {
        LoadableDetachableModel<PrismContainerDefinition<EnumerationValueTypeDefinitionType>> defModel = new LoadableDetachableModel() {
            @Override
            protected PrismContainerDefinition<EnumerationValueTypeDefinitionType> load() {
                PrismContainerDefinition<PrismSchemaType> schemaDef =
                        PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(PrismSchemaType.class);
                return schemaDef.findContainerDefinition(
                        ItemPath.create(PrismSchemaType.F_ENUMERATION_TYPE, EnumerationTypeDefinitionType.F_VALUES));
            }
        };

        return Arrays.asList(
                new PrismPropertyWrapperColumn<>(defModel, EnumerationValueTypeDefinitionType.F_VALUE,
                        AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()){
                    @Override
                    public String getCssClass() {
                        return "text-nowrap";
                    }
                },
//                new PrismPropertyWrapperColumn<>(defModel, EnumerationValueTypeDefinitionType.F_CONSTANT_NAME,
//                        AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()){
//                    @Override
//                    public String getCssClass() {
//                        return "text-nowrap";
//                    }
//                },
                new PrismPropertyWrapperColumn<>(defModel, EnumerationValueTypeDefinitionType.F_DOCUMENTATION,
                        AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()){
                    @Override
                    public String getCssClass() {
                        return "text-nowrap";
                    }
                }
        );
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> menuItems = new ArrayList<>();
        menuItems.add(createDeleteInlineMenu());
        menuItems.add(createEditInlineMenu());
        return menuItems;
    }

    @Override
    protected ButtonInlineMenuItem createDeleteInlineMenu() {
        ButtonInlineMenuItem menu = new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createDeleteColumnAction();
            }
        };
        menu.setVisibilityChecker(
                (InlineMenuItem.VisibilityChecker) (rowModel, isHeader) -> {
                    if (isHeader){
                        return isHeaderVisible();
                    }
                    PrismContainerValueWrapper<PrismItemDefinitionType> rowObject =
                            (PrismContainerValueWrapper<PrismItemDefinitionType>) rowModel.getObject();
                    return ValueStatus.ADDED.equals(rowObject.getStatus());
                });
        return menu;
    }

    @Override
    protected boolean allowEditMultipleValuesAtOnce() {
        return false;
    }

    @Override
    public void editItemPerformed(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<EnumerationValueTypeDefinitionType>> rowModel,
            List<PrismContainerValueWrapper<EnumerationValueTypeDefinitionType>> listItems) {

        if (isValidFormComponentsOfRow(rowModel, target)) {

            PageAssignmentHolderDetails parent = findParent(PageAssignmentHolderDetails.class);
            if (parent == null) {
                warn("Couldn't create popup for new item");
                return;
            }

            if ((listItems != null && !listItems.isEmpty()) || rowModel != null) {
                IModel<PrismContainerValueWrapper<EnumerationValueTypeDefinitionType>> valueModel;
                if (rowModel == null) {
                    valueModel = () -> listItems.iterator().next();
                } else {
                    valueModel = rowModel;
                }
                if (valueModel != null) {
                    OnePanelPopupPanel popup = new OnePanelPopupPanel(
                            getPageBase().getMainPopupBodyId(),
                            createStringResource("EnumerationValueDefinitionsTable.modifyProperty")) {
                        @Override
                        protected WebMarkupContainer createPanel(String id) {
                            return new BasicEnumValuePanel(
                                    (AssignmentHolderDetailsModel<SchemaType>) parent.getObjectDetailsModels(), valueModel) {
                                @Override
                                public String getId() {
                                    return id;
                                }
                            };
                        }

                        @Override
                        protected void processHide(AjaxRequestTarget target) {
                            WebComponentUtil.showToastForRecordedButUnsavedChanges(target, valueModel.getObject());
                            target.add(getTableComponent());
                            super.processHide(target);
                        }
                    };
                    popup.setOutputMarkupId(true);
                    getPageBase().showMainPopup(popup, target);
                }
            } else {
                warn(createStringResource("MultivalueContainerListPanel.message.noItemsSelected").getString());
                target.add(getPageBase().getFeedbackPanel());
            }
        }
    }

    protected String getKeyOfTitleForNewObjectButton() {
        return "EnumerationValueDefinitionsTable.newObject";
    }

    protected String getNewButtonCssClass() {
        return "btn btn-primary btn-sm";
    }

    @Override
    protected boolean isDuplicationSupported() {
        return false;
    }
}
