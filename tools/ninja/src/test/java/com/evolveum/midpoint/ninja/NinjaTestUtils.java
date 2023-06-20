package com.evolveum.midpoint.ninja;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.impl.NinjaContext;

import java.util.List;

public class NinjaTestUtils {

    public static <O, R, A extends Action<O, R>> R runAction(Class<A> actionClass, O actionOptions, List<Object> options) throws Exception {
        A action = actionClass.getConstructor().newInstance();
        try (NinjaContext context = new NinjaContext(System.out, System.err, options, action.getApplicationContextLevel(options))) {
            action.init(context, actionOptions);

            return action.execute();
        }
    }
}
