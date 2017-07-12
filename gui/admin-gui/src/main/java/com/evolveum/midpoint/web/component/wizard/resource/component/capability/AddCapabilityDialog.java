/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.wizard.resource.component.capability;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.wizard.resource.CapabilityStep;
import com.evolveum.midpoint.web.component.wizard.resource.dto.Capability;
import com.evolveum.midpoint.web.component.wizard.resource.dto.CapabilityDto;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class AddCapabilityDialog extends ModalWindow {

    private static final String ID_TABLE = "table";
    private static final String ID_CANCEL = "cancelButton";
    private static final String ID_ADD = "addButton";

    private static final String DEFAULT_SORTABLE_PROPERTY = null;

    private boolean initialized;
    private IModel<List<CapabilityDto<CapabilityType>>> model;

    protected AddCapabilityDialog(String id, final IModel<CapabilityStepDto> capabilityModel) {
        super(id);

        model = new LoadableModel<List<CapabilityDto<CapabilityType>>>() {
            @Override
            protected List<CapabilityDto<CapabilityType>> load() {
                return loadModel(capabilityModel);
            }
        };

        setTitle(createStringResource("addCapabilityDialog.title"));
        showUnloadConfirmation(false);
        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        setCookieName(AddCapabilityDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
        setResizable(false);
        setInitialWidth(500);
        setInitialHeight(500);
        setWidthUnit("px");

        WebMarkupContainer content = new WebMarkupContainer(getContentId());
        content.setOutputMarkupId(true);
        setContent(content);
    }

    private List<CapabilityDto<CapabilityType>> loadModel(IModel<CapabilityStepDto> capabilityModel) {

        List<Class<? extends CapabilityType>> existingCapabilityClasses = new ArrayList<>();
        for (CapabilityDto cap: capabilityModel.getObject().getCapabilities()) {
            existingCapabilityClasses.add(cap.getCapability().getClass());
        }

		List<CapabilityDto<CapabilityType>> rv = new ArrayList<>();
		for (Capability supportedCapability : Capability.values()) {
            if (!existingCapabilityClasses.contains(supportedCapability.getClazz())) {
                rv.add(new CapabilityDto<>(CapabilityStep.fillDefaults(supportedCapability.newInstance()), false));		// 'among natives' doesn't matter here
            }
        }

		return rv;
    }

    private ListDataProvider<CapabilityDto<CapabilityType>> createProvider() {
        return new ListDataProvider<>(this, model);
    }

    @Override
    protected void onBeforeRender(){
        super.onBeforeRender();

        if (initialized) {
            return;
        }

        initLayout((WebMarkupContainer) get(getContentId()));
        initialized = true;
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
    	return PageBase.createStringResourceStatic(this, resourceKey, objects);
    }

    private void initLayout(WebMarkupContainer container){
        List<IColumn<CapabilityDto<CapabilityType>, String>> columns = initColumns();

        TablePanel<CapabilityDto<CapabilityType>> table = new TablePanel<>(ID_TABLE, createProvider(), columns);
        table.setOutputMarkupId(true);
        table.setShowPaging(false);
        container.add(table);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL,
                createStringResource("addCapabilityDialog.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                cancelPerformed(ajaxRequestTarget);
            }
        };
        container.add(cancelButton);

        AjaxButton addButton = new AjaxButton(ID_ADD,
                createStringResource("addCapabilityDialog.button.Add")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target);
            }
        };
        container.add(addButton);

    }

    private List<IColumn<CapabilityDto<CapabilityType>, String>> initColumns(){
        List<IColumn<CapabilityDto<CapabilityType>, String>> columns = new ArrayList<>();

        IColumn<CapabilityDto<CapabilityType>, String> column = new CheckBoxHeaderColumn<>();
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("addCapabilityDialog.column.name"), CapabilityDto.F_DISPLAY_NAME);
        columns.add(column);

        return columns;
    }

    public void updateTable(AjaxRequestTarget target, IModel<CapabilityStepDto> selected){
        model.setObject(loadModel(selected));
    }

	@SuppressWarnings("unchecked")
    public TablePanel<CapabilityDto<CapabilityType>> getTable() {
        return (TablePanel<CapabilityDto<CapabilityType>>) get(getContentId()+":"+ID_TABLE);
    }

    public String getSortableProperty(){
        return DEFAULT_SORTABLE_PROPERTY;
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        close(target);
    }

    protected List<CapabilityDto<CapabilityType>> getSelectedData() {
        List<CapabilityDto<CapabilityType>> selected = new ArrayList<>();
        for (CapabilityDto<CapabilityType> cap: model.getObject()) {
            if (cap.isSelected()) {
                selected.add(cap);
            }
        }
        return selected;
    }

    public BaseSortableDataProvider getDataProviderSearchProperty(){
        return null;
    }

    protected void chooseOperationPerformed(AjaxRequestTarget target, CapabilityDto object) {}

    protected void addPerformed(AjaxRequestTarget target){}
}
