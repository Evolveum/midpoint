package com.evolveum.midpoint.wf.util;

import com.evolveum.midpoint.util.SerializationUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WfProcessInstanceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WfProcessInstanceVariableType;

import java.io.IOException;
import java.io.Serializable;

/**
 * Utilities useful for working with process instance and task form variables in workflow API.
 *
 * @author mederly
 */
public class WfVariablesUtil {

    /**
     * Finds a variable among process instance variables and return its raw value (WfProcessInstanceVariableType).
     *
     * @param instance process instance to look into
     * @param varName name of the variable
     * @return variable value in the WfProcessInstanceVariableType form (non-string variables serialized into
     *         base64-encoded strings); or null if variable does not exist
     */
    public static WfProcessInstanceVariableType getVariableRawValue(WfProcessInstanceType instance, String varName) {
        for (WfProcessInstanceVariableType var : instance.getVariables()) {
            if (varName.equals(var.getName())) {
                return var;
            }
        }
        return null;
    }

    /**
     * Returns a variable value in unfolded (Object) form.
     *
     * @param instance process instance to look into
     * @param varName name of the variable
     * @param clazz expected class of the variable value
     * @return variable value in the Object form; or null if variable does not exist
     */

    public static <T extends Serializable> T getVariable(WfProcessInstanceType instance, String varName, Class<T> clazz) {
        Object value;
        WfProcessInstanceVariableType var = getVariableRawValue(instance, varName);
        if (var != null) {
            if (var.isEncoded()) {
                try {
                    value = SerializationUtil.fromString(var.getValue());
                } catch (IOException e) {
                    throw new SystemException("Couldn't decode value of variable " + varName, e);
                } catch (ClassNotFoundException e) {
                    throw new SystemException("Couldn't decode value of variable " + varName, e);
                }
            } else {
                value = var.getValue();
            }
        } else {
            return null;
        }

        try {
            return (T) value;
        } catch (ClassCastException e) {
            throw new SystemException("Couldn't retrieve value of variable " + varName + ": expected " + clazz + ", got " + value.getClass() + " instead", e);
        }
    }


    public static String getAnswer(WfProcessInstanceType instance) {
        return getVariable(instance, CommonProcessVariableNames.VARIABLE_WF_ANSWER, String.class);
    }

    public static boolean isAnswered(WfProcessInstanceType instance) {
        return getAnswer(instance) != null;
    }

    // null if not answered or answer is not true/false
    public static Boolean getAnswerAsBoolean(WfProcessInstanceType instance) {
        return CommonProcessVariableNames.approvalBooleanValue(getAnswer(instance));
    }

    public static String getWatchingTaskOid(WfProcessInstanceType instance) {
        return getVariable(instance, CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID, String.class);
    }

}
