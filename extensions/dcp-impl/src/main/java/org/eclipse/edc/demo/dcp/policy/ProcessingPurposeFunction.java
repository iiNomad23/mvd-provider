package org.eclipse.edc.demo.dcp.policy;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.monitor.Monitor;

public abstract class ProcessingPurposeFunction<C extends ParticipantAgentPolicyContext, R extends Rule> extends AbstractCredentialEvaluationFunction implements AtomicConstraintRuleFunction<R, C> {
    protected final Monitor monitor;

    private static final String DATAPROCESSOR_CRED_TYPE = "DataProcessorCredential";
    private static final String PURPOSE_CLAIM = "purpose";

    protected ProcessingPurposeFunction(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, R rule, C policyContext) {
        if (!operator.equals(Operator.EQ)) {
            policyContext.reportProblem("Cannot evaluate operator %s, only %s is supported".formatted(operator, Operator.EQ));
            monitor.severe("Cannot evaluate operator %s, only %s is supported".formatted(operator, Operator.EQ));
            return false;
        }

        var pa = policyContext.participantAgent();
        if (pa == null) {
            policyContext.reportProblem("ParticipantAgent not found on PolicyContext");
            monitor.severe("ParticipantAgent not found on PolicyContext");
            return false;
        }

        var credentialResult = getCredentialList(pa);
        if (credentialResult.failed()) {
            policyContext.reportProblem(credentialResult.getFailureDetail());
            monitor.severe(credentialResult.getFailureDetail());
            return false;
        }

        return credentialResult.getContent()
                .stream()
                .filter(vc -> vc.getType().stream().anyMatch(t -> t.endsWith(DATAPROCESSOR_CRED_TYPE)))
                .flatMap(credential -> credential.getCredentialSubject().stream())
                .anyMatch(credentialSubject -> {
                    var purpose = credentialSubject.getClaim(MVD_NAMESPACE, PURPOSE_CLAIM).toString();

                    monitor.debug("Purpose: %s".formatted(purpose));

                    return purpose.equals(rightOperand.toString());
                });
    }
}
