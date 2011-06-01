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
package com.evolveum.midpoint.web.controller.util;

import java.util.List;

import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;

import com.evolveum.midpoint.web.bean.SelectableBean;

/**
 * 
 * @author lazyman
 * 
 */
public class ControllerUtil {

	private static boolean isEventAvailable(ValueChangeEvent evt) {
		if (evt.getPhaseId() != PhaseId.INVOKE_APPLICATION) {
			evt.setPhaseId(PhaseId.INVOKE_APPLICATION);
			evt.queue();

			return false;
		}

		return true;
	}

	public static boolean selectPerformed(ValueChangeEvent evt, List<? extends SelectableBean> beans) {
		boolean selectedAll = false;
		if (isEventAvailable(evt)) {
			boolean selected = ((Boolean) evt.getNewValue()).booleanValue();
			if (!selected) {
				selectedAll = false;
			} else {
				selectedAll = true;
				for (SelectableBean item : beans) {
					if (!item.isSelected()) {
						selectedAll = false;
						break;
					}
				}
			}
		}

		return selectedAll;
	}

	public static void selectAllPerformed(ValueChangeEvent evt, List<? extends SelectableBean> beans) {
		if (isEventAvailable(evt)) {
			boolean selectAll = ((Boolean) evt.getNewValue()).booleanValue();
			for (SelectableBean item : beans) {
				item.setSelected(selectAll);
			}
		}
	}
}
