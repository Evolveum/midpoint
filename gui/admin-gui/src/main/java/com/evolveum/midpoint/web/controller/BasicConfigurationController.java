package com.evolveum.midpoint.web.controller;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

@Controller("basic")
@Scope("session")
public class BasicConfigurationController {

	private String content = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
			+ "<ui:composition template=\"/resources/templates/template.xhtml\" "
			+ "xmlns=\"http://www.w3.org/1999/xhtml\">\n</ui:composition>";

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
	public String action() {
		System.out.println(content);
		return null;
	}
}
