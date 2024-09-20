/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.schema.util.PrismSchemaTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/schemas", matchUrlForSecurity = "/admin/schemas")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SCHEMAS_ALL_URL,
                        label = "PageSchemas.auth.schemasAll.label",
                        description = "PageSchemas.auth.schemasAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SCHEMAS_URL,
                        label = "PageSchemas.auth.schemas.label",
                        description = "PageSchemas.auth.schemas.description"),
        })
@CollectionInstance(identifier = "allSchemas", applicableForType = SchemaType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.schemas.list", singularLabel = "ObjectType.schema", icon = GuiStyleConstants.CLASS_OBJECT_USER_ICON))
public class PageSchemas extends PageAdmin {

    private static final long serialVersionUID = 1L;
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    public PageSchemas() {
        super();
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new MidpointForm(ID_MAIN_FORM);
        add(mainForm);

        MainObjectListPanel<SchemaType> table = new MainObjectListPanel<>(ID_TABLE, SchemaType.class) {

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.PAGE_SCHEMAS_TABLE;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
               return new ArrayList<>();
            }

            @Override
            protected String getNothingSelectedMessage() {
                return getString("pageUsers.message.nothingSelected");
            }

            @Override
            protected String getConfirmMessageKeyForMultiObject() {
                return "pageUsers.message.confirmationMessageForMultipleObject";
            }

            @Override
            protected String getConfirmMessageKeyForSingleObject() {
                return "pageUsers.message.confirmationMessageForSingleObject";
            }

            @Override
            protected List<IColumn<SelectableBean<SchemaType>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<SchemaType>, String>> columns = new ArrayList<>();
                columns.add(new PropertyColumn<>(createStringResource("PrismSchemaType.namespace"), "value." + SchemaType.F_DEFINITION.getLocalPart()){
                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<SchemaType>> rowModel) {
                        return () -> {
                            PrismSchemaType prismSchema = getXsdSchema(rowModel.getObject(), super.getDataModel(rowModel));

                            if (prismSchema == null) {
                                return null;
                            } else {
                                return prismSchema.getNamespace();
                            }
                        };
                    }
                });
                columns.add(new PropertyColumn<>(createStringResource("PrismSchemaType.defaultPrefix"), "value." + SchemaType.F_DEFINITION.getLocalPart()){
                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<SchemaType>> rowModel) {
                        return () -> {
                            PrismSchemaType prismSchema = getXsdSchema(rowModel.getObject(), super.getDataModel(rowModel));

                            if (prismSchema == null) {
                                return null;
                            } else {
                                return prismSchema.getDefaultPrefix();
                            }
                        };
                    }
                });
                return columns;
            }

            @Override
            protected boolean isDuplicationSupported() {
                return false;
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    private PrismSchemaType getXsdSchema(SelectableBean<SchemaType> schema, IModel<?> dataModel) {
        if (schema == null || schema.getValue() == null) {
            return null;
        }

        SchemaDefinitionType xsdDefinition = (SchemaDefinitionType) dataModel.getObject();
        if (xsdDefinition == null) {
            return null;
        }

        PrismSchemaType prismSchema = null;

        try {
            prismSchema = PrismSchemaTypeUtil.convertToPrismSchemaType(xsdDefinition, schema.getValue().getLifecycleState());
        } catch (SchemaException ignored) {
        }
        return prismSchema;
    }
}
