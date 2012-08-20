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

package com.evolveum.midpoint.web.page.admin.help;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.LoadableModel;

/**
 * @author mserbak
 */
public class PageSystem extends PageAboutAdmin {

	private static final String[] properties = new String[] { "file.separator", "java.class.path",
			"java.home", "java.vendor", "java.vendor.url", "java.version", "line.separator", "os.arch",
			"os.name", "os.version", "path.separator" };

	public PageSystem() {

		Label revisionLabel = new Label("revision", createStringResource("pageSystem.midPointRevision"));
		add(revisionLabel);
		createSystemItemsList();
	}

	private void createSystemItemsList() {
		ListView<SystemItem> listSystemItems = new ListView<SystemItem>("listSystemItems", getItems()) {

			@Override
			protected void populateItem(ListItem<SystemItem> item) {
				item.add(new Label("property", item.getModel().getObject().getProperty()));
				item.add(new Label("value", item.getModel().getObject().getValue()));
			}
		};
		add(listSystemItems);
	}

	public IModel<List<SystemItem>> getItems() {
		return new LoadableModel<List<SystemItem>>(false) {

			@Override
			protected List<SystemItem> load() {
				List<SystemItem> items = new ArrayList<SystemItem>();
				for (String property : properties) {
					items.add(new SystemItem(property, System.getProperty(property)));
				}
				return items;
			}
		};
	}

	public static class SystemItem {
		private String property;
		private String value;

		private SystemItem(String property, String value) {
			this.property = property;
			this.value = value;
		}

		public String getProperty() {
			return property;
		}

		public String getValue() {
			return value;
		}
	}
}
