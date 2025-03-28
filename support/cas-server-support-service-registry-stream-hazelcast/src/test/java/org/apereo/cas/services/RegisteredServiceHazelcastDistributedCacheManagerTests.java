package org.apereo.cas.services;

import org.apereo.cas.configuration.model.support.hazelcast.BaseHazelcastProperties;
import org.apereo.cas.hz.HazelcastConfigurationFactory;
import org.apereo.cas.services.publisher.DefaultCasRegisteredServiceStreamPublisher;
import org.apereo.cas.support.events.service.CasRegisteredServiceDeletedEvent;
import org.apereo.cas.support.events.service.CasRegisteredServiceLoadedEvent;
import org.apereo.cas.support.events.service.CasRegisteredServiceSavedEvent;
import org.apereo.cas.test.CasTestExtension;
import org.apereo.cas.util.PublisherIdentifier;
import org.apereo.cas.util.cache.DistributedCacheObject;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.HazelcastInstanceFactory;
import lombok.val;
import org.apereo.inspektr.common.web.ClientInfoHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link RegisteredServiceHazelcastDistributedCacheManagerTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@Tag("Hazelcast")
@ExtendWith(CasTestExtension.class)
class RegisteredServiceHazelcastDistributedCacheManagerTests {
    private HazelcastInstance hz;

    private RegisteredServiceHazelcastDistributedCacheManager mgr;

    @BeforeEach
    void initialize() {
        val properties = new BaseHazelcastProperties();
        properties.getCluster().getCore().setInstanceName(getClass().getSimpleName());
        val config = HazelcastConfigurationFactory.build(properties,
            HazelcastConfigurationFactory.buildMapConfig(properties, "cache", 10));
        this.hz = HazelcastInstanceFactory.getOrCreateHazelcastInstance(config);
        mgr = new RegisteredServiceHazelcastDistributedCacheManager(this.hz, hz.getMap("cache"));
    }

    @AfterEach
    public void shutdown() {
        this.hz.shutdown();
    }

    @Test
    void verifyAction() {
        val registeredService = RegisteredServiceTestUtils.getRegisteredService();
        var obj = mgr.get(registeredService);
        assertNull(obj);
        assertFalse(mgr.contains(registeredService));

        val id = new PublisherIdentifier();
        val cache = DistributedCacheObject.<RegisteredService>builder()
            .value(registeredService)
            .publisherIdentifier(id)
            .build();

        mgr.set(registeredService, cache, true);
        assertFalse(mgr.getAll().isEmpty());
        obj = mgr.get(registeredService);
        assertNotNull(obj);
        val result = mgr.findAll(obj1 -> obj1.getValue().equals(registeredService));
        assertFalse(result.isEmpty());
        mgr.remove(registeredService, cache, true);
        assertTrue(mgr.getAll().isEmpty());
    }

    @Test
    void verifyPublisher() {
        val registeredService = RegisteredServiceTestUtils.getRegisteredService();
        val casRegisteredServiceStreamPublisherIdentifier = new PublisherIdentifier("123456");
        
        val publisher = new DefaultCasRegisteredServiceStreamPublisher(mgr);
        val clientInfo = ClientInfoHolder.getClientInfo();
        publisher.publish(registeredService,
            new CasRegisteredServiceDeletedEvent(this, registeredService, clientInfo),
            casRegisteredServiceStreamPublisherIdentifier);
        publisher.publish(registeredService,
            new CasRegisteredServiceSavedEvent(this, registeredService, clientInfo),
            casRegisteredServiceStreamPublisherIdentifier);
        publisher.publish(registeredService,
            new CasRegisteredServiceLoadedEvent(this, registeredService, clientInfo),
            casRegisteredServiceStreamPublisherIdentifier);
        assertFalse(mgr.getAll().isEmpty());
    }
}
