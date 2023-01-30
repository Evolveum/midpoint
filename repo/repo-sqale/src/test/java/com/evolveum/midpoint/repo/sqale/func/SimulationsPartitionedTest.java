package com.evolveum.midpoint.repo.sqale.func;

public class SimulationsPartitionedTest extends SimulationsBaselineTest {

    @Override
    protected boolean getPartitioned() {
        return true;
    }
}
