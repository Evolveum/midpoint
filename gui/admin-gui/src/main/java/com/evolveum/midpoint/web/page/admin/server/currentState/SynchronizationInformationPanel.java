/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.currentState;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.SynchronizationInformationDto;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class SynchronizationInformationPanel extends BasePanel<SynchronizationInformationDto> {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationInformationPanel.class);

    private static final String ID_TITLE_BEFORE = "titleBefore";
    private static final String ID_TITLE_AFTER = "titleAfter";
    private static final String ID_PROTECTED = "protected";
    private static final String ID_NO_SYNCHRONIZATION_POLICY = "noSynchronizationPolicy";
    private static final String ID_SYNCHRONIZATION_DISABLED = "synchronizationDisabled";
    private static final String ID_NOT_APPLICABLE_FOR_TASK = "notApplicableForTask";
    private static final String ID_DELETED = "deleted";
    private static final String ID_DISPUTED = "disputed";
    private static final String ID_LINKED = "linked";
    private static final String ID_UNLINKED = "unlinked";
    private static final String ID_UNMATCHED = "unmatched";

    public SynchronizationInformationPanel(String id, IModel<SynchronizationInformationDto> model, boolean useAfter) {
        super(id, model);
        initLayout(useAfter);
    }

    protected void initLayout(final boolean useAfter) {

        WebMarkupContainer titleBefore = new WebMarkupContainer(ID_TITLE_BEFORE);
        titleBefore.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !useAfter;
            }
        });
        add(titleBefore);

        WebMarkupContainer titleAfter = new WebMarkupContainer(ID_TITLE_AFTER);
        titleAfter.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return useAfter;
            }
        });
        add(titleAfter);

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
