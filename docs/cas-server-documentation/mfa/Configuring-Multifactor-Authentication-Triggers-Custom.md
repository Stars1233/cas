---
layout: default
title: CAS - Configuring Multifactor Authentication Custom Triggers
category: Multifactor Authentication
---

{% include variables.html %}

# Multifactor Authentication Custom Triggers

To create your own custom multifactor authentication trigger, you will need to design a component that is able to resolve events in the CAS authentication chain. The trigger's (i.e. event resolver's) job is to examine a set of conditions and requirements and provide an event id to CAS that would indicate the next step in the authentication flow.

A typical custom trigger, as an example, might be:

- Activate MFA provider identified by `mfa-duo` if the client browser's IP address matches the pattern `123.+`.

Note that:

- You are really not doing anything *custom* per se. All built-in CAS triggers behave in the same exact way when they attempt to resolve the next event.
- As you will observe below, the event resolution machinery is completely oblivious to multifactor authentication; all it cares about is finding the next event in the chain in a very generic way. Our custom implementation of course wants to have the next event deal with some form of MFA via a provider, but in theory we could have resolved the next event to be `hello-world`.

## Requirements

You will need to have compile-time access to the following modules in the Overlay:

{% include_cached casmodule.html group="org.apereo.cas" module="cas-server-core-webflow" %}

These are modules that ship with CAS by default and thou shall mark them with a `compile` or `provided` scope in your build configuration.

## Design Triggers

The below example demonstrates a reasonable outline of a custom event resolver:

```java
package org.apereo.cas.custom.mfa;

public class ExampleMultifactorAuthenticationTrigger implements MultifactorAuthenticationTrigger {

    @Autowired
    private CasConfigurationProperties casProperties;

   @Override
   public Optional<MultifactorAuthenticationProvider> isActivated(final Authentication authentication,
                                                                  final RegisteredService registeredService,
                                                                  final HttpServletRequest httpServletRequest,
                                                                  final Service service) {

       return Optional.empty();
   }
}
```

## Register Triggers

The event resolver trigger then needs to be registered. [See this guide](../configuration/Configuration-Management-Extensions.html) for better details.

The below example demonstrates a reasonable outline of a custom event resolver:

```java
package org.apereo.cas.custom.config;

@AutoConfiguration
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class SomethingConfiguration {
    @Bean
    public MultifactorAuthenticationTrigger exampleMultifactorAuthenticationTrigger() {
        return new ExampleMultifactorAuthenticationTrigger();
    }
    
    @Bean
    public CasWebflowEventResolver exampleMultifactorAuthenticationWebflowEventResolver(
        @Qualifier(CasDelegatingWebflowEventResolver.BEAN_NAME_INITIAL_AUTHENTICATION_EVENT_RESOLVER)
        final CasDelegatingWebflowEventResolver initialEventResolver) {
        var resolver = new DefaultMultifactorAuthenticationProviderEventResolver(
            authenticationSystemSupport.getObject(),
            centralAuthenticationService.getObject(),
            servicesManager.getObject(),
            ticketRegistrySupport.getObject(),
            warnCookieGenerator.getObject(),
            authenticationRequestServiceSelectionStrategies.getObject(),
            multifactorAuthenticationProviderSelector.getObject(),
            exampleMultifactorAuthenticationTrigger());
        initialEventResolver.addDelegate(resolver);
        return resolver;
    }
}
```

Do not forget to register the configuration class with CAS. [See this guide](../configuration/Configuration-Management-Extensions.html) for better details.
