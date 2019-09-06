/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.ScannerHandlerDto;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class ScannerHandlerPanel extends BasePanel<ScannerHandlerDto> {
	private static final long serialVersionUID = 1L;

	private static final String ID_LAST_SCAN_TIMESTAMP_CONTAINER = "lastScanTimestampContainer";
	private static final String ID_LAST_SCAN_TIMESTAMP = "lastScanTimestamp";

	public ScannerHandlerPanel(String id, IModel<ScannerHandlerDto> model) {
		super(id, model);
		initLayout();
		setOutputMarkupId(true);
	}

	private void initLayout() {
		WebMarkupContainer lastScanTimestampContainer = new WebMarkupContainer(ID_LAST_SCAN_TIMESTAMP_CONTAINER);
//		lastScanTimestampContainer.add(new VisibleEnableBehaviour() {
//			@Override
//			public boolean isVisible() {
//				return getModelObject().getTaskDto().getObjectRefOid() != null;
//			}
//		});
		lastScanTimestampContainer.add(new Label(ID_LAST_SCAN_TIMESTAMP, new PropertyModel<>(getModel(), ScannerHandlerDto.F_LAST_SCAN_TIMESTAMP)));
		add(lastScanTimestampContainer);
	}

}
