/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.prism;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageAdminLTE;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FormTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractFormItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormAuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormFieldGroupType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class DynamicFormPanel<O extends ObjectType> extends BasePanel<PrismObjectWrapper<O>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(DynamicFormPanel.class);

    private static final String DOT_CLASS = DynamicFormPanel.class.getName() + ".";

    private static final String ID_FORM_FIELDS = "formFields";

    private LoadableModel<PrismObjectWrapper<O>> wrapperModel;
    private FormType form;

    public DynamicFormPanel(String id, final IModel<O> model, String formOid, Form<?> mainForm,
            Task task, final PageAdminLTE parentPage, boolean enforceRequiredFields) {
        this(id, (PrismObject<O>) model.getObject().asPrismObject(), formOid, mainForm, task, parentPage, enforceRequiredFields);
    }

    public DynamicFormPanel(String id, final PrismObject<O> prismObject, String formOid, Form<?> mainForm,
            Task task, final PageAdminLTE parentPage, boolean enforceRequiredFields) {
        super(id);
        initialize(prismObject, formOid, mainForm, task, parentPage, enforceRequiredFields);
    }

    public DynamicFormPanel(String id, final QName objectType, String formOid, Form<?> mainForm,
            Task task, final PageAdminLTE parentPage, boolean enforceRequiredFields) {
        super(id);
        PrismObject<O> prismObject = instantiateObject(objectType, parentPage);
        initialize(prismObject, formOid, mainForm, task, parentPage, enforceRequiredFields);
    }

    private PrismObject<O> instantiateObject(QName objectType, PageAdminLTE parentPage) {
        PrismObjectDefinition<O> objectDef = parentPage.getPrismContext().getSchemaRegistry()
                .findObjectDefinitionByType(objectType);
        PrismObject<O> prismObject;
        try {
            prismObject = parentPage.getPrismContext().createObject((Class<O>)WebComponentUtil.qnameToClass(parentPage.getPrismContext(), objectType));
//            parentPage.getPrismContext().adopt(prismObject);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Could not initialize model for forgot password", e);
            throw new RestartResponseException(parentPage);
        }
        return prismObject;
    }

    private void initialize(final PrismObject<O> prismObject, String formOid, Form<?> mainForm,
            final Task task, final PageAdminLTE parentPage, boolean enforceRequiredFields) {

        if (prismObject == null) {
            getSession().error(getString("DynamicFormPanel.object.must.not.be.null"));
            throw new RestartResponseException(parentPage);
        }

//        setParent(parentPage);
        form = loadForm(formOid, task, parentPage);
        if (form == null || form.getFormDefinition() == null) {
            LOGGER.debug("No form or form definition; form OID = {}", formOid);
            add(new Label(ID_FORM_FIELDS));            // to avoid wicket exceptions
            return;
        }

        PrismObjectWrapperFactory<O> factory = parentPage.findObjectWrapperFactory(prismObject.getDefinition());
        PrismObjectWrapper<O> objectWrapper = createObjectWrapper(factory, task, prismObject, enforceRequiredFields);
        wrapperModel = LoadableModel.create(() -> objectWrapper, true);
        initLayout(mainForm, parentPage);
    }

    private PrismObjectWrapper<O> createObjectWrapper(PrismObjectWrapperFactory<O> factory, Task task, PrismObject<O> prismObject,
            boolean enforceRequiredFields) {

        FormAuthorizationType formAuthorization = form.getFormDefinition().getAuthorization();
        AuthorizationPhaseType authorizationPhase = formAuthorization != null && formAuthorization.getPhase() != null
                ? formAuthorization.getPhase()
                : AuthorizationPhaseType.REQUEST;

        OperationResult result = task.getResult();
        WrapperContext context = new WrapperContext(task, result);
        context.setShowEmpty(true);
        context.setCreateIfEmpty(true);
        context.setAuthzPhase(authorizationPhase);
        //TODO: enforce required fields???? what is it?
        PrismObjectWrapper<O> objectWrapper = null;
        try {
            objectWrapper = factory.createObjectWrapper(prismObject, prismObject.getOid() == null ? ItemStatus.ADDED : ItemStatus.NOT_CHANGED,
                    context);
            result.recordSuccess();
        } catch (SchemaException e) {
            result.recordFatalError(createStringResource("DynamicFormPanel.message.createObjectWrapper.fatalError", e.getMessage()).getString());
            getPageAdminLTE().showResult(result);

        }
        return objectWrapper;
    }

    @Override
    public IModel<PrismObjectWrapper<O>> getModel() {
        return wrapperModel;
    }

    private void initLayout(Form<?> mainForm, PageAdminLTE parenPage) {
        DynamicFieldGroupPanel<O> formFields = new DynamicFieldGroupPanel<O>(ID_FORM_FIELDS, getModel(),
                form.getFormDefinition(), mainForm, parenPage);
        formFields.setOutputMarkupId(true);
        add(formFields);
    }

    private FormType loadForm(String formOid, Task task, PageAdminLTE parentPage) {
        OperationResult result = new OperationResult("some some operation");
        return asObjectable(WebModelServiceUtils.loadObject(FormType.class, formOid, null, false,
                parentPage, task, result));
    }

    public ObjectDelta<O> getObjectDelta() throws SchemaException {
        return wrapperModel.getObject().getObjectDelta();
    }

    public boolean checkRequiredFields(PageAdminLTE pageBase) {
        return getFormFields().checkRequiredFields();
    }

    @SuppressWarnings("unchecked")
    public DynamicFieldGroupPanel<O> getFormFields() {
        return (DynamicFieldGroupPanel<O>) get(ID_FORM_FIELDS);
    }

    public PrismObject<O> getObject() throws SchemaException {
        ObjectDelta<O> delta = wrapperModel.getObject().getObjectDelta();
        if (delta != null && delta.isAdd()) {
            return delta.getObjectToAdd();
        }
        return wrapperModel.getObject().getObject();
    }

    public List<ItemPath> getChangedItems() {
        DynamicFieldGroupPanel<O> formFields = (DynamicFieldGroupPanel<O>) get(ID_FORM_FIELDS);
        List<AbstractFormItemType> items = formFields.getFormItems();
        List<ItemPath> paths = new ArrayList<>();
        collectItemPaths(items, paths);
        return paths;
    }

    private void collectItemPaths(List<AbstractFormItemType> items, List<ItemPath> paths) {
        for (AbstractFormItemType aItem : items) {
            ItemPath itemPath = GuiImplUtil.getItemPath(aItem);
            if (itemPath != null) {
                paths.add(itemPath);
            }
            if (aItem instanceof FormFieldGroupType) {
                collectItemPaths(FormTypeUtil.getFormItems(((FormFieldGroupType) aItem).getFormItems()), paths);
            }
        }
    }
}
