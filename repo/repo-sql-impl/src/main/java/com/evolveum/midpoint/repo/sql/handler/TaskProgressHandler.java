package com.evolveum.midpoint.repo.sql.handler;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import java.util.Collection;

/**
 * > modify object TaskType, oid=f7fdd34d-562f-4355-a2ed-c7e111b9bae1, modifications=[PropertyDelta( / {.../common/common-3}progress, REPLACE)]
 * select this_.oid as oid1_18_0_, this_1_.createChannel as createCh2_18_0_, this_1_.createTimestamp as createTi3_18_0_, this_1_.creatorRef_relation as creatorR4_18_0_, this_1_.creatorRef_targetOid as creatorR5_18_0_, this_1_.creatorRef_type as creatorR6_18_0_, this_1_.datesCount as datesCou7_18_0_, this_1_.fullObject as fullObje8_18_0_, this_1_.longsCount as longsCou9_18_0_, this_1_.modifierRef_relation as modifie10_18_0_, this_1_.modifierRef_targetOid as modifie11_18_0_, this_1_.modifierRef_type as modifie12_18_0_, this_1_.modifyChannel as modifyC13_18_0_, this_1_.modifyTimestamp as modifyT14_18_0_, this_1_.name_norm as name_no15_18_0_, this_1_.name_orig as name_or16_18_0_, this_1_.objectTypeClass as objectT17_18_0_, this_1_.polysCount as polysCo18_18_0_, this_1_.referencesCount as referen19_18_0_, this_1_.stringsCount as strings20_18_0_, this_1_.tenantRef_relation as tenantR21_18_0_, this_1_.tenantRef_targetOid as tenantR22_18_0_, this_1_.tenantRef_type as tenantR23_18_0_, this_1_.version as version24_18_0_, this_.binding as binding1_36_0_, this_.canRunOnNode as canRunOn2_36_0_, this_.category as category3_36_0_, this_.completionTimestamp as completi4_36_0_, this_.executionStatus as executio5_36_0_, this_.handlerUri as handlerU6_36_0_, this_.lastRunFinishTimestamp as lastRunF7_36_0_, this_.lastRunStartTimestamp as lastRunS8_36_0_, this_.name_norm as name_nor9_36_0_, this_.name_orig as name_or10_36_0_, this_.node as node11_36_0_, this_.objectRef_relation as objectR12_36_0_, this_.objectRef_targetOid as objectR13_36_0_, this_.objectRef_type as objectR14_36_0_, this_.ownerRef_relation as ownerRe15_36_0_, this_.ownerRef_targetOid as ownerRe16_36_0_, this_.ownerRef_type as ownerRe17_36_0_, this_.parent as parent18_36_0_, this_.recurrence as recurre19_36_0_, this_.status as status20_36_0_, this_.taskIdentifier as taskIde21_36_0_, this_.threadStopAction as threadS22_36_0_, this_.waitingReason as waiting23_36_0_ from m_task this_ inner join m_object this_1_ on this_.oid=this_1_.oid where this_.oid=?
 * select parentorgr0_.owner_oid as owner_oi2_18_0_, parentorgr0_.owner_oid as owner_oi2_28_0_, parentorgr0_.relation as relation3_28_0_, parentorgr0_.targetOid as targetOi4_28_0_, parentorgr0_.owner_oid as owner_oi2_28_1_, parentorgr0_.relation as relation3_28_1_, parentorgr0_.targetOid as targetOi4_28_1_, parentorgr0_.containerType as containe5_28_1_, parentorgr0_.reference_type as referenc1_28_1_ from m_reference parentorgr0_ where ( parentorgr0_.reference_type=0) and parentorgr0_.owner_oid=?
 * select createappr0_.owner_oid as owner_oi2_18_0_, createappr0_.owner_oid as owner_oi2_28_0_, createappr0_.relation as relation3_28_0_, createappr0_.targetOid as targetOi4_28_0_, createappr0_.owner_oid as owner_oi2_28_1_, createappr0_.relation as relation3_28_1_, createappr0_.targetOid as targetOi4_28_1_, createappr0_.containerType as containe5_28_1_, createappr0_.reference_type as referenc1_28_1_ from m_reference createappr0_ where ( createappr0_.reference_type=5) and createappr0_.owner_oid=?
 * select dates0_.owner_oid as owner_oi2_18_0_, dates0_.eName as eName1_19_0_, dates0_.owner_oid as owner_oi2_19_0_, dates0_.ownerType as ownerTyp3_19_0_, dates0_.dateValue as dateValu4_19_0_, dates0_.eName as eName1_19_1_, dates0_.owner_oid as owner_oi2_19_1_, dates0_.ownerType as ownerTyp3_19_1_, dates0_.dateValue as dateValu4_19_1_, dates0_.dynamicDef as dynamicD5_19_1_, dates0_.eType as eType6_19_1_, dates0_.valueType as valueTyp7_19_1_ from m_object_ext_date dates0_ where dates0_.owner_oid=?
 * select longs0_.owner_oid as owner_oi2_18_0_, longs0_.eName as eName1_20_0_, longs0_.owner_oid as owner_oi2_20_0_, longs0_.ownerType as ownerTyp3_20_0_, longs0_.longValue as longValu4_20_0_, longs0_.eName as eName1_20_1_, longs0_.owner_oid as owner_oi2_20_1_, longs0_.ownerType as ownerTyp3_20_1_, longs0_.longValue as longValu4_20_1_, longs0_.dynamicDef as dynamicD5_20_1_, longs0_.eType as eType6_20_1_, longs0_.valueType as valueTyp7_20_1_ from m_object_ext_long longs0_ where longs0_.owner_oid=?
 * select modifyappr0_.owner_oid as owner_oi2_18_0_, modifyappr0_.owner_oid as owner_oi2_28_0_, modifyappr0_.relation as relation3_28_0_, modifyappr0_.targetOid as targetOi4_28_0_, modifyappr0_.owner_oid as owner_oi2_28_1_, modifyappr0_.relation as relation3_28_1_, modifyappr0_.targetOid as targetOi4_28_1_, modifyappr0_.containerType as containe5_28_1_, modifyappr0_.reference_type as referenc1_28_1_ from m_reference modifyappr0_ where ( modifyappr0_.reference_type=6) and modifyappr0_.owner_oid=?
 * select polys0_.owner_oid as owner_oi2_18_0_, polys0_.eName as eName1_21_0_, polys0_.owner_oid as owner_oi2_21_0_, polys0_.ownerType as ownerTyp3_21_0_, polys0_.orig as orig4_21_0_, polys0_.eName as eName1_21_1_, polys0_.owner_oid as owner_oi2_21_1_, polys0_.ownerType as ownerTyp3_21_1_, polys0_.orig as orig4_21_1_, polys0_.dynamicDef as dynamicD5_21_1_, polys0_.norm as norm6_21_1_, polys0_.eType as eType7_21_1_, polys0_.valueType as valueTyp8_21_1_ from m_object_ext_poly polys0_ where polys0_.owner_oid=?
 * select references0_.owner_oid as owner_oi2_18_0_, references0_.eName as eName1_22_0_, references0_.owner_oid as owner_oi2_22_0_, references0_.ownerType as ownerTyp3_22_0_, references0_.targetoid as targetoi4_22_0_, references0_.eName as eName1_22_1_, references0_.owner_oid as owner_oi2_22_1_, references0_.ownerType as ownerTyp3_22_1_, references0_.targetoid as targetoi4_22_1_, references0_.dynamicDef as dynamicD5_22_1_, references0_.relation as relation6_22_1_, references0_.targetType as targetTy7_22_1_, references0_.eType as eType8_22_1_, references0_.valueType as valueTyp9_22_1_ from m_object_ext_reference references0_ where references0_.owner_oid=?
 * select strings0_.owner_oid as owner_oi2_18_0_, strings0_.eName as eName1_23_0_, strings0_.owner_oid as owner_oi2_23_0_, strings0_.ownerType as ownerTyp3_23_0_, strings0_.stringValue as stringVa4_23_0_, strings0_.eName as eName1_23_1_, strings0_.owner_oid as owner_oi2_23_1_, strings0_.ownerType as ownerTyp3_23_1_, strings0_.stringValue as stringVa4_23_1_, strings0_.dynamicDef as dynamicD5_23_1_, strings0_.eType as eType6_23_1_, strings0_.valueType as valueTyp7_23_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 * select trigger0_.owner_oid as owner_oi2_18_0_, trigger0_.id as id1_38_0_, trigger0_.owner_oid as owner_oi2_38_0_, trigger0_.id as id1_38_1_, trigger0_.owner_oid as owner_oi2_38_1_, trigger0_.handlerUri as handlerU3_38_1_, trigger0_.timestampValue as timestam4_38_1_ from m_trigger trigger0_ where trigger0_.owner_oid=?
 * update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, datesCount=?, fullObject=?, longsCount=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, polysCount=?, referencesCount=?, stringsCount=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 * delete from m_task_dependent where task_oid=?
 * <p/>
 * CAN BE IMPROVED:
 * <p/>
 * select full object xml
 * update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, datesCount=?, fullObject=?, longsCount=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, polysCount=?, referencesCount=?, stringsCount=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 * <p/>
 * 12	=>	2
 *
 * @author lazyman
 */
public class TaskProgressHandler implements ModifyHandler {

    @Override
    public <T extends ObjectType> boolean canHandle(Class<T> type, String oid,
                                                    Collection<? extends ItemDelta> modifications) {
        if (!TaskType.class.equals(type) || modifications.size() != 1) {
            return false;
        }

        PropertyDelta delta = PropertyDelta.findPropertyDelta(modifications, TaskType.F_PROGRESS);
        return delta != null && delta.isReplace();
    }

    @Override
    public <T extends ObjectType> void modifyObject(Class<T> type, String oid,
                                                    Collection<? extends ItemDelta> modifications,
                                                    OperationResult result) throws ObjectNotFoundException {

    }
}
