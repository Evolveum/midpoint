package com.evolveum.midpoint.ninja.util;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;


/**
 * Created by Viliam Repan (lazyman).
 */
public class InitializationBeanPostprocessor implements BeanPostProcessor {

    private String jdbcUrl;
    private String jdbcUsername;
    private String jdbcPassword;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (jdbcUrl == null) {
            return bean;
        }

        if (!(bean instanceof MidpointConfiguration)) {
            return bean;
        }

        MidpointConfiguration config = (MidpointConfiguration) bean;
        Configuration repositoryConfig = config.getConfiguration("midpoint.repository");
        repositoryConfig.setProperty(SqlRepositoryConfiguration.PROPERTY_DATABASE, getDatabase(jdbcUrl));
        repositoryConfig.setProperty(SqlRepositoryConfiguration.PROPERTY_JDBC_URL, jdbcUrl);
        repositoryConfig.setProperty(SqlRepositoryConfiguration.PROPERTY_JDBC_USERNAME, jdbcUsername);
        repositoryConfig.setProperty(SqlRepositoryConfiguration.PROPERTY_JDBC_PASSWORD, jdbcPassword);

        return config;
    }

    private String getDatabase(String url) {
        String postfix = url.replaceFirst("jdbc:", "").toLowerCase();
        if (postfix.startsWith("postgresql")) {
            return "postgresql";
        } else if (postfix.startsWith("sqlserver")) {
            return "sqlserver";
        } else if (postfix.startsWith("mysql")) {
            return "mysql";
        } else if (postfix.startsWith("mariadb")) {
            return "mariadb";
        } else if (postfix.startsWith("oracle")) {
            return "oracle";
        } else if (postfix.startsWith("h2")) {
            return "h2";
        }

        throw new IllegalStateException("Unknown database for url " + url);
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public void setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
    }

    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }
}
