/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.controller;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.LogInfo;
import com.evolveum.midpoint.logging.LoggerMXBean;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.bean.TraceItem;
import com.evolveum.midpoint.web.bean.TraceModule;
import com.evolveum.midpoint.web.util.FacesUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.apache.log4j.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.stereotype.Controller;

/**
 * 
 * @author lazyman
 */
@Controller("trace")
@Scope("session")
public class TraceController implements Serializable {

	private static final long serialVersionUID = 6200472992965694954L;
	private static final Trace TRACE = TraceManager.getTrace(TraceController.class);
	private static final int[] levels = { Level.ALL_INT, Level.DEBUG_INT, Level.ERROR_INT, Level.FATAL_INT,
			Level.INFO_INT, Level.OFF_INT, Level.TRACE_INT, Level.WARN_INT };
	private static final String JMX_QUERY = "midPoint:name=WebApplicationLogger";
	private static final String PARAM_ITEM_ID = "itemId";
	private TraceModule module;
	@Autowired(required = true)
	private transient org.springframework.jmx.export.MBeanExporter exporterWeb;

	public String refreshPerformed() {
		try {
			MBeanServer mbs = exporterWeb.getServer();
			List<ObjectName> names = new ArrayList<ObjectName>(
					mbs.queryNames(new ObjectName(JMX_QUERY), null));

			if (names == null || names.size() == 0) {
				String message = "Couldn't refresh logging settings, reason: Couldn't find MXBean "
						+ JMX_QUERY;
				TRACE.error(message);
				FacesUtils.addErrorMessage(message);
				return null;
			}

			ObjectName name = names.get(0);
			TRACE.debug("Available logger MXBean: " + name);

			LoggerMXBean bean = JMX.newMXBeanProxy(mbs, name, LoggerMXBean.class);
			module = new TraceModule(bean.getModuleLogLevel(), bean.getLogPattern(), bean.getDisplayName());

			int id = 0;
			List<LogInfo> infoList = bean.getLogInfoList();
			for (LogInfo info : infoList) {
				module.getItems().add(new TraceItem(id, info.getPackageName(), info.getLevel()));
				id++;
			}

			if (module.getItemsSize() == 0) {
				module.getItems().add(new TraceItem(id));
				id++;
			}
		} catch (Exception ex) {
			TRACE.error("Couldn't refresh logging settings.", ex);
			FacesUtils.addErrorMessage("Couldn't refresh logging settings, reason: " + ex.getMessage());
		}

		return null;
	}

	public void setExporterWeb(MBeanExporter exporter) {
		this.exporterWeb = exporter;
		refreshPerformed();
	}

	public TraceModule getModule() {
		return module;
	}

	public List<SelectItem> getLogLevels() {
		List<SelectItem> list = new ArrayList<SelectItem>();
		for (Integer level : levels) {
			list.add(new SelectItem(level, Level.toLevel(level).toString()));
		}

		return list;
	}

	public int getValue(String value) {
		int id = -1;
		if (value != null && value.matches("[0-9]*")) {
			id = Integer.parseInt(value);
		}

		return id;
	}

	public void deleteTrace(ActionEvent evt) {
		TRACE.debug("Deleting package");
		int itemId = getValue(FacesUtils.getRequestParameter(PARAM_ITEM_ID));
		if (module == null || itemId == -1) {
			return;
		}

		TraceItem toBeRemoved = null;
		List<TraceItem> items = module.getItems();
		for (TraceItem item : items) {
			if (item.getId() == itemId) {
				toBeRemoved = item;
				break;
			}
		}

		if (toBeRemoved != null) {
			items.remove(toBeRemoved);
		}
	}

	public void addTrace(ActionEvent evt) {
		TRACE.debug("Adding package");
		if (module == null) {
			return;
		}

		module.getItems().add(new TraceItem(getNewItemId()));
	}

	private int getNewItemId() {
		int id = 0;
		List<TraceItem> items = module.getItems();
		for (TraceItem item : items) {
			if (item.getId() > id) {
				id = item.getId();
			}
		}
		id++;

		return id;
	}

	private void removeEmptyTraceItems() {
		List<TraceItem> toBeDeleted = new ArrayList<TraceItem>();
		List<TraceItem> items = module.getItems();
		for (TraceItem item : items) {
			if (item.getPackageName() == null || item.getPackageName().isEmpty()) {
				toBeDeleted.add(item);
			}
		}

		module.getItems().removeAll(toBeDeleted);
	}

	public String savePerformed() {
		try {
			saveLoggingConfiguration();

			MBeanServer mbs = exporterWeb.getServer();
			List<ObjectName> names = new ArrayList<ObjectName>(
					mbs.queryNames(new ObjectName(JMX_QUERY), null));
			if (names == null || names.size() == 0) {
				String message = "Couldn't save logging settings, reason: Couldn't find MXBean " + JMX_QUERY;
				TRACE.error(message);
				FacesUtils.addErrorMessage(message);
				return null;
			}

			ObjectName name = names.get(0);
			saveChangesToJMX(JMX.newMXBeanProxy(mbs, name, LoggerMXBean.class));

			removeEmptyTraceItems();

			if (module.getItemsSize() == 0) {
				module.getItems().add(new TraceItem(getNewItemId()));
			}
		} catch (Exception ex) {
			TRACE.error("Couldn't save logging settings.", ex);
			FacesUtils.addErrorMessage("Couldn't save logging settings, reason: " + ex.getMessage());
		}

		return null;
	}

	private void saveLoggingConfiguration() {
		// TODO: implement saving to idm configuration xml
	}

	private void saveChangesToJMX(LoggerMXBean bean) {
		if (bean == null) {
			FacesUtils.addErrorMessage("Couldn't save settings for module '" + module.getName() + "'.");
			return;
		}

		List<LogInfo> infoList = new ArrayList<LogInfo>();
		List<TraceItem> items = module.getItems();
		for (TraceItem item : items) {
			if (item.getPackageName() == null || item.getPackageName().isEmpty()) {
				continue;
			}
			infoList.add(new LogInfo(item.getPackageName(), item.getLevel()));
		}

		bean.setModuleLogLevel(module.getLevel());
		bean.setLogPattern(module.getLogPattern());
		bean.setLogInfoList(infoList);
	}
}
