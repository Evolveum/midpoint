/**
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.gui.api.component.objecttypeselect;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class ObjectTypeSelectPanel<O extends ObjectType> extends BasePanel<QName> {
	private static final long serialVersionUID = 1L;

	private static final String ID_SELECT = "select";

	private DropDownChoice<QName> select;

	public ObjectTypeSelectPanel(String id, IModel<QName> model, Class<O> superclass) {
		super(id, model);
		initLayout(model, superclass);
	}

	private void initLayout(IModel<QName> model, final Class<O> superclass) {
		select = new DropDownChoice<>(ID_SELECT, model,
				new AbstractReadOnlyModel<List<QName>>() {
					private static final long serialVersionUID = 1L;

					@Override
		            public List<QName> getObject() {
						if (superclass == null || superclass == ObjectType.class) {
							return WebComponentUtil.createObjectTypeList();
						}
						if (superclass == FocusType.class) {
							return WebComponentUtil.createFocusTypeList();
						}
						if (superclass == AbstractRoleType.class) {
							return WebComponentUtil.createAbstractRoleTypeList();
						}
						throw new IllegalArgumentException("Unknown superclass "+superclass);
		            }
			}, new QNameChoiceRenderer());
		select.setNullValid(true);

		add(select);
	}

	public void addInput(Behavior behavior) {
		select.add(behavior);
	}

}
