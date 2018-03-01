package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.repo.sql.DataSourceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

import java.util.Properties;

/**
 * Created by Viliam Repan (lazyman).
 */
public class StupidClass {

    @Autowired
    private DataSourceFactory dataSourceFactory;
    @Autowired
    private EntityStateInterceptor entityStateInterceptor;
    @Autowired
    private MidPointImplicitNamingStrategy midPointImplicitNamingStrategy;
    @Autowired
    private MidPointPhysicalNamingStrategy midPointPhysicalNamingStrategy;

    public LocalSessionFactoryBean initSessionFactory() throws Exception {
        LocalSessionFactoryBean bean = new LocalSessionFactoryBean();

        bean.setDataSource(dataSourceFactory.createDataSource());

        Properties hibernateProperties = new Properties();
        hibernateProperties.setProperty("hibernate.dialect", dataSourceFactory.getConfiguration().getHibernateDialect());
        hibernateProperties.setProperty("hibernate.hbm2ddl.auto", dataSourceFactory.getConfiguration().getHibernateHbm2ddl() );
        hibernateProperties.setProperty("hibernate.id.new_generator_mappings", "true");
        hibernateProperties.setProperty("hibernate.jdbc.batch_size", "20");
        hibernateProperties.setProperty("javax.persistence.validation.mode", "none");
        hibernateProperties.setProperty("hibernate.transaction.coordinator_class", "jdbc");
        hibernateProperties.setProperty("hibernate.hql.bulk_id_strategy", "org.hibernate.hql.spi.id.inline.InlineIdsOrClauseBulkIdStrategy");

        bean.setHibernateProperties(hibernateProperties);
        bean.setImplicitNamingStrategy(midPointImplicitNamingStrategy);
        bean.setPhysicalNamingStrategy(midPointPhysicalNamingStrategy);
        bean.setAnnotatedPackages("com.evolveum.midpoint.repo.sql.type");
        bean.setPackagesToScan(
                "com.evolveum.midpoint.repo.sql.data.common",
                "com.evolveum.midpoint.repo.sql.data.common.any",
                "com.evolveum.midpoint.repo.sql.data.common.container",
                "com.evolveum.midpoint.repo.sql.data.common.embedded",
                "com.evolveum.midpoint.repo.sql.data.common.enums",
                "com.evolveum.midpoint.repo.sql.data.common.id",
                "com.evolveum.midpoint.repo.sql.data.common.other",
                "com.evolveum.midpoint.repo.sql.data.common.type",
                "com.evolveum.midpoint.repo.sql.data.audit");
        bean.setEntityInterceptor(entityStateInterceptor);

        return bean;
    }
}
