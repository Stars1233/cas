package org.apereo.cas.config;

import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.authentication.attribute.AttributeDefinitionStore;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.consent.ConsentActivationStrategy;
import org.apereo.cas.consent.ConsentEngine;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.web.flow.CasWebflowConfigurer;
import org.apereo.cas.web.flow.CasWebflowExecutionPlanConfigurer;
import org.apereo.cas.web.flow.CheckConsentRequiredAction;
import org.apereo.cas.web.flow.ConfirmConsentAction;
import org.apereo.cas.web.flow.ConsentWebflowConfigurer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;
import org.springframework.webflow.execution.Action;

/**
 * This is {@link CasConsentWebflowConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnBean(name = "consentRepository")
@ConditionalOnProperty(prefix = "cas.consent.core", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration(value = "casConsentWebflowConfiguration", proxyBeanMethods = false)
public class CasConsentWebflowConfiguration {

    @ConditionalOnMissingBean(name = "checkConsentRequiredAction")
    @Bean
    @Autowired
    public Action checkConsentRequiredAction(final CasConfigurationProperties casProperties, final ConfigurableApplicationContext applicationContext,
                                             @Qualifier("attributeDefinitionStore")
                                             final AttributeDefinitionStore attributeDefinitionStore,
                                             @Qualifier("authenticationServiceSelectionPlan")
                                             final AuthenticationServiceSelectionPlan authenticationRequestServiceSelectionStrategies,
                                             @Qualifier(ConsentEngine.BEAN_NAME)
                                             final ConsentEngine consentEngine,
                                             @Qualifier(ServicesManager.BEAN_NAME)
                                             final ServicesManager servicesManager,
                                             @Qualifier("consentActivationStrategy")
                                             final ConsentActivationStrategy consentActivationStrategy) {
        return new CheckConsentRequiredAction(servicesManager, authenticationRequestServiceSelectionStrategies, consentEngine, casProperties, attributeDefinitionStore, applicationContext,
            consentActivationStrategy);
    }

    @ConditionalOnMissingBean(name = "confirmConsentAction")
    @Bean
    @Autowired
    public Action confirmConsentAction(final CasConfigurationProperties casProperties, final ConfigurableApplicationContext applicationContext,
                                       @Qualifier("attributeDefinitionStore")
                                       final AttributeDefinitionStore attributeDefinitionStore,
                                       @Qualifier("authenticationServiceSelectionPlan")
                                       final AuthenticationServiceSelectionPlan authenticationRequestServiceSelectionStrategies,
                                       @Qualifier(ConsentEngine.BEAN_NAME)
                                       final ConsentEngine consentEngine,
                                       @Qualifier(ServicesManager.BEAN_NAME)
                                       final ServicesManager servicesManager) {
        return new ConfirmConsentAction(servicesManager, authenticationRequestServiceSelectionStrategies, consentEngine, casProperties, attributeDefinitionStore, applicationContext);
    }

    @ConditionalOnMissingBean(name = "consentWebflowConfigurer")
    @Bean
    @Autowired
    public CasWebflowConfigurer consentWebflowConfigurer(final CasConfigurationProperties casProperties, final ConfigurableApplicationContext applicationContext,
                                                         @Qualifier("loginFlowRegistry")
                                                         final FlowDefinitionRegistry loginFlowDefinitionRegistry,
                                                         @Qualifier("flowBuilderServices")
                                                         final FlowBuilderServices flowBuilderServices) {
        return new ConsentWebflowConfigurer(flowBuilderServices, loginFlowDefinitionRegistry, applicationContext, casProperties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "consentCasWebflowExecutionPlanConfigurer")
    public CasWebflowExecutionPlanConfigurer consentCasWebflowExecutionPlanConfigurer(
        @Qualifier("consentWebflowConfigurer")
        final CasWebflowConfigurer consentWebflowConfigurer) {
        return plan -> plan.registerWebflowConfigurer(consentWebflowConfigurer);
    }
}
