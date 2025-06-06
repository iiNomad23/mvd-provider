package org.eclipse.edc.demo.dcp.policy;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;

import java.util.Map;

import static org.eclipse.edc.demo.dcp.policy.PolicyEvaluationExtension.MEMBERSHIP_CONSTRAINT_KEY;

public class OrganizationSizeFunction<C extends ParticipantAgentPolicyContext> extends AbstractCredentialEvaluationFunction implements AtomicConstraintRuleFunction<Permission, C> {
    private static final String ORGANIZATION_CLAIM = "organization";
    private static final String SIZE_CLAIM = "size";

    private OrganizationSizeFunction() { }

    public static <C extends ParticipantAgentPolicyContext> OrganizationSizeFunction<C> create() {
        return new OrganizationSizeFunction<>() {
        };
    }

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission permission, ParticipantAgentPolicyContext  policyContext) {
        if (!(rightValue instanceof Integer)) {
            policyContext.reportProblem("Right-value expected to be Integer but was " + rightValue.getClass());
            return false;
        }

        if (operator != Operator.GT) {
            policyContext.reportProblem("Invalid operator, only GT is allowed!");
            return false;
        }

        var participantAgent  = policyContext.participantAgent();
        if (participantAgent  == null) {
            policyContext.reportProblem("ParticipantAgent not found on PolicyContext");
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
                    var sizeClaim = organizationClaim.get(SIZE_CLAIM).toString();
                    var isLocationEqual = sizeClaim.equalsIgnoreCase(rightValue.toString());

                    if (!isLocationEqual) {
                        policyContext.reportProblem("Size expected to be '%s' but was '%s'".formatted(rightValue.toString(), sizeClaim));
                    }

                    return isLocationEqual;
                });
    }
}
