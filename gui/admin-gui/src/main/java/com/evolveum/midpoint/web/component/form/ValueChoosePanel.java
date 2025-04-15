/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * TODO: rename to ValueObjectChoicePanel, PrismValueObjectSelectorPanel or
 * something better
 */
public class ValueChoosePanel<R extends Referencable> extends BasePanel<R> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ValueChoosePanel.class);

    private static final String ID_TEXT_WRAPPER = "textWrapper";
    private static final String ID_TEXT = "text";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_EDIT = "edit";

    Popupable parentPopupableDialog = null;

    public ValueChoosePanel(String id, IModel<R> value) {
        super(id, value);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
        initParentPopupableDialog();
    }

    private void initLayout() {
        WebMarkupContainer textWrapper = new WebMarkupContainer(ID_TEXT_WRAPPER);

        textWrapper.setOutputMarkupId(true);

        Component text = createTextPanel(ID_TEXT);
        textWrapper.add(text);

        FeedbackAlerts feedback = new FeedbackAlerts(ID_FEEDBACK);
        feedback.setFilter(new ComponentFeedbackMessageFilter(text));
        textWrapper.add(feedback);

        AjaxLink<String> edit = new AjaxLink<String>(ID_EDIT) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                editValuePerformed(target);
            }
        };

        edit.add(new VisibleEnableBehaviour(this::isEditButtonVisible, this::isEditButtonEnabled));
        textWrapper.add(edit);
        add(textWrapper);

        initButtons();
    }

    private void initParentPopupableDialog() {
        parentPopupableDialog = findParent(Popupable.class);
    }

    protected Component createTextPanel(String id) {
        IModel<String> textModel = createTextModel();
        TextField<String> text = new TextField<>(id, textModel);
        text.add(new AjaxFormComponentUpdatingBehavior("blur") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
            }
        });
        text.add(AttributeAppender.append("title", textModel));
        text.setRequired(isRequired());
        text.setEnabled(true);
        return text;
    }

    protected boolean isEditButtonVisible() {
        return true;
    }

    protected boolean isEditButtonEnabled() {
        return true;
    }

    protected void replaceIfEmpty(ObjectType object) {
        ObjectReferenceType ort = ObjectTypeUtil.createObjectRef(object);
        getModel().setObject((R) ort);

    }

    protected ObjectQuery createChooseQuery() {
        ArrayList<String> oidList = new ArrayList<>();
        ObjectQuery query = PrismContext.get().queryFactory().createQuery();
        // TODO we should add to filter currently displayed value
        // not to be displayed on ObjectSelectionPanel instead of saved value

        if (oidList.isEmpty()) {
            ObjectFilter customFilter = createCustomFilter();
            if (customFilter != null) {
                query.addFilter(customFilter);
                return query;
            }

            return null;

        }

        ObjectFilter oidFilter = PrismContext.get().queryFactory().createInOid(oidList);
        query.setFilter(PrismContext.get().queryFactory().createNot(oidFilter));

        ObjectFilter customFilter = createCustomFilter();
        if (customFilter != null) {
            query.addFilter(customFilter);
        }

        return query;
    }

    protected boolean isRequired() {
        return false;
    }

    protected ObjectFilter createCustomFilter() {
        return null;
    }

    /**
     * @return css class for off-setting other values (not first, left to the
     * first there is a label)
     */
    protected String getOffsetClass() {
        return "col-md-offset-4";
    }

    protected IModel<String> createTextModel() {
        final IModel<R> model = getModel();
        return new IModel<>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                R prv = model.getObject();

                return prv == null ? null
                        : (prv.getTargetName() != null ? (WebComponentUtil.getTranslatedPolyString(prv.getTargetName())
                        + (prv.getType() != null ? ": " + prv.getType().getLocalPart() : ""))
                        : prv.getOid());
            }

            @Override
            public void setObject(String object) {
            }
        };
    }

    protected <O extends ObjectType> void editValuePerformed(AjaxRequestTarget target) {
        PageBase pageBase = getPageBase();
        List<QName> supportedTypes = getSupportedTypes();
        ObjectFilter filter = createChooseQuery() == null ? null
                : createChooseQuery().getFilter();
        if (CollectionUtils.isEmpty(supportedTypes)) {
            supportedTypes = ObjectTypeListUtil.createObjectTypeList();
        }
        Class<O> defaultType = getDefaultType(supportedTypes);
        ObjectBrowserPanel<O> objectBrowserPanel = new ObjectBrowserPanel<O>(
                pageBase.getMainPopupBodyId(), defaultType, supportedTypes, false, pageBase,
                filter) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSelectPerformed(AjaxRequestTarget target, O object) {
                ValueChoosePanel.this.choosePerformed(target, object);
                managePopupDialogContent(pageBase, target);
            }

            @Override
            protected void onClickCancelButton(AjaxRequestTarget ajaxRequestTarget) {
                managePopupDialogContent(pageBase, ajaxRequestTarget);
            }
        };

        if (parentPopupableDialog != null) {
            pageBase.replaceMainPopup(objectBrowserPanel, target);
        } else {
            pageBase.showMainPopup(objectBrowserPanel, target);
        }

    }

    protected Set<SearchItemType> getSpecialSearchItem() {
        return Collections.emptySet();
    }

    public List<QName> getSupportedTypes() {
        return ObjectTypeListUtil.createObjectTypeList();
    }

    protected <O extends ObjectType> Class<O> getDefaultType() {
        List<QName> supportedTypes = getSupportedTypes();
        if (CollectionUtils.isEmpty(supportedTypes)) {
            supportedTypes = ObjectTypeListUtil.createObjectTypeList();
        }
        return getDefaultType(supportedTypes);
    }

    protected <O extends ObjectType> Class<O> getDefaultType(List<QName> supportedTypes) {
        return (Class<O>) WebComponentUtil.qnameToClass(supportedTypes.iterator().next());
    }

    /*
     * TODO - this method contains check, if chosen object already is not in
     * selected values array This is a temporary solution until we well be able
     * to create "already-chosen" query
     */
    protected <O extends ObjectType> void choosePerformed(AjaxRequestTarget target, O object) {
        choosePerformedHook(target, object);

        if (isObjectUnique(object)) {
            replaceIfEmpty(object);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("New object instance has been added to the model.");
        }
        target.add(getTextWrapperComponent());
    }

    public WebMarkupContainer getTextWrapperComponent() {
        return (WebMarkupContainer) get(ID_TEXT_WRAPPER);
    }

    protected void initButtons() {
    }

    protected <O extends ObjectType> boolean isObjectUnique(O object) {
        Referencable old = getModelObject();
        if (old == null) {
            return true;
        }
        return !MiscUtil.equals(old.getOid(), object.getOid());
    }

    /**
     * A custom code in form of hook that can be run on event of choosing new
     * object with this chooser component
     */
    protected <O extends ObjectType> void choosePerformedHook(AjaxRequestTarget target, O object) {
    }

    public FormComponent<String> getBaseFormComponent() {
        return (FormComponent<String>) getBaseComponent();
    }

    protected final Component getBaseComponent() {
        return getTextWrapperComponent().get(ID_TEXT);
    }

    public AjaxLink getEditButton() {
        return (AjaxLink)getTextWrapperComponent().get(ID_EDIT);
    }

    protected void reloadPageFeedbackPanel(AjaxRequestTarget target) {
        target.add(getPageBase().getFeedbackPanel());
    }

    //fix for open project ticket 9588
    //if ValueChoosePanel is used in popup, ObjectBrowserPanel is opened in the second popup
    //after the object is selected in ObjectBrowserPanel, the second popup is closed and the first one is shown again
    private void managePopupDialogContent(PageBase pageBase, AjaxRequestTarget target) {
        if (parentPopupableDialog != null) {
            pageBase.replaceMainPopup(parentPopupableDialog, target);
        } else {
            pageBase.hideMainPopup(target);
        }
    }

    protected boolean hasParentPopupableDialog() {
        return parentPopupableDialog != null;
    }
}
