package com.evolveum.midpoint.model.intest.security;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;
import static org.assertj.core.api.Assertions.*;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSecurityValueMetadata extends AbstractInitializedSecurityTest {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        repoAdd(ROLE_END_USER_WITH_PRIVACY, initResult);
    }

    @Test
    public void testJackReadSelf() throws Exception{
        var task = createTask();
        var result = task.getResult();
        assignRole(USER_JACK_OID, ROLE_END_USER_WITH_PRIVACY.oid, task, result);
        login(USER_JACK_USERNAME);

        PrismObject<UserType> userJack = assertGetAllow(UserType.class, USER_JACK_OID);
        display("Jack", userJack);

        then("user should have object metadata visible");
        assertThat(userJack.getValue().getValueMetadata().isEmpty()).isFalse();

        assertThat(userJack.asObjectable().getAssignment()).extracting(a -> a.asPrismContainerValue().getValueMetadata().isEmpty())
                .allMatch(Boolean.TRUE::equals);

    }

}
