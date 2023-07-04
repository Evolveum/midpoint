/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.provider;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;

import com.evolveum.midpoint.web.page.admin.configuration.InternalsLoggedInUsersPanel;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

public class LoggedInUsersProvider<F extends FocusType> extends BaseSortableDataProvider<SelectableBean<F>> implements ISelectableDataProvider<SelectableBean<F>> {

    private static final String DOT_CLASS = LoggedInUsersProvider.class.getName() + ".";
    private static final String OPERATION_LOAD_LOGGED_IN_PRINCIPALS = DOT_CLASS + "loadLoggedInPrincipals";

    public LoggedInUsersProvider(Component component) {
        super(component);
    }

    @Override
    public Iterator<SelectableBean<F>> internalIterator(long first, long count) {
        getAvailableData().clear();

        List<UserSessionManagementType> loggedInUsers = getLoggedInUsers();


        if (loggedInUsers != null) {
            for (long i = first; i < first + count; i++) {
                if (i < 0 || i >= loggedInUsers.size()) {
                    throw new ArrayIndexOutOfBoundsException(
                            "Trying to get item on index " + i + " but list size is " + loggedInUsers.size());
                }

                UserSessionManagementType loggedInUser = loggedInUsers.get(WebComponentUtil.safeLongToInteger(i));
                SelectableBeanImpl<F> user = new SelectableBeanImpl<>(Model.of((F) loggedInUser.getFocus()));
                user.setActiveSessions(loggedInUser.getActiveSessions());
                user.setNodes(loggedInUser.getNode());
                getAvailableData().add(user);
            }
        }

        return getAvailableData().iterator();
    }
    @Override
    protected int internalSize() {
        List<UserSessionManagementType> loggedInUsers = getLoggedInUsers();
        if (loggedInUsers == null) {
            return 0;
        }

        return loggedInUsers.size();
    }

    private List<UserSessionManagementType> getLoggedInUsers() {
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_LOGGED_IN_PRINCIPALS);
        OperationResult result = task.getResult();
        return getPageBase().getModelInteractionService().getLoggedInPrincipals(task, result);
    }

}
