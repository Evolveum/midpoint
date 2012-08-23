/**
 * 
 */
package com.evolveum.midpoint.web.component.orgStruct;

import java.io.Serializable;

import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author mserbak
 * 
 */
public class NodeDto implements Serializable {
	private NodeType type;
	private String nodeName;
	
	public NodeDto(NodeType type, String node) {
		this.type = type;
		this.nodeName = node;
	}

	public NodeType getType() {
		return type;
	}

	public String getNodeName() {
		return nodeName;
	}
	
	public void setNodeName(String object) {
		this.nodeName = nodeName;
	}
}
