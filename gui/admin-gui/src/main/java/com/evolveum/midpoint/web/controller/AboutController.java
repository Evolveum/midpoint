package com.evolveum.midpoint.web.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

@Controller("about")
@Scope("request")
public class AboutController {

	private static final String[] properties = new String[] { "file.separator", "java.class.path",
			"java.home", "java.vendor", "java.vendor.url", "java.version", "line.separator", "os.arch",
			"os.name", "os.version", "path.separator" };

	public List<SystemItem> getItems() {
		List<SystemItem> items = new ArrayList<SystemItem>();
		for (String property : properties) {
			items.add(new SystemItem(property, System.getProperty(property)));
		}

		return items;
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
