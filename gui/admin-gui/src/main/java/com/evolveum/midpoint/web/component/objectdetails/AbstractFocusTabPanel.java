/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.LoadableModel;
import com.evolveum.midpoint.web.model.PropertyWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.page.admin.users.component.*;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusProjectionDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SimpleUserResourceProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import javax.xml.namespace.QName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author semancik
 */
public abstract class AbstractFocusTabPanel<F extends FocusType> extends Panel {
	private static final long serialVersionUID = 1L;

	public static final String AUTH_USERS_ALL = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL;
	public static final String AUTH_USERS_ALL_LABEL = "PageAdminUsers.auth.usersAll.label";
	public static final String AUTH_USERS_ALL_DESCRIPTION = "PageAdminUsers.auth.usersAll.description";

	public static final String AUTH_ORG_ALL = AuthorizationConstants.AUTZ_UI_ORG_ALL_URL;
	public static final String AUTH_ORG_ALL_LABEL = "PageAdminUsers.auth.orgAll.label";
	public static final String AUTH_ORG_ALL_DESCRIPTION = "PageAdminUsers.auth.orgAll.description";
	
	protected static final String ID_MAIN_FORM = "mainForm";

	private static final Trace LOGGER = TraceManager.getTrace(AbstractFocusTabPanel.class);

	private LoadableModel<ObjectWrapper<F>> focusModel;
	private PageBase pageBase;
	private Form mainForm;

	public AbstractFocusTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusModel, PageBase pageBase) {
		super(id);
		this.focusModel = focusModel;
		this.mainForm = mainForm;
		this.pageBase = pageBase;
	}

	public LoadableModel<ObjectWrapper<F>> getFocusModel() {
		return focusModel;
	}

	public ObjectWrapper<F> getFocusWrapper() {
		return focusModel.getObject();
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

	public Form getMainForm() {
		return mainForm;
	}

	public StringResourceModel createStringResource(String resourceKey, Object... objects) {
		return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
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
	
	protected void showResultInSession(OperationResult result) {
		pageBase.showResultInSession(result);
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

	protected void showModalWindow(String id, AjaxRequestTarget target) {
		ModalWindow window = (ModalWindow) get(id);
		window.show(target);
		target.add(getFeedbackPanel());
	}
	
	protected void addPrismPropertyPanel(MarkupContainer parentComponent, String id, QName propertyName) {
		addPrismPropertyPanel(parentComponent, id, new ItemPath(propertyName));
	}
	
	protected void addPrismPropertyPanel(MarkupContainer parentComponent, String id, ItemPath propertyPath) {
		parentComponent.add(
				new PrismPropertyPanel(id,
						new PropertyWrapperFromObjectWrapperModel<PolyString,F>(getFocusModel(), propertyPath),
						mainForm, pageBase));
	}
}
