package org.eclipse.edc.demo.dcp.policy;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Map;

import static org.eclipse.edc.demo.dcp.policy.PolicyEvaluationExtension.MEMBERSHIP_CONSTRAINT_KEY;

public class OrganizationLocationFunction<C extends ParticipantAgentPolicyContext> extends AbstractCredentialEvaluationFunction implements AtomicConstraintRuleFunction<Permission, C> {
    private static final String ORGANIZATION_CLAIM = "organization";
    private static final String LOCATION_CLAIM = "location";

    private final Monitor monitor;

    private OrganizationLocationFunction(Monitor monitor) {
        this.monitor = monitor;
    }

    public static <C extends ParticipantAgentPolicyContext> OrganizationLocationFunction<C> create(Monitor monitor) {
        return new OrganizationLocationFunction<>(monitor) {
        };
    }

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission permission, ParticipantAgentPolicyContext  policyContext) {
        if (!(rightValue instanceof String)) {
            var message = "Right-value expected to be String but was " + rightValue.getClass();
            policyContext.reportProblem(message);
            monitor.debug(message);
            return false;
        }

        if (operator != Operator.EQ) {
            var message = "Invalid operator, only EQ is allowed!";
            policyContext.reportProblem(message);
            monitor.debug(message);
            return false;
        }

        var participantAgent  = policyContext.participantAgent();
        if (participantAgent  == null) {
            var message = "ParticipantAgent not found on PolicyContext";
            policyContext.reportProblem(message);
            monitor.debug(message);
            return false;
        }

        var credentialResult = getCredentialList(participantAgent);
        if (credentialResult.failed()) {
            policyContext.reportProblem(credentialResult.getFailureDetail());
            monitor.debug(credentialResult.getFailureDetail());
            return false;
        }

        return credentialResult.getContent()
                .stream()
                .filter(vc -> vc.getType().stream().anyMatch(t -> t.endsWith(MEMBERSHIP_CONSTRAINT_KEY)))
                .flatMap(vc -> vc.getCredentialSubject().stream().filter(cs -> cs.getClaims().containsKey(ORGANIZATION_CLAIM)))
                .anyMatch(credential -> {
                    var organizationClaim = (Map<String, ?>) credential.getClaim(MVD_NAMESPACE, ORGANIZATION_CLAIM);
                    var locationClaim = organizationClaim.get(LOCATION_CLAIM).toString();
                    var isLocationEqual = locationClaim.equalsIgnoreCase(rightValue.toString());

                    if (!isLocationEqual) {
                        var message = "Location expected to be '%s' but was '%s'".formatted(rightValue.toString(), locationClaim);
                        policyContext.reportProblem(message);
                        monitor.debug(message);
                    }

                    return isLocationEqual;
                });
    }
}
