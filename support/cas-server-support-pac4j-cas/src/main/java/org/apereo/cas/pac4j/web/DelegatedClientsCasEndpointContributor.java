package org.apereo.cas.pac4j.web;

import org.apereo.cas.support.pac4j.authentication.clients.DelegatedClientsEndpointContributor;
import org.apereo.cas.util.CollectionUtils;
import org.pac4j.cas.client.CasClient;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.core.client.BaseClient;
import java.util.Map;

/**
 * This is {@link DelegatedClientsCasEndpointContributor}.
 *
 * @author Misagh Moayyed
 * @since 7.1.0
 */
public class DelegatedClientsCasEndpointContributor implements DelegatedClientsEndpointContributor {

    @Override
    public boolean supports(final BaseClient client) {
        return client instanceof CasClient;
    }

    @Override
    public Map<String, String> contribute(final BaseClient client) {
        return fetchCasConfiguration(((CasClient) client).getConfiguration());
    }

    protected Map<String, String> fetchCasConfiguration(final CasConfiguration configuration) {
        return CollectionUtils.wrap(
            "protocol", configuration.getProtocol(),
            "loginUrl", configuration.getLoginUrl());
    }
}
