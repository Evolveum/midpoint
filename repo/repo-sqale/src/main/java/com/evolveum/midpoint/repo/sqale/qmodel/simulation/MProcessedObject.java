package com.evolveum.midpoint.repo.sqale.qmodel.simulation;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectProcessingStateType;

public class MProcessedObject extends MContainer {

    public UUID oid;
    public MObjectType objectType;
    public String nameOrig;
    public String nameNorm;

    public ObjectProcessingStateType state;
    public String[] metricIdentifiers;

    public byte[] fullObject;

    public byte[] objectBefore;
    public byte[] objectAfter;

}
