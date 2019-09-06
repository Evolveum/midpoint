/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskChangesDto;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class TaskChangesPanel extends BasePanel<TaskChangesDto> {

//    private static final String ID_TITLE = "title";
    private static final String ID_PRIMARY_DELTA = "primaryDelta";

    public TaskChangesPanel(String id, IModel<TaskChangesDto> model) {
        super(id, model);
		initLayout();
    }

    protected void initLayout() {
//        add(new Label(ID_TITLE, new IModel<String>() {
//			@Override
//			public String getObject() {
//				return getString(getModelObject().getTitleKey());
//			}
//		}));

        ScenePanel deltaPanel = new ScenePanel(ID_PRIMARY_DELTA, new PropertyModel<>(getModel(), TaskChangesDto.F_PRIMARY_DELTAS));
        deltaPanel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getModelObject() != null && getModelObject().getPrimaryDeltas() != null;
            }
        });
        add(deltaPanel);
    }
}
