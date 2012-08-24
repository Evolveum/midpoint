/*
 * Copyright 2009 Sven Meier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.orgStruct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Sven Meier
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
