package com.evolveum.midpoint.web.page.self.component;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.home.PageMyPasswords;
import com.evolveum.midpoint.web.page.admin.home.dto.AssignmentItemDto;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.self.dto.LinkDto;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

/**
 * Created by Kate on 23.09.2015.
 */
public class LinksPanel extends SimplePanel<LinkDto> {
    private static final String ID_CREDENTIALS_LINK = "credentialsLink";
    private static final String ID_CREDENTIALS_LABEL = "credentialsLabel";
    private static final String ID_ASSIGNMENTS_LINK = "assignmentsLink";
    private static final String ID_ROLES_LINK = "rolesLink";
    private static final String ID_ROLES_LABEL = "rolesLabel";
    private static final String ID_ORGANIZATIONS_LINK = "organizationsLink";
    private static final String ID_ORGANIZATIONS_LABEL = "organizationsLabel";
    private static final String ID_CONFIGURATION_LINK = "configurationLink";
    private static final String ID_TASKS_LINK = "tasksLink";
    private static final String ID_TASKS_LABEL = "tasksLabel";

    public LinksPanel(String id){
        super(id, null);
    }

    public LinksPanel(String id, IModel<LinkDto> model) {
        super(id, model);
    }
    @Override
    protected void initLayout() {
        Link credentialsLink = new Link(ID_CREDENTIALS_LINK) {
            @Override
            public void onClick() {
                setResponsePage(PageMyPasswords.class);
            }

        };
        credentialsLink.add(new Label(ID_CREDENTIALS_LABEL, new Model<String>() {
            public String getObject() {
                return createStringResource("LinksPanel.credentialsLabel").getString();
            }
        }));
        add(credentialsLink);

        Link tasksLink = new Link(ID_TASKS_LINK) {
            @Override
            public void onClick() {
                setResponsePage(PageTasks.class);
            }

        };
        tasksLink.add(new Label(ID_TASKS_LABEL, new Model<String>() {
            public String getObject() {
                return createStringResource("LinksPanel.tasksLabel").getString();
            }
        }));
        add(tasksLink);

        Link organizationsLink = new Link(ID_ORGANIZATIONS_LINK) {
            @Override
            public void onClick() {
                setResponsePage(PageOrgTree.class);
            }

        };
        organizationsLink.add(new Label(ID_ORGANIZATIONS_LABEL, new Model<String>() {
            public String getObject() {
                return createStringResource("LinksPanel.organizationsLabel").getString();
            }
        }));
        add(organizationsLink);

        Link rolesLink = new Link(ID_ROLES_LINK) {
            @Override
            public void onClick() {
                setResponsePage(PageRoles.class);
            }

        };
        rolesLink.add(new Label(ID_ROLES_LABEL, new Model<String>() {
            public String getObject() {
                return createStringResource("LinksPanel.rolesLabel").getString();
            }
        }));
        add(rolesLink);

    }
}
