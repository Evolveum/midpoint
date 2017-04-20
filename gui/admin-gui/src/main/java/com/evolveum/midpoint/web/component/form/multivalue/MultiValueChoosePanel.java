/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.web.component.form.multivalue;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
/**
 * 
 * @param <T> model/chosen object types
 */
public class MultiValueChoosePanel<T extends ObjectType> extends BasePanel<List<T>> {


	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(MultiValueChoosePanel.class);

	private static final String ID_SELECTED_ROWS = "selectedRows";
	private static final String ID_TEXT_WRAPPER = "textWrapper";
	private static final String ID_TEXT = "text";
	private static final String ID_FEEDBACK = "feedback";
	private static final String ID_EDIT = "edit";
    private static final String ID_REMOVE = "remove";
    private static final String ID_BUTTON_GROUP = "buttonGroup";
	

	protected static final String MODAL_ID_OBJECT_SELECTION_POPUP = "objectSelectionPopup";

	private List<QName> typeQNames;

	private Class<? extends T> defaultType;

	private Collection<Class<? extends T>> types;
	
	public MultiValueChoosePanel(String id, IModel<List<T>> value, Collection<Class<? extends T>> types) {
		this(id, value, null, false, types);
	}
	
	public MultiValueChoosePanel(String id, IModel<List<T>> value, Collection<Class<? extends T>> types, boolean multiselect) {
		this(id, value, null, false, types, multiselect);
	}

	public MultiValueChoosePanel(String id, IModel<List<T>> chosenValues, List<PrismReferenceValue> filterValues, boolean required,
			Collection<Class<? extends T>> types) {
		this(id, chosenValues, filterValues, required, types, true);
	}

	public MultiValueChoosePanel(String id, IModel<List<T>> chosenValues, List<PrismReferenceValue> filterValues, boolean required,
			Collection<Class<? extends T>> types, boolean multiselect) {

		super(id, chosenValues);
		setOutputMarkupPlaceholderTag(true);

		this.types = types;
		this.defaultType = userOrFirst(types);
		// initialize typeQNames in onInitialize
		
		LOGGER.debug("Init multi value choose panel with model {}", chosenValues);
		initLayout(chosenValues, filterValues, required, multiselect);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		// initialize types when component is in page and getPageBase() has meaning
		this.typeQNames = WebComponentUtil.resolveObjectTypesToQNames(types,
				getPageBase().getPrismContext());
		typeQNames.sort((t1, t2) -> t1.getLocalPart().compareTo(t2.getLocalPart()));
	}
	
    private void initLayout(final IModel<List<T>> chosenValues, final List<PrismReferenceValue> filterValues,
			final boolean required, final boolean multiselect) {

		AjaxLink<String> edit = new AjaxLink<String>(ID_EDIT) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				editValuePerformed(chosenValues.getObject(), filterValues, target, multiselect);
			}
		};
		edit.setOutputMarkupPlaceholderTag(true);
		add(edit);

		ListView<T> selectedRowsList = new ListView<T>(ID_SELECTED_ROWS, chosenValues) {

			@Override
			protected void populateItem(ListItem<T> item) {
				WebMarkupContainer textWrapper = new WebMarkupContainer(ID_TEXT_WRAPPER);
				
				textWrapper.setOutputMarkupPlaceholderTag(true);
		
				TextField<String> text = new TextField<String>(ID_TEXT, createTextModel(item.getModel())); //was value
				text.add(new AjaxFormComponentUpdatingBehavior("blur") {
					private static final long serialVersionUID = 1L;
		
					@Override
					protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
					}
				});
				text.setRequired(required);
				text.setEnabled(false);
				text.setOutputMarkupPlaceholderTag(true);
				textWrapper.add(text);
		
				FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ComponentFeedbackMessageFilter(text));
				feedback.setOutputMarkupPlaceholderTag(true);
				textWrapper.add(feedback);
		
		        initButtons(item, item);
		        
		        item.add(textWrapper);
			}
		};
		selectedRowsList.setReuseItems(true);
		add(selectedRowsList);
    }

	protected ObjectQuery createChooseQuery(List<PrismReferenceValue> values) {
		ArrayList<String> oidList = new ArrayList<>();
		ObjectQuery query = new ObjectQuery();

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

	protected IModel<String> createTextModel(final IModel<T> model) {
		return new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {

				return ofNullable(model.getObject())
					.map(ObjectType::getName)
					.map(PolyString::getOrig)
					.orElse(null);
			}
		};
	}

	protected void editValuePerformed(List<T> chosenValues, List<PrismReferenceValue> filterValues, AjaxRequestTarget target, boolean multiselect) {

		ObjectBrowserPanel<T> objectBrowserPanel = new ObjectBrowserPanel<T>(
				getPageBase().getMainPopupBodyId(), defaultType, typeQNames, multiselect, getPageBase(),
				null, chosenValues) {

			private static final long serialVersionUID = 1L;
			
			@Override
			protected void addPerformed(AjaxRequestTarget target, QName type, List<T> selected) {
				getPageBase().hideMainPopup(target);
				MultiValueChoosePanel.this.addPerformed(target, selected);
			}

			@Override
			protected void onSelectPerformed(AjaxRequestTarget target, T focus) {
				super.onSelectPerformed(target, focus);
				if (!multiselect) {
					// asList alone is not modifiable, you can't add/remove
					// elements later
					selectPerformed(target, new ArrayList<>(asList(focus)));
				}
			}

		};

		getPageBase().showMainPopup(objectBrowserPanel, target);

	}

	protected void selectPerformed(AjaxRequestTarget target, List<T> chosenValues) {
		getModel().setObject(chosenValues);
		choosePerformedHook(target, chosenValues);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("New object instance has been added to the model.");
		}
		target.add(MultiValueChoosePanel.this);
	}

	protected void addPerformed(AjaxRequestTarget target, List<T> addedValues) {
		List<T> modelList = getModelObject();
		if(modelList == null) {
			modelList = new ArrayList<T>();
		}
		addedValues.removeAll(modelList); // add values not already in
		modelList.addAll(addedValues);
		getModel().setObject(modelList);
		choosePerformedHook(target, modelList);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("New object instance has been added to the model.");
		}
		target.add(MultiValueChoosePanel.this);
	}

    public WebMarkupContainer getTextWrapperComponent(){
        return (WebMarkupContainer)get(ID_TEXT_WRAPPER);
    }

    protected void initButtons(ListItem<T> item, WebMarkupContainer parent) {
        WebMarkupContainer buttonGroup = new WebMarkupContainer(ID_BUTTON_GROUP); {
	        buttonGroup.setOutputMarkupId(true);
	
	        AjaxLink remove = new AjaxLink(ID_REMOVE) {
	
	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                removeValuePerformed(target, item.getModelObject());
	            }
	        };
	        
	        remove.add(new VisibleEnableBehaviour() {
	
	            @Override
	            public boolean isVisible() {
	                return isRemoveButtonVisible();
	            }
	        });
	        buttonGroup.add(remove);
        }

        parent.add(buttonGroup);
}

	private boolean isRemoveButtonVisible() {
		return true;
	}

	private void removeValuePerformed(AjaxRequestTarget target, T value) {
		
		LOGGER.debug("Removing value {} from selected list", value);
		
		getModelObject().remove(value);
		removePerformedHook(target, value);
		target.add(this);
	}

	protected void removePerformedHook(AjaxRequestTarget target, T value) {
		
	}

	/**
	 * A custom code in form of hook that can be run on event of choosing new
	 * object with this chooser component
	 */
	protected void choosePerformedHook(AjaxRequestTarget target, List<T> selected) {
	}

	private Class<? extends T> userOrFirst(Collection<Class<? extends T>> types) {
		// ugly hack to select UserType as default if available
		if(types == null) {
			return null;
		}
		return types.stream()
				.filter(type -> type == UserType.class)
				.findFirst().orElse(
						CollectionUtils.isNotEmpty(types) ? types.iterator().next() : null);
	}
}
