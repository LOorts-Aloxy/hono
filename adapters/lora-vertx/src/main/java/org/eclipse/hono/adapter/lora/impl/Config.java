/*******************************************************************************
 * Copyright (c) 2019, 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.hono.adapter.lora.impl;

import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.hono.adapter.http.HttpAdapterMetrics;
import org.eclipse.hono.adapter.http.HttpProtocolAdapterProperties;
import org.eclipse.hono.adapter.http.MicrometerBasedHttpAdapterMetrics;
import org.eclipse.hono.adapter.lora.LoraProtocolAdapter;
import org.eclipse.hono.adapter.lora.providers.LoraProvider;
import org.eclipse.hono.adapter.spring.AbstractAdapterConfig;
import org.eclipse.hono.client.SendMessageSampler;
import org.eclipse.hono.service.metric.MetricsTags;
import org.eclipse.hono.util.Constants;
import org.springframework.beans.factory.config.ObjectFactoryCreatingFactoryBean;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

/**
 * Spring Boot configuration for the LoRa adapter.
 */
@Configuration
@EnableCaching
public class Config extends AbstractAdapterConfig {

    private static final String CONTAINER_ID_HONO_LORA_ADAPTER = "Hono LoRa Adapter";
    private static final String BEAN_NAME_LORA_PROTOCOL_ADAPTER = "loraProtocolAdapter";

    @PostConstruct
    void validateConfiguration() {
        final HttpProtocolAdapterProperties httpProtocolAdapterProperties = adapterProperties();
        if (!httpProtocolAdapterProperties.isAuthenticationRequired()) {
            throw new IllegalStateException(
                    "LoRa Protocol Adapter does not support unauthenticated mode. Please change your configuration accordingly.");
        }
    }

    @Bean
    HttpAdapterMetrics metrics(final MeterRegistry registry, final Vertx vertx) {
        return new MicrometerBasedHttpAdapterMetrics(registry, vertx);
    }

    /**
     * Creates a new LoRa adapter instance.
     *
     * @param samplerFactory The sampler factory to use.
     * @param metrics The component to use for reporting metrics.
     * @param providers The provider specific endpoint handlers.
     * @return The new instance.
     */
    @Bean(name = BEAN_NAME_LORA_PROTOCOL_ADAPTER)
    @Scope("prototype")
    public LoraProtocolAdapter loraProtocolAdapter(
            final SendMessageSampler.Factory samplerFactory,
            final HttpAdapterMetrics metrics,
            final List<LoraProvider> providers) {

        final LoraProtocolAdapter adapter = new LoraProtocolAdapter(WebClient.create(vertx()));
        setCollaborators(adapter, adapterProperties(), samplerFactory);
        adapter.setConfig(adapterProperties());
        adapter.setMetrics(metrics);
        adapter.setLoraProviders(providers);
        return adapter;
    }

    @Override
    public String getComponentName() {
        return CONTAINER_ID_HONO_LORA_ADAPTER;
    }

    /**
     * Exposes the LoRa adapter's configuration properties as a Spring bean.
     *
     * @return The configuration properties.
     */
    @Bean
    @ConfigurationProperties(prefix = "hono.lora")
    public HttpProtocolAdapterProperties adapterProperties() {
        return new HttpProtocolAdapterProperties();
    }

    /**
     * Exposes a factory for creating LoRa adapter instances.
     *
     * @return The factory bean.
     */
    @Bean
    public ObjectFactoryCreatingFactoryBean serviceFactory() {
        final ObjectFactoryCreatingFactoryBean factory = new ObjectFactoryCreatingFactoryBean();
        factory.setTargetBeanName(BEAN_NAME_LORA_PROTOCOL_ADAPTER);
        return factory;
    }

    /**
     * Gets the cache manager.
     *
     * @return the cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    /**
     * Customizer for meter registry.
     *
     * @return The new meter registry customizer.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags() {
        return r -> r.config().commonTags(
                MetricsTags.forProtocolAdapter(Constants.PROTOCOL_ADAPTER_TYPE_LORA));
    }

}
