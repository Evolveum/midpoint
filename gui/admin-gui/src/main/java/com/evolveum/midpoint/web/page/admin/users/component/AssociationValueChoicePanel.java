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
package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.ObjectSelectionPage;
import com.evolveum.midpoint.web.page.admin.configuration.component.ObjectSelectionPanel;
import com.evolveum.midpoint.web.page.admin.roles.component.UserOrgReferenceChoosePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Kate Honchar
 *
 */
    //TODO the class is created as a copy of ValueChoosePanel but
    //with the possibility to work with PrismPropertyValue objects
    // (for now ValueChoosePanel works only with PrismReferenceValue);
    //in future some super class is to be created to union the common
    // functionality of these 2 classes

public class AssociationValueChoicePanel<C extends ObjectType> extends BasePanel<PrismContainerValue<ShadowAssociationType>> {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(AssociationValueChoicePanel.class);

    private static final String ID_TEXT_WRAPPER = "textWrapper";
    private static final String ID_TEXT = "text";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_EDIT = "edit";
    protected static final String MODAL_ID_OBJECT_SELECTION_POPUP = "objectSelectionPopup";

    private IModel<ValueWrapper<PrismContainerValue<ShadowAssociationType>>> model;
    private ObjectQuery query = null;
    private RefinedObjectClassDefinition assocTargetDef;

    public AssociationValueChoicePanel(String id, IModel<ValueWrapper<PrismContainerValue<ShadowAssociationType>>> model,
    		List<PrismPropertyValue> values, boolean required, Class<C> type, ObjectQuery query,
    		RefinedObjectClassDefinition assocTargetDef) {
        super(id, (IModel)new PropertyModel<>(model, "value"));
        this.model = model;
        this.query = query;
        this.assocTargetDef = assocTargetDef;
        setOutputMarkupId(true);
        initLayout((IModel)new PropertyModel<>(model, "value"), values, required, type);
    }

    private void initLayout(final IModel<PrismContainerValue<ShadowAssociationType>> value,
    		final List<PrismPropertyValue> values, final boolean required, Class<C> type) {


        WebMarkupContainer textWrapper = new WebMarkupContainer(ID_TEXT_WRAPPER);

        textWrapper.setOutputMarkupId(true);

        TextField<String> text = new TextField<>(ID_TEXT, createTextModel(value));
        text.add(new AjaxFormComponentUpdatingBehavior("blur") {
			private static final long serialVersionUID = 1L;

			@Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
            }
        });
        text.setRequired(required);
        text.setEnabled(false);
        textWrapper.add(text);

        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ComponentFeedbackMessageFilter(text));
        feedback.setFilter(new ComponentFeedbackMessageFilter(text));
        textWrapper.add(feedback);

        AjaxLink edit = new AjaxLink(ID_EDIT) {
			private static final long serialVersionUID = 1L;

			@Override
            public void onClick(AjaxRequestTarget target) {
                editValuePerformed(target);
            }
        };
        edit.add(new VisibleEnableBehaviour() {
        	private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return model.getObject().isEmpty();
            }
        });
        textWrapper.add(edit);
        add(textWrapper);

        initDialog(type, values);

    }

    protected void replace(Object object) {
    	//TODO  be careful , non systematic hack
        ShadowType shadowType = (ShadowType) object;
        ValueWrapper<PrismContainerValue<ShadowAssociationType>> oldCValueWrapper = model.getObject();
        AssociationWrapper associationWrapper = (AssociationWrapper) oldCValueWrapper.getItem();
        PrismContainer<ShadowAssociationType> container = (PrismContainer<ShadowAssociationType>) associationWrapper.getItem();
        container.remove((PrismContainerValue)oldCValueWrapper.getValue());
        PrismContainerValue<ShadowAssociationType> newValue = container.createNewValue();
        ShadowAssociationType assocType = newValue.asContainerable();
        ObjectReferenceType shadowRef = new ObjectReferenceType();
        shadowRef.setOid(shadowType.getOid());
        shadowRef.asReferenceValue().setObject(shadowType.asPrismObject());
		assocType.setShadowRef(shadowRef);
		assocType.setName(model.getObject().getItem().getName());
        getModel().setObject(newValue);
        LOGGER.trace("Replaced value of {} with {} ({})", this, newValue, assocType);
    }

    protected void initDialog(final Class<C> type, List<PrismPropertyValue> values) {
    	LOGGER.debug("Initializing dialog for type {}", type);
        if (FocusType.class.equals(type)){
            initUserOrgDialog();
        } else {
            initGenericDialog(type, values);
        }
    }

    // for ModalWindow treatment see comments in ChooseTypePanel
    private void initGenericDialog(final Class<C> type, final List<PrismPropertyValue> values) {
        final ModalWindow dialog = new ModalWindow(MODAL_ID_OBJECT_SELECTION_POPUP);

        ObjectSelectionPanel.Context context = new ObjectSelectionPanel.Context(this) {
        	private static final long serialVersionUID = 1L;

            // See analogous discussion in ChooseTypePanel
            public AssociationValueChoicePanel getRealParent() {
                return WebComponentUtil.theSameForPage(AssociationValueChoicePanel.this, getCallingPageReference());
            }

            @Override
            public void chooseOperationPerformed(AjaxRequestTarget target, ObjectType object) {
                getRealParent().choosePerformed(target, object);
            }

            @Override
            public Collection<SelectorOptions<GetOperationOptions>> getDataProviderOptions(){
                return getAssociationsSearchOptions();
            }

            @Override
            public ObjectQuery getDataProviderQuery() {
                    return query;
            }

            @Override
            public boolean isSearchEnabled() {
                //TODO don't commit
                return false;
            }

            @Override
            public Class<? extends ObjectType> getObjectTypeClass() {
                return type;
            }

        };

        ObjectSelectionPage.prepareDialog(dialog, context, this, "chooseTypeDialog.title", ID_TEXT_WRAPPER);
        add(dialog);
    }


    private void initUserOrgDialog() {
        final ModalWindow dialog = new ModalWindow(MODAL_ID_OBJECT_SELECTION_POPUP);
        ObjectSelectionPanel.Context context = new ObjectSelectionPanel.Context(this) {
        	private static final long serialVersionUID = 1L;

            // See analogous discussion in ChooseTypePanel
            public AssociationValueChoicePanel getRealParent() {
                return WebComponentUtil.theSameForPage(AssociationValueChoicePanel.this, getCallingPageReference());
            }

            @Override
            public void chooseOperationPerformed(AjaxRequestTarget target, ObjectType object) {
                getRealParent().choosePerformed(target, object);
            }

            @Override
            public boolean isSearchEnabled() {
                return true;
            }

            @Override
            public Class<? extends ObjectType> getObjectTypeClass() {
                return UserType.class;
            }

            @Override
            protected WebMarkupContainer createExtraContentContainer(String extraContentId, final ObjectSelectionPanel objectSelectionPanel) {
                return new UserOrgReferenceChoosePanel(extraContentId, Boolean.FALSE) {
                    @Override
                    protected void onReferenceTypeChangePerformed(AjaxRequestTarget target, Boolean newValue) {
                        objectSelectionPanel.updateTableByTypePerformed(target, Boolean.FALSE.equals(newValue) ? UserType.class : OrgType.class);
                    }
                };
            }
        };

        ObjectSelectionPage.prepareDialog(dialog, context, this, "chooseTypeDialog.title", ID_TEXT_WRAPPER);
        add(dialog);
    }

    protected ObjectQuery createChooseQuery(List<PrismPropertyValue> values) {
        ArrayList<String> oidList = new ArrayList<>();
        ObjectQuery query = new ObjectQuery();
//TODO we should add to filter currently displayed value
//not to be displayed on ObjectSelectionPanel instead of saved value
//		for (PrismReferenceValue ref : values) {
//			if (ref != null) {
//				if (ref.getOid() != null && !ref.getOid().isEmpty()) {
//					oidList.add(ref.getOid());
//				}
//			}
//		}

//		if (isediting) {
//			oidList.add(orgModel.getObject().getObject().asObjectable().getOid());
//		}

        if (oidList.isEmpty()) {
            return null;
        }

        ObjectFilter oidFilter = InOidFilter.createInOid(oidList);
        query.setFilter(NotFilter.createNot(oidFilter));

        return query;
    }

    /**
     * @return css class for off-setting other values (not first, left to the
     *         first there is a label)
     */
    protected String getOffsetClass() {
        return "col-md-offset-4";
    }

    protected IModel<String> createTextModel(final IModel<PrismContainerValue<ShadowAssociationType>> model) {
        return new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
            public String getObject() {
            	PrismContainerValue<ShadowAssociationType> cval = model.getObject();
            	if (cval == null || cval.isEmpty()) {
            		return "";
            	}
            	PrismReferenceValue shadowRef = cval.findReference(ShadowAssociationType.F_SHADOW_REF).getValue();
            	if (shadowRef.getObject() == null) {
            		PrismContainer<Containerable> identifiersContainer = cval.findContainer(ShadowAssociationType.F_IDENTIFIERS);

            		List<PrismProperty<String>> identifiers = (List) identifiersContainer.getValue().getItems();
            		Collection<? extends RefinedAttributeDefinition<?>> secondaryIdentifierDefs = assocTargetDef.getSecondaryIdentifiers();

            		for (RefinedAttributeDefinition<?> secondaryIdentifierDef: secondaryIdentifierDefs) {
            			for (PrismProperty<String> identifier: identifiers) {
            				if (identifier.getElementName().equals(secondaryIdentifierDef.getName())) {
            					return identifier.getRealValue();
            				}
            			}
            		}

            		// fallback
            		PrismProperty<String> identifierProp = identifiers.get(0);
            		return identifierProp.getRealValue();

            	} else {
            		return shadowRef.getObject().getName().toString();
            	}

            }
        };
    }

    public void editValuePerformed(AjaxRequestTarget target) {
        ModalWindow window = (ModalWindow) get(MODAL_ID_OBJECT_SELECTION_POPUP);
        window.show(target);
    }

    protected void choosePerformed(AjaxRequestTarget target, C object) {
        ModalWindow window = (ModalWindow) get(MODAL_ID_OBJECT_SELECTION_POPUP);
        window.close(target);

        replace(object);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("New object instance has been added to the model.");
        }
    }


    protected boolean isObjectUnique(C object) {

    	PrismContainerValue<ShadowAssociationType> old = getModelObject();
        if (old == null || old.isEmpty()){
            return true;
        }
        if (old.asContainerable().getShadowRef().getOid().equals(object.getOid())) {
            return false;
        }

        return true;
    }

    public Component getTextComponent(){
        return get(ID_TEXT_WRAPPER).get(ID_TEXT);
    }


    /**
     * A custom code in form of hook that can be run on event of choosing new
     * object with this chooser component
     * */
    protected void choosePerformedHook(AjaxRequestTarget target, C object) {
    }

    private Collection<SelectorOptions<GetOperationOptions>> getAssociationsSearchOptions() {
        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        // HACK: raw shoudl not be here. noFetch should be enough. But it does not work
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH, GetOperationOptions.createRaw()));
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH, GetOperationOptions.createNoFetch()));
        return options;
    }

    //TODO move query creating code from PrismValuePanel
    private ObjectQuery getAssociationsSearchQuery() {
        return new ObjectQuery();
    }

}
