/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.TextPanel;

import com.evolveum.midpoint.web.component.util.EnableBehaviour;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.SchemaDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.ComplexTypeDefinitionDto;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.DefinitionDto;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.EnumerationTypeDefinitionDto;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;

import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.Serializable;

@PanelType(name = "schemaPanel")
@PanelInstance(identifier = "schemaPanel",
        applicableForType = SchemaType.class,
        display = @PanelDisplay(label = "SchemaPanelOld.title", icon = GuiStyleConstants.CLASS_CIRCLE_FULL, order = 25))
public class SchemaPanelOld<T extends DefinitionDto> extends AbstractObjectMainPanel<SchemaType, SchemaDetailsModel> {

    private static final String ID_DEFS = "defs";
    private static final String ID_FORM = "form";

    public SchemaPanelOld(String id, SchemaDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }


    private class NewDefinitionType implements Serializable {

        String name;

        public String getName() {
            return name;
        }
    }


    @Override
    protected void initLayout() {

        MidpointForm form = new MidpointForm(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        TextPanel<String> namespace = new TextPanel<>("namespace", getObjectDetailsModels().getNamespaceModel());
        namespace.add(new EnableBehaviour(() -> !getObjectDetailsModels().isEditObject()));
        namespace.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        namespace.setOutputMarkupId(true);
        form.add(namespace);


        NewDefinitionType newDefinitionType= new NewDefinitionType();
        TextPanel<String> complexTypeName = new TextPanel<>("complexTypeName", new PropertyModel<>(newDefinitionType, "name"));
        complexTypeName.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        complexTypeName.add(new VisibleBehaviour(() -> !getObjectDetailsModels().isEditObject()));
        complexTypeName.setOutputMarkupId(true);
        form.add(complexTypeName);

        AjaxIconButton createDef = new AjaxIconButton("createDef", Model.of("fa fa-plus"), createStringResource("SchemaPanel.createDef")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                String namespace = getObjectDetailsModels().getNamespaceModel().getObject();
                String name = newDefinitionType.getName();


                PrismSchemaImpl newSchema = new PrismSchemaImpl(namespace);
                newSchema.setRuntime(true);

                ComplexTypeDefinition def = PrismContext.get().definitionFactory().newComplexTypeDefinition(new QName(namespace, name));
                newSchema.add(def);

                Document newXsdSchemaDoc = null;
                try {
                    newXsdSchemaDoc = newSchema.serializeToXsd();
                } catch (SchemaException e) {
                    //TODO handle exception
                }
                Element newXsdSchema = DOMUtil.getFirstChildElement(newXsdSchemaDoc);

                SchemaType extensionType = new SchemaType();
                SchemaDefinitionType newDef = new SchemaDefinitionType();
                newDef.setSchema(newXsdSchema);
                extensionType.setDefinition(newDef);

                getObjectDetailsModels().reset();
                getObjectDetailsModels().reloadPrismObjectModel(extensionType.asPrismObject());

                target.add(SchemaPanelOld.this);
            }
        };
        createDef.setOutputMarkupId(true);
        form.add(createDef);


        ListView<T> list = new ListView<>(ID_DEFS, getObjectDetailsModels().getSchemaModel()) {
            @Override
            protected void populateItem(ListItem<T> item) {
                if (item.getModelObject() instanceof ComplexTypeDefinitionDto) {
                    ComplexTypeDefinitionPanel panel = new ComplexTypeDefinitionPanel("definition", (IModel<ComplexTypeDefinitionDto>) item.getModel());
                    item.add(panel);
                } else if (item.getModelObject() instanceof EnumerationTypeDefinitionDto) {
                    EnumerationTypeDefinitionPanel panel = new EnumerationTypeDefinitionPanel("definition", (IModel<EnumerationTypeDefinitionDto>) item.getModel());
                    item.add(panel);
                } else {
                    DefinitionPanel panel = new DefinitionPanel("definition", item.getModel());
                    item.add(panel);
                }
            }

        };
        add(list);
    }

}
