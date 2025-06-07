package org.eclipse.edc.demo.dcp.policy;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Map;

import static org.eclipse.edc.demo.dcp.policy.PolicyEvaluationExtension.MEMBERSHIP_CONSTRAINT_KEY;

public class OrganizationSizeFunction<C extends ParticipantAgentPolicyContext> extends AbstractCredentialEvaluationFunction implements AtomicConstraintRuleFunction<Permission, C> {
    private static final String ORGANIZATION_CLAIM = "organization";
    private static final String SIZE_CLAIM = "size";

    private final Monitor monitor;

    private OrganizationSizeFunction(Monitor monitor) {
        this.monitor = monitor;
    }

    public static <C extends ParticipantAgentPolicyContext> OrganizationSizeFunction<C> create(Monitor  monitor) {
        return new OrganizationSizeFunction<>(monitor) {
        };
    }

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission permission, ParticipantAgentPolicyContext  policyContext) {
        if (parseIntOrNull(rightValue.toString()) == null) {
            policyContext.reportProblem("Right-value expected to be Integer but was " + rightValue.getClass());
            monitor.severe("Right-value expected to be Integer but was " + rightValue.getClass());
            return false;
        }

        if (operator != Operator.GT) {
            policyContext.reportProblem("Invalid operator, only GT is allowed!");
            monitor.severe("Invalid operator, only GT is allowed!");
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
            return false;
        }

        return credentialResult.getContent()
                .stream()
                .filter(vc -> vc.getType().stream().anyMatch(t -> t.endsWith(MEMBERSHIP_CONSTRAINT_KEY)))
                .flatMap(vc -> vc.getCredentialSubject().stream().filter(cs -> cs.getClaims().containsKey(ORGANIZATION_CLAIM)))
                .anyMatch(credential -> {
                    var organizationClaim = (Map<String, ?>) credential.getClaim(MVD_NAMESPACE, ORGANIZATION_CLAIM);
                    var sizeClaim = Integer.parseInt(organizationClaim.get(SIZE_CLAIM).toString());
                    var isSizeGreater = sizeClaim > Integer.parseInt(rightValue.toString());

                    if (!isSizeGreater) {
                        policyContext.reportProblem("Size expected to be greater than '%s' but was '%s'".formatted(rightValue.toString(), sizeClaim));
                        monitor.severe("Size expected to be greater than '%s' but was '%s'".formatted(rightValue.toString(), sizeClaim));
                    }

                    return isSizeGreater;
                });
    }

    private Integer parseIntOrNull(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
