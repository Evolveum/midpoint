/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.logging;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.input.QNameIChoiceRenderer;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.application.Counter;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
@PanelType(name = "appendersPanel")
@PanelInstance(
        identifier = "appendersPanel",
        applicableForType = LoggingConfigurationType.class,
        display = @PanelDisplay(
                label = "AppendersContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 40
        )
)
@Counter(provider = AppendersMenuLinkCounter.class)
public class AppendersContentPanel extends MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType> {

    private DropDownChoicePanel<QName> newAppenderChoice;

    private IModel<PrismContainerWrapper<AppenderConfigurationType>> model;

    public AppendersContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, AppenderConfigurationType.class, configurationType);

        this.model = PrismContainerWrapperModel.fromContainerWrapper(model.getObjectWrapperModel(), ItemPath.create(
                SystemConfigurationType.F_LOGGING,
                LoggingConfigurationType.F_APPENDER
        ));
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<AppenderConfigurationType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AppenderConfigurationType>, String>> createDefaultColumns() {
        return Arrays.asList(
                new PrismPropertyWrapperColumn<>(getContainerModel(), AppenderConfigurationType.F_NAME,
                        AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<AppenderConfigurationType>> model) {
                        AppendersContentPanel.this.itemDetailsPerformed(target, model);
                    }
                },
                new PrismPropertyWrapperColumn<>(getContainerModel(), AppenderConfigurationType.F_PATTERN,
                        AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<AppenderConfigurationType>> model) {
                        AppendersContentPanel.this.itemDetailsPerformed(target, model);
                    }
                }
        );
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<AppenderConfigurationType>> getContainerModel() {
        return model;
    }

    @Override
    protected MultivalueContainerDetailsPanel<AppenderConfigurationType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<AppenderConfigurationType>> item) {

        return new AppenderDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_APPENDERS_CONTENT;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return getDefaultMenuActions();
    }

    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        Component choice = createNewAppenderChoice(idButton);
        // todo push html through title? not very nice
        AjaxSubmitButton newObjectIcon = new AjaxSubmitButton(idButton, new Model<>("<i class=\"fa fa-plus\"></i>")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                newItemPerformed(target, null);
            }
        };

        newObjectIcon.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isCreateNewObjectVisible();
            }

            @Override
            public boolean isEnabled() {
                return isNewObjectButtonEnabled();
            }
        });

        newObjectIcon.add(AttributeModifier.append("class", createStyleClassModelForNewObjectIcon()));

        return Arrays.asList(choice, newObjectIcon);
    }

    private Component createNewAppenderChoice(String id) {
        List<QName> choices = Arrays.asList(FileAppenderConfigurationType.COMPLEX_TYPE, SyslogAppenderConfigurationType.COMPLEX_TYPE);

        DropDownChoicePanel<QName> choice = new DropDownChoicePanel<>(
                id,
                Model.of(FileAppenderConfigurationType.COMPLEX_TYPE),
                Model.ofList(choices),
                new QNameIChoiceRenderer("AppendersContentPanel.appendersChoice"));
        choice.setOutputMarkupId(true);

        this.newAppenderChoice = choice;

        return choice;
    }

    @Override
    protected void newItemPerformed(PrismContainerValue<AppenderConfigurationType> value, AjaxRequestTarget target, AssignmentObjectRelation relationSepc) {
        PrismContainerValue<AppenderConfigurationType> container = value;
        if (container == null) {
            if (QNameUtil.match(newAppenderChoice.getModel().getObject(), FileAppenderConfigurationType.COMPLEX_TYPE)) {
                container = new FileAppenderConfigurationType().asPrismContainerValue();
            } else {
                container = new SyslogAppenderConfigurationType().asPrismContainerValue();
            }
            container.setParent(model.getObject().getItem());
        }

        PrismContainerValueWrapper<AppenderConfigurationType> newAppenderContainerWrapper = createNewItemContainerValueWrapper(container, model.getObject(), target);
        itemDetailsPerformed(target, Collections.singletonList(newAppenderContainerWrapper));
    }
}
