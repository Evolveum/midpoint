package com.evolveum.midpoint.web.session;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class SessionStorage implements Serializable {

    /**
     * place to store "previous page" for back button
     */
    private Class<? extends WebPage> previousPage;
    /**
     * place to store "previous page" parameters for back button
     */
    private PageParameters previousPageParams;

    /**
     * place to store informations in session for "configuration" pages
     */
    private ConfigurationStorage configuration;
    /**
     * Store session information for "users" pages
     */
    private UsersStorage users;

    public Class<? extends WebPage> getPreviousPage() {
        return previousPage;
    }

    public void setPreviousPage(Class<? extends WebPage> previousPage) {
        this.previousPage = previousPage;
    }

    public PageParameters getPreviousPageParams() {
        return previousPageParams;
    }

    public void setPreviousPageParams(PageParameters previousPageParams) {
        this.previousPageParams = previousPageParams;
    }

    public ConfigurationStorage getConfiguration() {
        if (configuration == null) {
            configuration = new ConfigurationStorage();
        }
        return configuration;
    }

    public UsersStorage getUsers() {
        if (users == null) {
            users = new UsersStorage();
        }
        return users;
    }
}
