/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.server.currentState;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.progress.StatisticsDtoModel;
import com.evolveum.midpoint.web.component.progress.StatisticsPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author mederly
 */
public class SynchronizationInformationPanel extends SimplePanel<SynchronizationInformationDto> {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationInformationPanel.class);

    private static final String ID_PROTECTED = "protected";
    private static final String ID_NO_SYNCHRONIZATION_POLICY = "noSynchronizationPolicy";
    private static final String ID_SYNCHRONIZATION_DISABLED = "synchronizationDisabled";
    private static final String ID_NOT_APPLICABLE_FOR_TASK = "notApplicableForTask";
    private static final String ID_DELETED = "deleted";
    private static final String ID_DISPUTED = "disputed";
    private static final String ID_LINKED = "linked";
    private static final String ID_UNLINKED = "unlinked";
    private static final String ID_UNMATCHED = "unmatched";

    public SynchronizationInformationPanel(String id, IModel<SynchronizationInformationDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {

        Label aProtected = new Label(ID_PROTECTED, new PropertyModel<>(getModel(), SynchronizationInformationDto.F_COUNT_PROTECTED));
        add(aProtected);

        Label noSync = new Label(ID_NO_SYNCHRONIZATION_POLICY, new PropertyModel<>(getModel(), SynchronizationInformationDto.F_COUNT_NO_SYNCHRONIZATION_POLICY));
        add(noSync);

        Label syncDisabled = new Label(ID_SYNCHRONIZATION_DISABLED, new PropertyModel<>(getModel(), SynchronizationInformationDto.F_COUNT_SYNCHRONIZATION_DISABLED));
        add(syncDisabled);

        Label notAppl = new Label(ID_NOT_APPLICABLE_FOR_TASK, new PropertyModel<>(getModel(), SynchronizationInformationDto.F_COUNT_NOT_APPLICABLE_FOR_TASK));
        add(notAppl);

        Label deleted = new Label(ID_DELETED, new PropertyModel<>(getModel(), SynchronizationInformationDto.F_COUNT_DELETED));
        add(deleted);

        Label disputed = new Label(ID_DISPUTED, new PropertyModel<>(getModel(), SynchronizationInformationDto.F_COUNT_DISPUTED));
        add(disputed);

        Label linked = new Label(ID_LINKED, new PropertyModel<>(getModel(), SynchronizationInformationDto.F_COUNT_LINKED));
        add(linked);

        Label unlinked = new Label(ID_UNLINKED, new PropertyModel<>(getModel(), SynchronizationInformationDto.F_COUNT_UNLINKED));
        add(unlinked);

        Label unmatched = new Label(ID_UNMATCHED, new PropertyModel<>(getModel(), SynchronizationInformationDto.F_COUNT_UNMATCHED));
        add(unmatched);
    }
}
