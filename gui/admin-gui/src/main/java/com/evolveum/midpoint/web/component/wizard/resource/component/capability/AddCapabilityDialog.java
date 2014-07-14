/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.wizard.resource.dto.CapabilityDto;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class AddCapabilityDialog extends ModalWindow{

    private static final String ID_TABLE = "table";
    private static final String ID_CANCEL = "cancelButton";
    private static final String ID_ADD = "addButton";

    private static final String DEFAULT_SORTABLE_PROPERTY = null;

    private boolean initialized;
    private IModel<CapabilityStepDto> model;

    public AddCapabilityDialog(String id, final IModel<CapabilityStepDto> capabilityModel){
        super(id);

        model = new LoadableModel<CapabilityStepDto>() {
            @Override
            protected CapabilityStepDto load() {
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

    private CapabilityStepDto loadModel(IModel<CapabilityStepDto> capabilityModel){
        CapabilityStepDto dto = new CapabilityStepDto();
        List<CapabilityDto> capabilityList = new ArrayList<>();
        List<Class<? extends CapabilityType>> capabilityClassList = new ArrayList<>();

        for(CapabilityDto cap: capabilityModel.getObject().getCapabilities()){
            capabilityClassList.add(cap.getCapability().getClass());
        }

        for(Class<? extends CapabilityType> cap: CapabilityPanel.capabilities){
            if(!capabilityClassList.contains(cap)){
                capabilityList.add(createCapabilityDto(cap));
            }
        }

        dto.setCapabilities(capabilityList);

        return dto;
    }

    private CapabilityDto createCapabilityDto(Class<? extends CapabilityType> capabilityClass){
        if(capabilityClass.equals(ActivationCapabilityType.class)){
            return new CapabilityDto(new ActivationCapabilityType(), "Activation", true);
        } else if(capabilityClass.equals(ScriptCapabilityType.class)){
            return new CapabilityDto(new ScriptCapabilityType(), "Script", true);
        } else if(capabilityClass.equals(CredentialsCapabilityType.class)){
            return new CapabilityDto(new CredentialsCapabilityType(), "Credentials", true);
        } else if(capabilityClass.equals(DeleteCapabilityType.class)){
            return new CapabilityDto(new DeleteCapabilityType(), "Delete", true);
        } else if(capabilityClass.equals(ReadCapabilityType.class)){
            return new CapabilityDto(new ReadCapabilityType(), "Read", true);
        } else if(capabilityClass.equals(DeleteCapabilityType.class)){
            return new CapabilityDto(new CreateCapabilityType(), "Create", true);
        } else if(capabilityClass.equals(UpdateCapabilityType.class)){
            return new CapabilityDto(new UpdateCapabilityType(), "Update", true);
        } else if(capabilityClass.equals(TestConnectionCapabilityType.class)){
            return new CapabilityDto(new TestConnectionCapabilityType(), "Test Connection", true);
        } else {  //if(capabilityClass.equals(LiveSyncCapabilityType.class)){
            return new CapabilityDto(new LiveSyncCapabilityType(), "Live Sync", true);
        }
    }

    private ListDataProvider<CapabilityDto> createProvider(){
        return new ListDataProvider<>(this,
                new PropertyModel<List<CapabilityDto>>(model, CapabilityStepDto.F_CAPABILITIES));
    }

    @Override
    protected void onBeforeRender(){
        super.onBeforeRender();

        if(initialized){
            return;
        }

        initLayout((WebMarkupContainer) get(getContentId()));
        initialized = true;
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }

    private void initLayout(WebMarkupContainer container){
        List<IColumn<SelectableBean<CapabilityDto>, String>> columns = initColumns();

        TablePanel table = new TablePanel<>(ID_TABLE, createProvider(), columns);
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

    private List<IColumn<SelectableBean<CapabilityDto>, String>> initColumns(){
        List<IColumn<SelectableBean<CapabilityDto>, String>> columns = new ArrayList<>();

        IColumn column = new CheckBoxHeaderColumn<CapabilityDto>();
        columns.add(column);

        column = new PropertyColumn(createStringResource("addCapabilityDialog.column.name"), CapabilityDto.F_VALUE);
        columns.add(column);

        return columns;
    }

    public void updateTable(AjaxRequestTarget target, IModel<CapabilityStepDto> selected){
        model.setObject(loadModel(selected));
    }

    public TablePanel getTable(){
        return (TablePanel)get(getContentId()+":"+ID_TABLE);
    }

    public String getSortableProperty(){
        return DEFAULT_SORTABLE_PROPERTY;
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        close(target);
    }

    protected List<CapabilityDto> getSelectedData(){
        DataTable dataTable = getTable().getDataTable();
        ListDataProvider provider = (ListDataProvider)dataTable.getDataProvider();

        List<CapabilityDto> data = (List<CapabilityDto>)provider.getAvailableData();
        List<CapabilityDto> selected = new ArrayList<>();

        for(CapabilityDto cap: data){
            if(cap.isSelected()){
                selected.add(cap);
            }
        }

        return selected;
    }

    public BaseSortableDataProvider getDataProviderSearchProperty(){
        return null;
    }

    protected void chooseOperationPerformed(AjaxRequestTarget target, CapabilityDto object){}

    protected void addPerformed(AjaxRequestTarget target){}
}
