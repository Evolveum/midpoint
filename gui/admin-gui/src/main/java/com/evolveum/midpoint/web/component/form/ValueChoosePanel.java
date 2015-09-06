package com.evolveum.midpoint.web.component.form;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueChoosePanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypeDialog;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ValueChoosePanel <T, C extends ObjectType> extends SimplePanel<T> {

	private static final Trace LOGGER = TraceManager.getTrace(MultiValueChoosePanel.class);

	private static final String ID_LABEL = "label";

	private static final String ID_TEXT_WRAPPER = "textWrapper";
	private static final String ID_TEXT = "text";
	private static final String ID_FEEDBACK = "feedback";
	private static final String ID_ADD = "add";
	private static final String ID_REMOVE = "remove";
	private static final String ID_BUTTON_GROUP = "buttonGroup";
	private static final String ID_EDIT = "edit";

	protected static final String MODAL_ID_CHOOSE_PANEL = "showPopup";

	private static final String CLASS_MULTI_VALUE = "multivalue-form";

	public ValueChoosePanel(String id, IModel<T> value, IModel<String> label, String labelSize,
			String textSize, boolean required, Class<C> type) {
		super(id, value);
		setOutputMarkupId(true);

		initLayout(value, label, labelSize, textSize, required, type);
	}

	private void initLayout(final IModel<T> value, final IModel<String> label, final String labelSize,
			final String textSize, final boolean required, Class<C> type) {

		Label l = new Label(ID_LABEL, label);

		if (StringUtils.isNotEmpty(labelSize)) {
			l.add(AttributeAppender.prepend("class", labelSize));
		}
		add(l);

		WebMarkupContainer textWrapper = new WebMarkupContainer(ID_TEXT_WRAPPER);
		textWrapper.add(AttributeAppender.prepend("class", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				StringBuilder sb = new StringBuilder();
				if (StringUtils.isNotEmpty(textSize)) {
					sb.append(textSize).append(' ');
				}

				return sb.toString();
			}
		}));

		TextField text = new TextField<>(ID_TEXT, createTextModel(value));
		text.add(new AjaxFormComponentUpdatingBehavior("onblur") {
			@Override
			protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
			}
		});
		text.setRequired(required);
		text.setEnabled(false);
		text.add(AttributeAppender.replace("placeholder", label));
		text.setLabel(label);
		textWrapper.add(text);

		FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ComponentFeedbackMessageFilter(text));
		textWrapper.add(feedback);

		AjaxLink edit = new AjaxLink(ID_EDIT) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				editValuePerformed(target);
			}
		};
		textWrapper.add(edit);
		add(textWrapper);

		initDialog(type);

	}
	
	  protected T createNewEmptyItem() throws InstantiationException, IllegalAccessException {
	        return null;
	    }
	  
	  protected void replaceIfEmpty(Object object) {
		  boolean added = false;
		 	  ObjectReferenceType ort = ObjectTypeUtil.createObjectRef((ObjectType) object);
			  getModel().setObject((T)ort.asReferenceValue());
		 
	  }

	protected void initDialog(final Class<C> type) {
		ModalWindow dialog = new ChooseTypeDialog(MODAL_ID_CHOOSE_PANEL, type) {

			@Override
			protected void chooseOperationPerformed(AjaxRequestTarget target, ObjectType object) {
				choosePerformed(target, (C) object);
			}

			@Override
			protected ObjectQuery getDataProviderQuery() {
				return createChooseQuery();
			}
			
			@Override
			public boolean isSearchEnabled() {
				return true;
			}
		};
		add(dialog);
	}

	protected ObjectQuery createChooseQuery() {
		return null;
	}

	/**
	 * @return css class for off-setting other values (not first, left to the
	 *         first there is a label)
	 */
	protected String getOffsetClass() {
		return "col-md-offset-4";
	}

	protected IModel<String> createTextModel(final IModel<T> model) {
		return new IModel<String>() {
			@Override
			public String getObject() {
				T obj = model.getObject();
				return obj != null ? obj.toString() : null;
			}

			@Override
			public void setObject(String object) {
			}

			@Override
			public void detach() {
			}
		};
	}

	protected void editValuePerformed(AjaxRequestTarget target) {
		ModalWindow window = (ModalWindow) get(MODAL_ID_CHOOSE_PANEL);
		ChooseTypeDialog dialog = (ChooseTypeDialog) window;
		dialog.updateTablePerformed(target, createChooseQuery());
		window.show(target);
	}

	/*
	 * TODO - this method contains check, if chosen object already is not in
	 * selected values array This is a temporary solution until we well be able
	 * to create "already-chosen" query
	 */
	protected void choosePerformed(AjaxRequestTarget target, C object) {
		choosePerformedHook(target, object);
		ModalWindow window = (ModalWindow) get(MODAL_ID_CHOOSE_PANEL);
		window.close(target);

		 if(isObjectUnique(object)){
			 replaceIfEmpty(object);
		 }

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("New object instance has been added to the model.");
		}

		target.add(this);
	}

	// protected void replaceIfEmpty(Object object){
	// List<T> objects = getModelObject();
	// objects.add((T)object);
	// }

	protected boolean isObjectUnique(C object) {

		// for(T o: ){
		PrismReferenceValue old = (PrismReferenceValue)getModelObject();
		if (old == null || old.isEmpty()){
			return true;
		}
		if (old.getOid().equals(object.getOid())) {
			return false;
		}
		// }
		return true;
	}

//	protected void removeValuePerformed(AjaxRequestTarget target, ListItem<T> item) {
//		List<T> objects = getModelObject();
//		Iterator<T> iterator = objects.iterator();
//		while (iterator.hasNext()) {
//			T object = iterator.next();
//
//			if (object.equals(item.getModelObject())) {
//				iterator.remove();
//				break;
//			}
//		}
//
//		if (objects.size() == 0) {
//			objects.add(createNewEmptyItem());
//		}
//
//		target.add(this);
//	}

	/**
	 * A custom code in form of hook that can be run on event of choosing new
	 * object with this chooser component
	 * */
	protected void choosePerformedHook(AjaxRequestTarget target, C object) {
	}

}