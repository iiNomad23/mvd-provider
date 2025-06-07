/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.demo.dcp.policy;

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;

public class PolicyEvaluationExtension implements ServiceExtension {

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    public static final String MEMBERSHIP_CONSTRAINT_KEY = "MembershipCredential";
    private static final String ORGANIZATION_LOCATION_KEY = "Organization.location";
    private static final String ORGANIZATION_SIZE_KEY = "Organization.size";

    private static final String DATA_ACCESS_LEVEL_KEY = "DataAccess.level";
    private static final String DATA_ACCESS_PURPOSE = "DataAccess.purpose";

    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        registerMembershipCredentialEvaluationFunction();
        registerOrganizationLocationFunction();
        registerOrganizationSizeFunction();

        registerDataAccessLevelFunction();
        registerProcessingPurposeFunction();
    }

    private void registerMembershipCredentialEvaluationFunction() {
        bindPermissionFunction(MembershipCredentialEvaluationFunction.create(monitor), TransferProcessPolicyContext.class, TransferProcessPolicyContext.TRANSFER_SCOPE, MEMBERSHIP_CONSTRAINT_KEY);
        bindPermissionFunction(MembershipCredentialEvaluationFunction.create(monitor), ContractNegotiationPolicyContext.class, ContractNegotiationPolicyContext.NEGOTIATION_SCOPE, MEMBERSHIP_CONSTRAINT_KEY);
        bindPermissionFunction(MembershipCredentialEvaluationFunction.create(monitor), CatalogPolicyContext.class, CatalogPolicyContext.CATALOG_SCOPE, MEMBERSHIP_CONSTRAINT_KEY);
    }

    private void registerOrganizationLocationFunction() {
        bindPermissionFunction(OrganizationLocationFunction.create(monitor), TransferProcessPolicyContext.class, TransferProcessPolicyContext.TRANSFER_SCOPE, ORGANIZATION_LOCATION_KEY);
        bindPermissionFunction(OrganizationLocationFunction.create(monitor), ContractNegotiationPolicyContext.class, ContractNegotiationPolicyContext.NEGOTIATION_SCOPE, ORGANIZATION_LOCATION_KEY);
        bindPermissionFunction(OrganizationLocationFunction.create(monitor), CatalogPolicyContext.class, CatalogPolicyContext.CATALOG_SCOPE, ORGANIZATION_LOCATION_KEY);
    }

    private void registerOrganizationSizeFunction() {
        bindPermissionFunction(OrganizationSizeFunction.create(monitor), TransferProcessPolicyContext.class, TransferProcessPolicyContext.TRANSFER_SCOPE, ORGANIZATION_SIZE_KEY);
        bindPermissionFunction(OrganizationSizeFunction.create(monitor), ContractNegotiationPolicyContext.class, ContractNegotiationPolicyContext.NEGOTIATION_SCOPE, ORGANIZATION_SIZE_KEY);
        bindPermissionFunction(OrganizationSizeFunction.create(monitor), CatalogPolicyContext.class, CatalogPolicyContext.CATALOG_SCOPE, ORGANIZATION_SIZE_KEY);
    }

    private void registerDataAccessLevelFunction() {
        bindDutyFunction(DataAccessLevelFunction.create(monitor), TransferProcessPolicyContext.class, TransferProcessPolicyContext.TRANSFER_SCOPE, DATA_ACCESS_LEVEL_KEY);
        bindDutyFunction(DataAccessLevelFunction.create(monitor), ContractNegotiationPolicyContext.class, ContractNegotiationPolicyContext.NEGOTIATION_SCOPE, DATA_ACCESS_LEVEL_KEY);
        bindDutyFunction(DataAccessLevelFunction.create(monitor), CatalogPolicyContext.class, CatalogPolicyContext.CATALOG_SCOPE, DATA_ACCESS_LEVEL_KEY);
    }

    private void registerProcessingPurposeFunction() {
        bindDutyFunction(ProcessingPurposeFunction.create(monitor), TransferProcessPolicyContext.class, TransferProcessPolicyContext.TRANSFER_SCOPE, DATA_ACCESS_PURPOSE);
        bindDutyFunction(ProcessingPurposeFunction.create(monitor), ContractNegotiationPolicyContext.class, ContractNegotiationPolicyContext.NEGOTIATION_SCOPE, DATA_ACCESS_PURPOSE);
        bindDutyFunction(ProcessingPurposeFunction.create(monitor), CatalogPolicyContext.class, CatalogPolicyContext.CATALOG_SCOPE, DATA_ACCESS_PURPOSE);
    }

    private <C extends PolicyContext> void bindPermissionFunction(AtomicConstraintRuleFunction<Permission, C> function, Class<C> contextClass, String scope, String constraintType) {
        ruleBindingRegistry.bind("use", scope);
        ruleBindingRegistry.bind(ODRL_SCHEMA + "use", scope);
        ruleBindingRegistry.bind(constraintType, scope);

        policyEngine.registerFunction(contextClass, Permission.class, constraintType, function);
    }

    private <C extends PolicyContext> void bindDutyFunction(AtomicConstraintRuleFunction<Duty, C> function, Class<C> contextClass, String scope, String constraintType) {
        ruleBindingRegistry.bind("use", scope);
        ruleBindingRegistry.bind(ODRL_SCHEMA + "use", scope);
        ruleBindingRegistry.bind(constraintType, scope);

        policyEngine.registerFunction(contextClass, Duty.class, constraintType, function);
    }
}
