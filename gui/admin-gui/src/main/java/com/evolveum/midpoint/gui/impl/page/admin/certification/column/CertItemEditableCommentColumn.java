/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.column;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.ColumnTypeConfigContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.ColumnType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.data.column.DirectlyEditablePropertyColumn;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import java.io.Serial;

@ColumnType(identifier = "certItemEditableComment",
        applicableForType = AccessCertificationWorkItemType.class,
        display = @PanelDisplay(label = "PageCertDecisions.table.comment", order = 80))
public class CertItemEditableCommentColumn extends AbstractCertificationItemColumn {

    private static final Trace LOGGER = TraceManager.getTrace(CertItemEditableCommentColumn.class);
    private static final String DOT_CLASS = CertItemEditableCommentColumn.class.getName() + ".";
    private static final String OPERATION_RECORD_COMMENT = DOT_CLASS + "recordComment";

    public CertItemEditableCommentColumn(GuiObjectColumnType columnConfig, ColumnTypeConfigContext context) {
        super(columnConfig, context);
    }


    @Override
    public IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createColumn() {
        String propertyExpression = "realValue" + "." + AccessCertificationWorkItemType.F_OUTPUT.getLocalPart() + "."
                + AbstractWorkItemOutputType.F_COMMENT.getLocalPart();
        return new DirectlyEditablePropertyColumn<>(getColumnLabelModel(), propertyExpression) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onBlur(AjaxRequestTarget target,
                    IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> model) {
                recordCommentPerformed(target, model.getObject());
            }
        };
    }

    private void recordCommentPerformed(AjaxRequestTarget target, PrismContainerValueWrapper<AccessCertificationWorkItemType> certItemWrapper) {
        if (certItemWrapper == null) {
            return;
        }
        PageBase pageBase = context.getPageBase();
        OperationResult result = new OperationResult(OPERATION_RECORD_COMMENT);
        try {
            AccessCertificationWorkItemType certItem = certItemWrapper.getRealValue();
            if (certItem == null) {
                return;
            }
            //todo check if comment was really changed
            //for now certItemWrapper.findProperty(ItemPath.create(AccessCertificationWorkItemType.F_OUTPUT, AbstractWorkItemOutputType.F_COMMENT))
            //returns null so that we cannot analyze the delta
            Task task = pageBase.createSimpleTask(OPERATION_RECORD_COMMENT);
            String comment = certItem.getOutput() != null ? certItem.getOutput().getComment() : null;
            CertMiscUtil.recordCertItemResponse(
                    certItem, null, comment, result, task, pageBase);

        } catch (Exception ex) {
            LOGGER.error("Couldn't record comment for certification work item", ex);
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!result.isSuccess()) {
            pageBase.showResult(result);
        }
        target.add(pageBase);
    }

    @Override
    public boolean isVisible() {
        return getColumnConfig() != null && WebComponentUtil.getElementVisibility(getColumnConfig().getVisibility());
    }

    public boolean isDefaultColumn() {
        return false;
    }
}
