/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.input;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.lang.NonNull;

import com.evolveum.midpoint.common.MimeTypeUtil;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.input.DateTimePickerPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;

public class AnyTypePanel extends InputPanel {

    private static final String ID_TYPE = "type";

    private static final String ID_INPUT = "input";

    private final IModel<Object> model;

    private final IModel<AnyTypeType> typeModel;

    public AnyTypePanel(String id, IModel<Object> model) {
        super(id);

        this.model = model;
        this.typeModel = createTypeModel(model);

        setOutputMarkupId(true);

        initLayout();
    }

    private IModel<AnyTypeType> createTypeModel(IModel<Object> model) {
        return new LoadableModel<>(false) {

            @Override
            protected AnyTypeType load() {
                Object obj = model.getObject();

                return AnyTypeType.fromObject(obj);
            }
        };
    }

    private void initLayout() {
        DropDownChoice<AnyTypeType> type = new DropDownChoice<>(
                ID_TYPE, typeModel, WebComponentUtil.createReadonlyModelFromEnum(AnyTypeType.class),
                new EnumChoiceRenderer<>(this));
        type.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updatePanelType(target);
            }
        });
        add(type);

        InputPanel input = createInputPanel(ID_INPUT);
        add(input);
    }

    private void updatePanelType(AjaxRequestTarget target) {
        model.setObject(null);

        InputPanel input = createInputPanel(ID_INPUT);
        replace(input);

        target.add(this);
    }

    private InputPanel createInputPanel(String id) {
        AnyTypeType type = typeModel.getObject();

        if (type == null) {
            TextPanel<?> input = new TextPanel<>(id, Model.of());
            input.getBaseFormComponent().add(new EnableBehaviour(() -> false));
            return input;
        }

        switch (type) {
            case STRING:
                return new TextAreaPanel<>(id, model);
            case BOOLEAN:
                return new TriStateComboPanel(id, new TypedModel<>(Boolean.class, model));
            case BYTE:
                TypedModel<byte[]> bytes = new TypedModel<>(byte[].class, model);

                return new UploadDownloadPanel(id, false) {

                    @Override
                    public InputStream getInputStream() {
                        byte[] content = bytes.getObject();
                        if (content == null) {
                            content = new byte[0];
                        }
                        return new ByteArrayInputStream(content);
                    }

                    @Override
                    public String getDownloadFileName() {
                        String fileName = "data";

                        String mimetype = getDownloadContentType();
                        if (StringUtils.isNotEmpty(mimetype)) {
                            String extension = MimeTypeUtil.getExtension(mimetype);
                            if (extension != null) {
                                fileName += extension;
                            }
                        }

                        return fileName;
                    }

                    @Override
                    public void updateValue(byte[] file) {
                        bytes.setObject(file);
                    }

                    @Override
                    public void uploadFileFailed(AjaxRequestTarget target) {
                        super.uploadFileFailed(target);

                        target.add(getPageBase().getFeedbackPanel());
                    }
                };
            case DATE_TIME:
                return DateTimePickerPanel.createByXMLGregorianCalendarModel(id, new TypedModel<>(XMLGregorianCalendar.class, model));
            default:
                TextPanel<?> input = new TextPanel<>(id, model);
                FormComponent<?> fc = input.getBaseFormComponent();
                fc.setType(type.type);

                return input;
        }
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        InputPanel input = (InputPanel) get(ID_INPUT);
        return input.getBaseFormComponent();
    }

    private static class TypedModel<T> implements IModel<T> {

        private final Class<T> type;

        private final IModel<Object> model;

        public TypedModel(@NonNull Class<T> type, @NonNull IModel<Object> model) {
            this.type = type;
            this.model = model;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T getObject() {
            Object object = model.getObject();
            if (object == null) {
                return null;
            }

            if (type.isAssignableFrom(object.getClass())) {
                return (T) object;
            }

            throw new IllegalArgumentException("Object is not of type " + type);
        }

        @Override
        public void setObject(T object) {
            model.setObject(object);
        }
    }
}
