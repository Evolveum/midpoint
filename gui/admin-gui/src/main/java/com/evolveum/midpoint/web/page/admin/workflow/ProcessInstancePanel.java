package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.lang.reflect.InvocationTargetException;

public class ProcessInstancePanel extends SimplePanel<ProcessInstanceDto> {

    private static final String ID_DETAILS = "details";
    private static final String ID_TASK = "task";
    private static final String ID_TASK_COMMENT = "taskComment";

    public ProcessInstancePanel(String id, IModel<ProcessInstanceDto> model, String detailsPanelClassName) {
        super(id, model);
        initLayoutLocal(detailsPanelClassName);
    }

    private void initLayoutLocal(String detailsPanelClassName) {
        final IModel<ProcessInstanceDto> model = getModel();

        Label name = new Label("name", new PropertyModel(model, "name"));
        add(name);

        Label pid = new Label("pid", new PropertyModel(model, "instanceId"));
        add(pid);

        Label started = new Label("started", new PropertyModel(model, "startedTime"));
        add(started);

        Label finished = new Label("finished", new PropertyModel(model, "finishedTime"));
        add(finished);

        // todo what if task does not exist?
        LinkPanel task = new LinkPanel(ID_TASK, new PropertyModel(model, ProcessInstanceDto.F_WATCHING_TASK_OID)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String oid = model.getObject().getWatchingTaskOid();
                if (oid != null) {
                    PageParameters parameters = new PageParameters();
                    parameters.add(PageTaskEdit.PARAM_TASK_EDIT_ID, oid);
                    setResponsePage(new PageTaskEdit(parameters, this.getPage()));
                }
            }
        };
        add(task);

        Label taskComment = new Label(ID_TASK_COMMENT, createStringResource("processInstancePanel.taskMightBeRemoved"));
        taskComment.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return model.getObject().isFinished();
            }
        });
        add(taskComment);

        Throwable problem = null;
        try {
            Class<? extends Panel> panelClass = (Class<? extends Panel>) Class.forName(detailsPanelClassName);
            Panel detailsPanel = panelClass.getConstructor(String.class, IModel.class).newInstance(ID_DETAILS, model);
            add(detailsPanel);
        } catch (ClassNotFoundException e) {
            problem = e;
        } catch (InvocationTargetException e) {
            problem = e;
        } catch (InstantiationException e) {
            problem = e;
        } catch (NoSuchMethodException e) {
            problem = e;
        } catch (IllegalAccessException e) {
            problem = e;
        } catch (RuntimeException e) {
            problem = e;
        }

        if (problem != null) {
            Label problemLabel = new Label(ID_DETAILS, "Details cannot be shown because of the following exception: " + problem.getMessage() + ". Please see the log for more details");
            add(problemLabel);
        }
    }


}
