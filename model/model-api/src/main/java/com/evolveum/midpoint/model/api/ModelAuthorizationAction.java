package com.evolveum.midpoint.model.api;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;

public enum ModelAuthorizationAction implements DisplayableValue<String> {

	READ("read", "Read", "READ_HELP"),
	ADD("add", "Add", "ADD_HELP"),
	MODIFY("modify", "Modify", "MODIFY_HELP"),
	DELETE("delete", "Delete", "DELETE_HELP"),
	RECOMPUTE("recompute", "Recompute", "RECOMPUTE_HELP"),
	TEST("test", "Test resource", "TEST_RESOURCE_HELP"),
	
	/**
	 * Import objects from file or a stream. This means importing any type of
	 * object (e.g. user, configuration, resource, object templates, ...
	 */
	IMPORT_OBJECTS("importObjects", "Import Objects", "IMPORT_OBJECTS_HELP"),
	
	/**
	 * Import resource objects from resource. This means import of accounts, entitlements
	 * or other objects from a resource. The import creates shadows.
	 */
	IMPORT_FROM_RESOURCE("importFromResource", "Import from Resource", "IMPORT_FROM_RESOURCE_HELP"),
	
	DISCOVER_CONNECTORS("discoverConnectors", "Discover Connectors", "DISCOVER_CONNECTORS_HELP"), 
	
	ASSIGN("assign", "Assign", "ASSIGN_HELP"),
	UNASSIGN("unassign", "Unassign", "UNASSIGN_HELP"),
    EXECUTE_SCRIPT("executeScript", "Execute script", "EXECUTE_SCRIPT_HELP");
	
	private String url;
	private String label;
	private String description;
	
	private ModelAuthorizationAction(String urlLocalPart, String label, String desc) {
		this.url = QNameUtil.qNameToUri(new QName(ModelService.AUTZ_NAMESPACE, urlLocalPart));
		this.label = label;
		this.description = desc;
	}
	
	public String getUrl() {
		return url;
	}
	
	@Override
	public String getValue() {
		return url;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	
}
