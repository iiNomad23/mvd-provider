package org.eclipse.edc.demo.dcp.policy;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.monitor.Monitor;

public class ProcessingPurposeDutyFunction<C extends ParticipantAgentPolicyContext> extends ProcessingPurposeFunction<C, Duty> {
    private ProcessingPurposeDutyFunction(Monitor monitor) {
        super(monitor);
    }

    public static <C extends ParticipantAgentPolicyContext> ProcessingPurposeDutyFunction<C> create(Monitor monitor) {
        return new ProcessingPurposeDutyFunction<>(monitor) {
        };
    }

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Duty duty, C policyContext) {
        monitor.debug("ProcessingPurposeDutyFunction evaluate called");
        return super.evaluate(operator, rightOperand, duty, policyContext);
    }
}