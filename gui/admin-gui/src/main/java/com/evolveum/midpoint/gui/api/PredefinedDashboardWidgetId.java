/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.gui.api;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;

/**
 * @author semancik
 *
 */
public enum PredefinedDashboardWidgetId {

	SEARCH("search"),
	SHORTCUTS("shortcuts"),
	MY_WORKITEMS("myWorkItems"),
	MY_REQUESTS("myRequests"),
	MY_ASSIGNMENTS("myAssignments"),
	MY_ACCOUNTS("myAccounts");

	private final QName qname;
	private final String uri;

	PredefinedDashboardWidgetId(String localPart) {
		this.qname = new QName(ComponentConstants.NS_DASHBOARD_WIDGET, localPart);
		this.uri = QNameUtil.qNameToUri(qname);
	}

	public QName getQname() {
		return qname;
	}

	public String getUri() {
		return uri;
	}

}
