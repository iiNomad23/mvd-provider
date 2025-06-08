package org.eclipse.edc.demo.dcp.policy;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.spi.monitor.Monitor;

public class ProcessingPurposeProhibitionFunction<C extends ParticipantAgentPolicyContext> extends ProcessingPurposeFunction<C, Prohibition> {
    private ProcessingPurposeProhibitionFunction(Monitor monitor) {
        super(monitor);
    }

    public static <C extends ParticipantAgentPolicyContext> ProcessingPurposeProhibitionFunction<C> create(Monitor monitor) {
        return new ProcessingPurposeProhibitionFunction<>(monitor) {
        };
    }

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Prohibition prohibition, C policyContext) {
        monitor.debug("ProcessingPurposeProhibitionFunction evaluate called");
        return super.evaluate(operator, rightOperand, prohibition, policyContext);
    }
}
