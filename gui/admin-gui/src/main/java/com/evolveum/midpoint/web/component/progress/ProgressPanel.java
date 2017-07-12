/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.util.List;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.RESOURCE_OBJECT_OPERATION;
import static com.evolveum.midpoint.web.component.progress.ProgressReportActivityDto.ResourceOperationResult;

/**
 * @author mederly
 */
public class ProgressPanel extends BasePanel<ProgressDto> {

    private static final String ID_CONTENTS_PANEL = "contents";
    private static final String ID_ACTIVITIES = "progressReportActivities";
    private static final String ID_ACTIVITY_DESCRIPTION = "description";
    private static final String ID_ACTIVITY_STATE = "status";
    private static final String ID_ACTIVITY_COMMENT = "comment";
    private static final String ID_STATISTICS = "statistics";
    private static final String ID_LOG_ITEMS = "logItems";
    private static final String ID_LOG_ITEM = "logItem";
    private static final String ID_EXECUTION_TIME = "executionTime";
    private static final String ID_PROGRESS_FORM = "progressForm";
	private static final String ID_BACK = "back";
	private static final String ID_ABORT = "abort";
	private static final String ID_CONTINUE_EDITING = "continueEditing";

	private ProgressReporter progressReporter;
    private Form progressForm;
    private long operationStartTime;            // if 0, operation hasn't start yet
    private long operationDurationTime;         // if >0, operation has finished

    private WebMarkupContainer contentsPanel;
    private StatisticsPanel statisticsPanel;

    public ProgressPanel(String id) {
        super(id);
    }

    public ProgressPanel(String id, IModel<ProgressDto> model, ProgressReporter progressReporter, ProgressReportingAwarePage page) {
        super(id, model);
        this.progressReporter = progressReporter;
        initLayout(page);
    }

    private void initLayout(ProgressReportingAwarePage page) {
    	progressForm = new Form<>(ID_PROGRESS_FORM, true);
    	add(progressForm);
    	
        contentsPanel = new WebMarkupContainer(ID_CONTENTS_PANEL);
        contentsPanel.setOutputMarkupId(true);
        progressForm.add(contentsPanel);

        ListView statusItemsListView = new ListView<ProgressReportActivityDto>(ID_ACTIVITIES, new AbstractReadOnlyModel<List<ProgressReportActivityDto>>() {
            @Override
            public List<ProgressReportActivityDto> getObject() {
                ProgressDto progressDto = ProgressPanel.this.getModelObject();
                return progressDto.getProgressReportActivities();
            }
        }) {
            protected void populateItem(final ListItem<ProgressReportActivityDto> item) {
                item.add(new Label(ID_ACTIVITY_DESCRIPTION, new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        ProgressReportActivityDto si = item.getModelObject();
                        if (si.getActivityType() == RESOURCE_OBJECT_OPERATION && si.getResourceShadowDiscriminator() != null) {
                            ResourceShadowDiscriminator rsd = si.getResourceShadowDiscriminator();
                            return createStringResource(rsd.getKind()).getString()
                                    + " (" + rsd.getIntent() + ") on " + si.getResourceName();             // TODO correct i18n
                        } else {
                            return createStringResource(si.getActivityType()).getString();
                        }
                    }
                }));
                item.add(createImageLabel(ID_ACTIVITY_STATE,
                        new AbstractReadOnlyModel<String>() {

                            @Override
                            public String getObject() {
                                OperationResultStatusType statusType = item.getModelObject().getStatus();
                                if (statusType == null) {
                                    return null;
                                } else {
                                    return OperationResultStatusPresentationProperties.parseOperationalResultStatus(statusType).getIcon() + " fa-lg";
                                }
                            }
                        },
                        new AbstractReadOnlyModel<String>() {

                            @Override
                            public String getObject() {     // TODO why this does not work???
                                OperationResultStatusType statusType = item.getModelObject().getStatus();       // TODO i18n
                                if (statusType == null) {
                                    return null;
                                } else {
                                    return statusType.toString();
                                }
                            }
                        }
                ));
                item.add(new Label(ID_ACTIVITY_COMMENT, new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        ProgressReportActivityDto si = item.getModelObject();
                        if (si.getResourceName() != null || si.getResourceOperationResultList() != null) {
                            StringBuilder sb = new StringBuilder();
                            boolean first = true;
                            if (si.getResourceOperationResultList() != null) {
                                for (ResourceOperationResult ror : si.getResourceOperationResultList()) {
                                    if (!first) {
                                        sb.append(", ");
                                    } else {
                                        first = false;
                                    }
                                    sb.append(createStringResource("ChangeType." + ror.getChangeType()).getString());
                                    sb.append(":");
                                    sb.append(createStringResource(ror.getResultStatus()).getString());
                                }
                            }
                            if (si.getResourceObjectName() != null) {
                                if (!first) {
                                    sb.append(" -> ");
                                }
                                sb.append(si.getResourceObjectName());
                            }
                            return sb.toString();
                        } else {
                            return null;
                        }
                    }
                }));
            }

            private Label createImageLabel(String id, IModel<String> cssClass, IModel<String> title) {
                Label label = new Label(id);
                label.add(AttributeModifier.replace("class", cssClass));
                label.add(AttributeModifier.replace("title", title));           // does not work, currently

                return label;
            }

        };
        contentsPanel.add(statusItemsListView);

        statisticsPanel = new StatisticsPanel(ID_STATISTICS, new StatisticsDtoModel());
        contentsPanel.add(statisticsPanel);

        ListView logItemsListView = new ListView(ID_LOG_ITEMS, new AbstractReadOnlyModel<List>() {
            @Override
            public List getObject() {
                ProgressDto progressDto = ProgressPanel.this.getModelObject();
                return progressDto.getLogItems();
            }
        }) {
            protected void populateItem(ListItem item) {
                item.add(new Label(ID_LOG_ITEM, item.getModel()));
            }
        };
        contentsPanel.add(logItemsListView);

        Label executionTime = new Label(ID_EXECUTION_TIME, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                if (operationDurationTime > 0) {
                    return createStringResource("ProgressPanel.ExecutionTimeWhenFinished", operationDurationTime).getString();
                } else if (operationStartTime > 0) {
                    return createStringResource("ProgressPanel.ExecutionTimeWhenRunning", (System.currentTimeMillis()-operationStartTime)/1000).getString();
                } else {
                    return null;
                }
            }
        });
        contentsPanel.add(executionTime);
        
        initButtons(progressForm, page);
    }
    
    private void initButtons(final Form progressForm, final ProgressReportingAwarePage page) {

		AjaxSubmitButton abortButton = new AjaxSubmitButton(ID_ABORT,
				createStringResource("pageAdminFocus.button.abort")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				progressReporter.onAbortSubmit(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				target.add(page.getFeedbackPanel());
			}
		};

        progressReporter.registerAbortButton(abortButton);
        progressForm.add(abortButton);

		AjaxSubmitButton backButton = new AjaxSubmitButton(ID_BACK,
				createStringResource("pageAdminFocus.button.back")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                backPerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				target.add(page.getFeedbackPanel());
			}
		};
		progressReporter.registerBackButton(backButton);
		progressForm.add(backButton);

		AjaxSubmitButton continueEditingButton = new AjaxSubmitButton(ID_CONTINUE_EDITING,
				createStringResource("pageAdminFocus.button.continueEditing")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
				ProgressReportingAwarePage page = (ProgressReportingAwarePage) getPage();
				page.continueEditing(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				target.add(page.getFeedbackPanel());
			}
		};
		progressReporter.registerContinueEditingButton(continueEditingButton);
		progressForm.add(continueEditingButton);

	}

    protected void backPerformed(AjaxRequestTarget target) {
        PageBase page = getPageBase();
        page.redirectBack();
    }

    // Note: do not setVisible(false) on the progress panel itself - it will disable AJAX refresh functionality attached to it.
    // Use the following two methods instead.

    public void show() {
        contentsPanel.setVisible(true);
    }

    public void hide() {
        contentsPanel.setVisible(false);
    }

    public void recordExecutionStart() {
        operationDurationTime = 0;
        operationStartTime = System.currentTimeMillis();
    }

    public void recordExecutionStop() {
        operationDurationTime = System.currentTimeMillis() - operationStartTime;
    }

    public void setTask(Task task) {
        if (statisticsPanel != null && statisticsPanel.getModel() instanceof StatisticsDtoModel) {
            ((StatisticsDtoModel) statisticsPanel.getModel()).setTask(task);
        }
    }

    public void invalidateCache() {
        if (statisticsPanel != null && statisticsPanel.getModel() instanceof StatisticsDtoModel) {
            ((StatisticsDtoModel) (statisticsPanel.getModel())).invalidateCache();
        }
    }
}
