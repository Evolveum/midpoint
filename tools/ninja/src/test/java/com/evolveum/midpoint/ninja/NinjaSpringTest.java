package com.evolveum.midpoint.ninja;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.test.util.InfraTestMixin;

/**
 * Base class for Ninja tests that need Spring context, e.g. for repository state initialization.
 */
public abstract class NinjaSpringTest extends AbstractSpringTest implements InfraTestMixin, NinjaTestMixin {

    @Qualifier("repositoryService")
    @Autowired
    protected RepositoryService repository;

    @Autowired
    protected DataSource repositoryDataSource;

    @Autowired
    protected ApplicationContext applicationContext;

    @BeforeClass
    public void beforeClass() throws Exception {
        setupMidpointHome();

        clearMidpointTestDatabase(applicationContext);

        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }
}
