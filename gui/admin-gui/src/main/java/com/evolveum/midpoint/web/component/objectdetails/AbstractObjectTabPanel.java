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
package com.evolveum.midpoint.web.component.objectdetails;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismPropertyPanel;
import com.evolveum.midpoint.web.model.PropertyWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 */
public abstract class AbstractObjectTabPanel<O extends ObjectType> extends Panel {
	private static final long serialVersionUID = 1L;

	protected static final String ID_MAIN_FORM = "mainForm";

	private static final Trace LOGGER = TraceManager.getTrace(AbstractObjectTabPanel.class);

	private LoadableModel<ObjectWrapper<O>> objectWrapperModel;
	protected PageBase pageBase;
	private Form<ObjectWrapper<O>> mainForm;

	public AbstractObjectTabPanel(String id, Form<ObjectWrapper<O>> mainForm, LoadableModel<ObjectWrapper<O>> objectWrapperModel, PageBase pageBase) {
		super(id);
		this.objectWrapperModel = objectWrapperModel;
		this.mainForm = mainForm;
		this.pageBase = pageBase;
	}

	public LoadableModel<ObjectWrapper<O>> getObjectWrapperModel() {
		return objectWrapperModel;
	}

	public ObjectWrapper<O> getObjectWrapper() {
		return objectWrapperModel.getObject();
	}

	protected PrismContext getPrismContext() {
		return pageBase.getPrismContext();
	}

	protected PageParameters getPageParameters() {
		return pageBase.getPageParameters();
	}
	
	public PageBase getPageBase() {
		return pageBase;
	}

	public Form<ObjectWrapper<O>> getMainForm() {
		return mainForm;
	}

	public StringResourceModel createStringResource(String resourceKey, Object... objects) {
		return PageBase.createStringResourceStatic(this, resourceKey, objects);
//		return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
	}

	public String getString(String resourceKey, Object... objects) {
		return createStringResource(resourceKey, objects).getString();
	}

	protected String createComponentPath(String... components) {
		return StringUtils.join(components, ":");
	}

	protected void showResult(OperationResult result) {
		pageBase.showResult(result);
	}
	
	protected void showResult(OperationResult result, boolean showSuccess) {
		pageBase.showResult(result, false);
	}
	
	
	protected WebMarkupContainer getFeedbackPanel() {
		return pageBase.getFeedbackPanel();
	}

	public Object findParam(String param, String oid, OperationResult result) {

		Object object = null;

		for (OperationResult subResult : result.getSubresults()) {
			if (subResult != null && subResult.getParams() != null) {
				if (subResult.getParams().get(param) != null
						&& subResult.getParams().get(OperationResult.PARAM_OID) != null
						&& subResult.getParams().get(OperationResult.PARAM_OID).equals(oid)) {
					return subResult.getParams().get(param);
				}
				object = findParam(param, oid, subResult);

			}
		}
		return object;
	}

	protected void showModalWindow(Popupable popupable, AjaxRequestTarget target) {
        getPageBase().showMainPopup(popupable, target);
		target.add(getFeedbackPanel());
	}

	protected void addPrismPropertyPanel(MarkupContainer parentComponent, String id, QName propertyName) {
		addPrismPropertyPanel(parentComponent, id, new ItemPath(propertyName));
	}
	
	protected void addPrismPropertyPanel(MarkupContainer parentComponent, String id, ItemPath propertyPath) {
		parentComponent.add(
				new PrismPropertyPanel(id,
						new PropertyWrapperFromObjectWrapperModel<PolyString,O>(getObjectWrapperModel(), propertyPath),
						mainForm, pageBase));
	}
}
