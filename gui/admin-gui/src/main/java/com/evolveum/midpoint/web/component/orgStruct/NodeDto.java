/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.web.component.orgStruct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author mserbak
 */
public class NodeDto
{

	private static final long serialVersionUID = 1L;

	private String id;
	private String bar;
	private String baz;
	private boolean quux;
	private boolean loaded;
	private NodeDto parent;
	private NodeType type = NodeType.USER;

	private List<NodeDto> foos = new ArrayList<NodeDto>();

	public NodeDto(String id){
		this.id = id;
		this.bar = id.toLowerCase() + "Bar";
		this.baz = id.toLowerCase() + "Baz";
	}
	
	public NodeDto(NodeDto parent, String name)
	{
		this(parent, name, null);
	}
	
	public NodeDto(NodeDto parent, String name, NodeType type){
		this(name);
		
		if(type != null) {
			this.type = type;
		}
		this.parent = parent;
		this.parent.foos.add(this);
	}

	
	
	public NodeType getType() {
		return type;
	}
	
	public NodeDto getParent()
	{
		return parent;
	}

	public String getId()
	{
		return id;
	}

	public String getBar()
	{
		return bar;
	}

	public String getBaz()
	{
		return baz;
	}

	public void setBar(String bar)
	{
		this.bar = bar;
	}

	public void setBaz(String baz)
	{
		this.baz = baz;
	}

	public void setQuux(boolean quux)
	{
		this.quux = quux;

		if (quux)
		{
			// set quux on all descendants
			for (NodeDto foo : foos)
			{
				foo.setQuux(true);
			}
		}
		else
		{
			// clear quux on all ancestors
			if (parent != null)
			{
				parent.setQuux(false);
			}
		}
	}

	public boolean getQuux()
	{
		return quux;
	}

	public List<NodeDto> getFoos()
	{
		return Collections.unmodifiableList(foos);
	}

	@Override
	public String toString()
	{
		return id;
	}
	
	public boolean isLoaded() {
		return loaded;
	}
	
	public void setLoaded(boolean loaded)
	{
		this.loaded = loaded;
	}
}
