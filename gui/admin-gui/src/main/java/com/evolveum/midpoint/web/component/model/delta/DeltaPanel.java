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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.model.delta;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.workflow.PageWorkItems;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDetailedDto;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.wf.api.WorkItemDetailed;
import com.evolveum.midpoint.wf.api.WorkflowService;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author mederly
 */
public class DeltaPanel extends SimplePanel<DeltaDto> {

    private static final Trace LOGGER = TraceManager.getTrace(DeltaPanel.class);

    private static final String ID_CHANGE_TYPE = "changeType";
    private static final String ID_OID = "oid";
    private static final String ID_OID_LABEL = "oidLabel";
    private static final String ID_OBJECT_TO_ADD = "objectToAdd";
    private static final String ID_OBJECT_TO_ADD_LABEL = "objectToAddLabel";
    private static final String ID_MODIFICATIONS = "modifications";
    private static final String ID_MODIFICATIONS_LABEL = "modificationsLabel";

    public DeltaPanel(String id, IModel<DeltaDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {

        Label changeType = new Label(ID_CHANGE_TYPE, new PropertyModel<String>(getModel(), DeltaDto.F_CHANGE_TYPE));
        add(changeType);

        VisibleEnableBehaviour isAdd = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getModel().getObject().isAdd();
            }
        };

        VisibleEnableBehaviour isNotAdd = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !getModel().getObject().isAdd();
            }
        };

        Label oidLabel = new Label(ID_OID_LABEL, new ResourceModel("DeltaPanel.label.oid"));
        oidLabel.add(isNotAdd);
        add(oidLabel);
        Label oid = new Label(ID_OID, new PropertyModel<String>(getModel(), DeltaDto.F_OID));
        oid.add(isNotAdd);
        add(oid);

        VisibleEnableBehaviour never = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return false;
            }
        };

        Label objectToAddLabel = new Label(ID_OBJECT_TO_ADD_LABEL, new ResourceModel("DeltaPanel.label.objectToAdd"));
        //objectToAddLabel.add(isAdd);
        objectToAddLabel.add(never);
        add(objectToAddLabel);

        ContainerValuePanel objectToAddPanel =
                new ContainerValuePanel(ID_OBJECT_TO_ADD,
                        new PropertyModel<ContainerValueDto>(getModel(), DeltaDto.F_OBJECT_TO_ADD));
        objectToAddPanel.add(isAdd);
        add(objectToAddPanel);

        Label modificationsLabel = new Label(ID_MODIFICATIONS_LABEL, new ResourceModel("DeltaPanel.label.modifications"));
        //modificationsLabel.add(isNotAdd);
        modificationsLabel.add(never);
        add(modificationsLabel);

        ModificationsPanel modificationsPanel = new ModificationsPanel(ID_MODIFICATIONS, getModel());
        modificationsPanel.add(isNotAdd);
        add(modificationsPanel);
    }
}
