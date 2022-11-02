/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.search.AbstractSearchItemWrapper;
import com.evolveum.midpoint.web.application.Counter;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
@PanelType(name = "classLoggersPanel")
@PanelInstance(
        identifier = "classLoggersPanel",
        applicableForType = LoggingConfigurationType.class,
        display = @PanelDisplay(
                label = "ClassLoggersContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 20
        )
)
@Counter(provider = ClassLoggersMenuLinkCounter.class)
public class ClassLoggersContentPanel extends MultivalueContainerListPanel<ClassLoggerConfigurationType> {

    private static final long serialVersionUID = 1L;

    private IModel<PrismContainerWrapper<ClassLoggerConfigurationType>> model;

    public ClassLoggersContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, ClassLoggerConfigurationType.class, configurationType);

        this.model = PrismContainerWrapperModel.fromContainerWrapper(model.getObjectWrapperModel(), ItemPath.create(
                SystemConfigurationType.F_LOGGING,
                LoggingConfigurationType.F_CLASS_LOGGER
        ));
    }

    @Override
    protected void newItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation) {
        PrismContainerWrapper<ClassLoggerConfigurationType> wrapper = model.getObject();

        PrismContainerValue<ClassLoggerConfigurationType> newLogger = wrapper.getItem().createNewValue();
        PrismContainerValueWrapper<ClassLoggerConfigurationType> newLoggerWrapper = createNewItemContainerValueWrapper(newLogger, wrapper, target);
        loggerEditPerformed(target, Model.of(newLoggerWrapper), null);
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<ClassLoggerConfigurationType>> getContainerModel() {
        return model;
    }

    @Override
    protected String getStorageKey() {
        return SessionStorage.KEY_LOGGING_TAB_LOGGER_TABLE;
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.LOGGING_TAB_LOGGER_TABLE;
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ClassLoggerConfigurationType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<ClassLoggerConfigurationType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());
        columns.add(new IconColumn<>(Model.of("")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<ClassLoggerConfigurationType>> rowModel) {
                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(SystemConfigurationType.COMPLEX_TYPE));

            }

        });

        columns.add(new PrismPropertyWrapperColumn(model, ClassLoggerConfigurationType.F_PACKAGE, AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()) {

            @Override
            public String getCssClass() {
                return "mp-w-5";
            }
        });
        columns.add(new PrismPropertyWrapperColumn<>(model, ClassLoggerConfigurationType.F_LEVEL, AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()));
        columns.add(new PrismPropertyWrapperColumn<>(model, ClassLoggerConfigurationType.F_APPENDER, AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()));

        List<InlineMenuItem> menuActionsList = getDefaultMenuActions();
        columns.add(new InlineMenuButtonColumn(menuActionsList, getPageBase()) {

            @Override
            public String getCssClass() {
                return "mp-w-1";
            }
        });

        return columns;
    }

    @Override
    protected void editItemPerformed(AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<ClassLoggerConfigurationType>> rowModel,
            List<PrismContainerValueWrapper<ClassLoggerConfigurationType>> listItems) {
        loggerEditPerformed(target, rowModel, listItems);
    }

    private void loggerEditPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ClassLoggerConfigurationType>> rowModel,
            List<PrismContainerValueWrapper<ClassLoggerConfigurationType>> listItems) {
        if (rowModel != null) {
            PrismContainerValueWrapper<ClassLoggerConfigurationType> logger = rowModel.getObject();
            logger.setSelected(true);
        } else {
            for (PrismContainerValueWrapper<ClassLoggerConfigurationType> logger : listItems) {
                logger.setSelected(true);
            }
        }
        getTable().goToLastPage();
        target.add(this);
    }
}
