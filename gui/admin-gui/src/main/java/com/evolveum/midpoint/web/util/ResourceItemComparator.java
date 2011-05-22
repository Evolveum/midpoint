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
 */
package com.evolveum.midpoint.web.util;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.web.bean.ResourceListItem;
import com.evolveum.midpoint.web.bean.ResourceListItem.ConnectionStatus;

/**
 * 
 * @author lazyman
 * 
 */
public class ResourceItemComparator extends SortableListComparator<ResourceListItem> {

	private static final long serialVersionUID = 8938044753222634767L;
	private static final ConnectionStatusComparator STATUS_ORDER = new ConnectionStatusComparator();

	public ResourceItemComparator(String attribute, boolean ascending) {
		super(attribute, ascending);
	}

	@Override
	public int compare(ResourceListItem item1, ResourceListItem item2) {
		// name, type, version, status
		if (StringUtils.isEmpty(getAttribute())) {
			return 0;
		}

		int value = 0;
		if (getAttribute().equals("name")) {
			value = String.CASE_INSENSITIVE_ORDER.compare(item1.getName(), item2.getName());
		} else if (getAttribute().equals("type")) {
			value = String.CASE_INSENSITIVE_ORDER.compare(item1.getType(), item2.getType());
		} else if (getAttribute().equals("version")) {
			value = String.CASE_INSENSITIVE_ORDER.compare(item1.getVersion(), item2.getVersion());
		} else if (getAttribute().equals("status")) {
			value = STATUS_ORDER.compare(item1.getStatus(), item2.getStatus());
		}

		return isAscending() ? value : -value;
	}

	private static class ConnectionStatusComparator implements Comparator<ConnectionStatus>, Serializable {

		private static final long serialVersionUID = 5728544984564936762L;

		@Override
		public int compare(ConnectionStatus status1, ConnectionStatus status2) {
			int intStatus1 = getStatusPosition(status1);
			int intStatus2 = getStatusPosition(status2);
			return (intStatus2 - intStatus1);
		}

		private int getStatusPosition(ConnectionStatus status) {
			ConnectionStatus[] statuses = ConnectionStatus.values();
			for (int i = 0; i < statuses.length; i++) {
				if (statuses[i].equals(status)) {
					return i;
				}
			}

			return -1;
		}
	}
}
