package org.eclipse.edc.demo.dcp.policy;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Map;

import static org.eclipse.edc.demo.dcp.policy.PolicyEvaluationExtension.MEMBERSHIP_CONSTRAINT_KEY;

public class OrganizationLocationFunction<C extends ParticipantAgentPolicyContext> extends AbstractCredentialEvaluationFunction implements AtomicConstraintRuleFunction<Permission, C> {
    private final Monitor monitor;

    private static final String ORGANIZATION_CLAIM = "organization";
    private static final String LOCATION_CLAIM = "location";

    private OrganizationLocationFunction(Monitor monitor) {
        this.monitor = monitor;
    }

    public static <C extends ParticipantAgentPolicyContext> OrganizationLocationFunction<C> create(Monitor monitor) {
        return new OrganizationLocationFunction<>(monitor) {
        };
    }

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission permission, ParticipantAgentPolicyContext  policyContext) {
        monitor.debug("OrganizationLocationFunction evaluate called");

        if (!(rightValue instanceof String)) {
            policyContext.reportProblem("Right-value expected to be String but was " + rightValue.getClass());
            monitor.severe("Right-value expected to be String but was " + rightValue.getClass());
            return false;
        }

        if (operator != Operator.EQ && operator != Operator.NEQ) {
            policyContext.reportProblem("Invalid operator %s, only %s and %s is allowed!".formatted(operator, Operator.EQ, Operator.NEQ));
            monitor.severe("Invalid operator %s, only %s and %s is allowed!".formatted(operator, Operator.EQ, Operator.NEQ));
            return false;
        }

        var participantAgent  = policyContext.participantAgent();
        if (participantAgent  == null) {
            policyContext.reportProblem("ParticipantAgent not found on PolicyContext");
            monitor.severe("ParticipantAgent not found on PolicyContext");
            return false;
        }

        var credentialResult = getCredentialList(participantAgent);
        if (credentialResult.failed()) {
            policyContext.reportProblem(credentialResult.getFailureDetail());
            monitor.severe(credentialResult.getFailureDetail());
            return false;
        }

        return credentialResult.getContent()
                .stream()
                .filter(vc -> vc.getType().stream().anyMatch(t -> t.endsWith(MEMBERSHIP_CONSTRAINT_KEY)))
                .flatMap(vc -> vc.getCredentialSubject().stream().filter(cs -> cs.getClaims().containsKey(ORGANIZATION_CLAIM)))
                .anyMatch(credential -> {
                    var organizationClaim = (Map<String, ?>) credential.getClaim(MVD_NAMESPACE, ORGANIZATION_CLAIM);
                    var locationClaim = organizationClaim.get(LOCATION_CLAIM).toString();

                    monitor.debug("Organization Location: %s".formatted(locationClaim));

                    return switch (operator) {
                        case EQ -> locationClaim.equalsIgnoreCase(rightValue.toString());
                        case NEQ -> !locationClaim.equalsIgnoreCase(rightValue.toString());
                        default -> false;
                    };
                });
    }
}
