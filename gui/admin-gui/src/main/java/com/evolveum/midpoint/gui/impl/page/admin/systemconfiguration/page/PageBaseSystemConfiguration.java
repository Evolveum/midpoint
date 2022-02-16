/*
 * Copyright (c) 2010-2022 Evolveum
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
package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.page;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

public abstract class PageBaseSystemConfiguration extends PageAssignmentHolderDetails<SystemConfigurationType, AssignmentHolderDetailsModel<SystemConfigurationType>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageBaseSystemConfiguration.class);

    public PageBaseSystemConfiguration() {
        super();
    }

    public PageBaseSystemConfiguration(PageParameters parameters) {
        super(parameters);
    }

    public PageBaseSystemConfiguration(final PrismObject<SystemConfigurationType> object) {
        super(object);
    }

    @Override
    public Class<SystemConfigurationType> getType() {
        return SystemConfigurationType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<SystemConfigurationType> model) {
        return new ObjectSummaryPanel(id, SystemConfigurationType.class, model, getSummaryPanelSpecification()) {

            @Override
            protected String getDefaultIconCssClass() {
                return null;
            }

            @Override
            protected String getIconBoxAdditionalCssClass() {
                return null;
            }

            @Override
            protected String getBoxAdditionalCssClass() {
                return null;
            }
        };
    }

    @Override
    protected String getObjectOidParameter() {
        return SystemObjectsType.SYSTEM_CONFIGURATION.value();
    }

    //
//    @Override
//    protected PrismObject<SystemConfigurationType> loadPrismObject(PrismObject<SystemConfigurationType> objectToEdit, Task task, OperationResult result) {
//        return WebModelServiceUtils.loadSystemConfigurationAsPrismObject(this, task, result);
//    }
//
//    @Override
//    protected ItemStatus computeWrapperStatus() {
//        return ItemStatus.NOT_CHANGED;
//    }
//
//    @Override
//    public Class<SystemConfigurationType> getCompileTimeClass() {
//        return SystemConfigurationType.class;
//    }
//
//    @Override
//    public SystemConfigurationType createNewObject() {
//        return new SystemConfigurationType(getPrismContext());
//    }
//
//    @Override
//    protected ObjectSummaryPanel<SystemConfigurationType> createSummaryPanel(IModel<SystemConfigurationType> summaryModel) {
//        return new SystemConfigurationSummaryPanel(ID_SUMMARY_PANEL, summaryModel, WebComponentUtil.getSummaryPanelSpecification(SystemConfigurationType.class, getCompiledGuiProfile()));
//    }
//
//    @Override
//    protected void setSummaryPanelVisibility(ObjectSummaryPanel<SystemConfigurationType> summaryPanel) {
//        summaryPanel.setVisible(true);
//    }
//
//    @Override
//    protected AbstractObjectMainPanel<SystemConfigurationType> createMainPanel(String id) {
//        return new AbstractObjectMainPanel<>(id, getObjectModel(), this) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            protected List<ITab> createTabs(PageAdminObjectDetails<SystemConfigurationType> parentPage) {
//                return PageAbstractSystemConfiguration.this.createTabs();
//            }
//
//            @Override
//            protected boolean getOptionsPanelVisibility() {
//                return false;
//            }
//
//            @Override
//            protected boolean isPreviewButtonVisible() {
//                return false;
//            }
//
//        };
//    }
//
//    @Override
//    protected Class<? extends Page> getRestartResponsePage() {
//        return PageSystemConfigurationNew.class;
//    }
//
//    <C extends Containerable> ContainerOfSystemConfigurationPanel<C> createContainerPanel(String panelId, IModel<? extends PrismContainerWrapper<C>> objectModel, ItemName propertyName, QName propertyType) {
//        return new ContainerOfSystemConfigurationPanel<>(panelId, createModel(objectModel, propertyName), propertyType);
//    }
//
//    <C extends Containerable, T extends Containerable> PrismContainerWrapperModel<C, T> createModel(IModel<? extends PrismContainerWrapper<C>> model, ItemName itemName) {
//        return PrismContainerWrapperModel.fromContainerWrapper(model, itemName);
//    }
//
//    protected abstract List<ITab> createTabs();
//
//    @Override
//    public void saveOrPreviewPerformed(AjaxRequestTarget target, OperationResult result, boolean previewOnly) {
//
//        ProgressPanel progressPanel = getProgressPanel();
//        progressPanel.hide();
//        Task task = createSimpleTask(OPERATION_SEND_TO_SUBMIT);
//        super.saveOrPreviewPerformed(target, result, previewOnly, task);
//
//        try {
//            TimeUnit.SECONDS.sleep(1);
//            while(task.isClosed()) {TimeUnit.SECONDS.sleep(1);}
//        } catch ( InterruptedException ex) {
//            result.recomputeStatus();
//            result.recordFatalError(getString("PageSystemConfiguration.message.saveOrPreviewPerformed.fatalError"), ex);
//
//            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't use sleep", ex);
//        }
//        result.recomputeStatus();
//        target.add(getFeedbackPanel());
//
//        if(result.getStatus().equals(OperationResultStatus.SUCCESS)) {
//            showResult(result);
//            redirectBack();
//        }
//    }
}
