/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.component.PendingOperationPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author skublik
 */

public class ProjectionDisplayNamePanel extends DisplayNamePanel<ShadowType>{

	private static final long serialVersionUID = 1L;
	
	private final static String ID_PENDING_OPERATION_CONTAINER = "pendingOperationContainer";
    private final static String ID_PENDING_OPERATION = "pendingOperation";

	public ProjectionDisplayNamePanel(String id, IModel<ShadowType> model) {
		super(id, model);

	}

	@Override
	protected void onInitialize(){
		super.onInitialize();
		initLayout();
	}

	private void initLayout() {
		WebMarkupContainer pendingOperationContainer = new WebMarkupContainer(ID_PENDING_OPERATION_CONTAINER);
		List<PendingOperationType> pendingOperations = getModelObject().getPendingOperation();
		if(pendingOperations != null
				&& !pendingOperations.isEmpty()) {
			
			pendingOperationContainer.add(new PendingOperationPanel(ID_PENDING_OPERATION, new IModel<List<PendingOperationType>>() {

				@Override
				public List<PendingOperationType> getObject() {
					return pendingOperations;
				}
				
			}));
		} else {
			pendingOperationContainer.add(new WebMarkupContainer(ID_PENDING_OPERATION));
			pendingOperationContainer.add(new VisibleEnableBehaviour() {
				@Override
				public boolean isVisible() {
					return false;
				}
			});
		}
		add(pendingOperationContainer);
	}
}
