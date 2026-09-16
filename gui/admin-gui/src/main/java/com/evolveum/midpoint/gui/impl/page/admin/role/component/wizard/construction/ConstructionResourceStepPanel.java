/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.input.LifecycleStatePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.SingleTileWizardStepPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ConstructionValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.util.SelectableBean;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.LoadableDetachableModel;

import javax.xml.namespace.QName;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

@PanelType(name = "arw-construction-resource")
@PanelInstance(identifier = "arw-construction-resource",
        applicableForType = AbstractRoleType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRole.wizard.step.construction.resource", icon = "fa fa-database"),
        containerPath = "empty")
public class ConstructionResourceStepPanel<AR extends AbstractRoleType>
        extends SingleTileWizardStepPanel<ResourceType, FocusDetailsModels<AR>, AssignmentType> {

    @Serial private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(ConstructionResourceStepPanel.class);

    public static final String PANEL_TYPE = "arw-construction-resource";
    private static final String ID_LIFECYCLE_STATE = "lifecycleState";

    private LoadableDetachableModel<PrismPropertyWrapper<String>> lifecycleStateModel;

    public ConstructionResourceStepPanel(
            FocusDetailsModels<AR> model, IModel<PrismContainerValueWrapper<AssignmentType>> valueModel) {
        super(model, valueModel);
    }

    @Override
    protected Fragment createFragment(String id) {
        Fragment fragment = super.createFragment(id);
        addLifeCycleStatePanel(fragment);
        return fragment;
    }

    private void addLifeCycleStatePanel(Fragment fragment) {
        initLifeCycleStatePanel();

        LifecycleStatePanel panel = new LifecycleStatePanel(ID_LIFECYCLE_STATE, lifecycleStateModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String getLifecyclePanelStyle() {
                return "form-item-parent construction-lifecycle-state";
            }
        };
        panel.setOutputMarkupId(true);
        fragment.add(panel);
    }

    private void initLifeCycleStatePanel() {
        lifecycleStateModel = new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected PrismPropertyWrapper<String> load() {
                try {
                    PrismContainerValueWrapper<AssignmentType> constructionWrapper = getValueModel().getObject();
                    return constructionWrapper.findProperty(AssignmentType.F_LIFECYCLE_STATE);
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't find lifecycle state value.");
                }
                return null;
            }
        };
    }

    @Override
    protected boolean isTogglePanelVisible() {
        return true;
    }

    @Override
    protected ItemPath getPathForValueContainer() {
        return RoleType.F_INDUCEMENT;
    }

    @Override
    protected ItemPath getPathForTargetReference() {
        return ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF);
    }

    @Override
    protected ObjectQuery getCustomQuery() {
        return PrismContext.get().queryFor(ResourceType.class)
                .item(ResourceType.F_TEMPLATE).isNull().or().item(ResourceType.F_TEMPLATE).eq(false)
                .build();
    }

    @Override
    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected Class<ResourceType> getType() {
        return ResourceType.class;
    }

    protected PrismContainerValue<AssignmentType> createNewValue(PrismContainerWrapper<AssignmentType> parent) {
        PrismContainerValue<AssignmentType> newValue = super.createNewValue(parent);
        newValue.asContainerable().beginConstruction();
        return newValue;
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    protected String getIcon() {
        return "fa fa-database";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageRole.wizard.step.construction.resource");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageRole.wizard.step.construction.resource.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageRole.wizard.step.construction.resource.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    protected boolean isMandatory() {
        return true;
    }

    @Override
    protected <C extends Containerable> void performSelectedTile(String oid, QName typeName, PrismContainerValueWrapper<C> value) {
        super.performSelectedTile(oid, typeName, value);

        try {
            PrismContainerWrapper constructionWrapper = value.findContainer(AssignmentType.F_CONSTRUCTION);

            if (constructionWrapper.getValue() instanceof ConstructionValueWrapper) {
                ((ConstructionValueWrapper) constructionWrapper.getValue()).setResourceOid(oid);
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find construction wrapper.");
        }
    }

    @Override
    protected List<IColumn<SelectableBean<ResourceType>, String>> createColumns() {
        List<IColumn<SelectableBean<ResourceType>, String>> columns = new ArrayList<>();

        columns.add(ColumnUtils.createIconColumn(getPageBase()));

        columns.add(new AbstractColumn<>(createStringResource("ObjectType.name")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ResourceType>>> item, String id, IModel<SelectableBean<ResourceType>> row) {
                item.add(AttributeAppender.append("class", "align-middle"));
                item.add(new Label(id,
                        () -> WebComponentUtil.getDisplayNameOrName(row.getObject().getValue().asPrismObject())));
            }
        });

        columns.add(new PropertyColumn(createStringResource("ObjectType.description"), "value.description"));

        return columns;
    }

    @Override
    protected boolean isDefaultViewTile() {
        return false;
    }
}
