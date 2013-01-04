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

import org.apache.wicket.Component;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.page.admin.PageAdmin;

/**
 * todo WTF!!!!!!!!!!!!!!!
 *
 * why Content extends PageAdmin in here??what is this class for...?
 *
 * @author mserbak
 */
public abstract class Content extends PageAdmin implements IDetachable
{
	public abstract Component newContentComponent(String id, AbstractTree<NodeDto> tree,
			IModel<NodeDto> model);
	
	public abstract Component newNodeComponent(String id, AbstractTree<NodeDto> tree,
			IModel<NodeDto> model);

}