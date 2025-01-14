/*******************************************************************************
 * Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.hono.client.telemetry.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.hono.client.kafka.producer.AbstractKafkaBasedMessageSender;
import org.eclipse.hono.client.kafka.producer.KafkaProducerFactory;
import org.eclipse.hono.client.kafka.producer.MessagingKafkaProducerConfigProperties;
import org.eclipse.hono.client.util.DownstreamMessageProperties;
import org.eclipse.hono.util.MessageHelper;
import org.eclipse.hono.util.QoS;
import org.eclipse.hono.util.RegistrationAssertion;
import org.eclipse.hono.util.TenantObject;

import io.opentracing.Tracer;
import io.vertx.core.buffer.Buffer;

/**
 * A client for publishing downstream messages to a Kafka cluster.
 */
public abstract class AbstractKafkaBasedDownstreamSender extends AbstractKafkaBasedMessageSender {

    private final boolean isDefaultsEnabled;

    /**
     * Creates a new Kafka-based downstream sender.
     *
     * @param producerFactory The factory to use for creating Kafka producers.
     * @param producerName The producer name to use.
     * @param config The Kafka producer configuration properties to use.
     * @param includeDefaults {@code true} if a device's default properties should be included in messages being sent.
     * @param tracer The OpenTracing tracer.
     * @throws NullPointerException if any of the parameters are {@code null}.
     */
    public AbstractKafkaBasedDownstreamSender(
            final KafkaProducerFactory<String, Buffer> producerFactory,
            final String producerName,
            final MessagingKafkaProducerConfigProperties config,
            final boolean includeDefaults,
            final Tracer tracer) {
        super(producerFactory, producerName, config, tracer);
        this.isDefaultsEnabled = includeDefaults;
    }

    /**
     * Adds default properties defined either at the device or tenant level are added to the message headers.
     *
     * @param endpointName The endpoint that the message is targeted at.
     * @param tenant The tenant that the device belongs to.
     * @param device The registration assertion for the device that the data originates from.
     * @param qos The delivery semantics to use for sending the data.
     * @param contentType The content type of the data. If {@code null}, the content type will be determined
     *            from the following sources (in that order, the first one that is present is used):
     *            <ol>
     *            <li>the <em>contentType</em> parameter</li>
     *            <li>the property with key {@value org.eclipse.hono.util.MessageHelper#SYS_PROPERTY_CONTENT_TYPE} in the
     *            <em>properties</em> parameter</li>
     *            <li>the device default</li>
     *            <li>the tenant default</li>
     *            <li>the default content type ({@value org.eclipse.hono.util.MessageHelper#CONTENT_TYPE_OCTET_STREAM}
     *            if payload is not {@code null}</li>
     *            </ol>
     * @param payload The data to send in the message or {@code null}.
     * @param properties Additional meta data that should be included in the downstream message.
     * @return The augmented properties.
     * @throws NullPointerException if endpoint name, tenant, device or qos are {@code null}.
     */
    protected final Map<String, Object> addDefaults(
            final String endpointName,
            final TenantObject tenant,
            final RegistrationAssertion device,
            final QoS qos,
            final String contentType,
            final Buffer payload,
            final Map<String, Object> properties) {

        Objects.requireNonNull(endpointName);
        Objects.requireNonNull(tenant);
        Objects.requireNonNull(device);
        Objects.requireNonNull(qos);

        final Map<String, Object> messageProperties = Optional.ofNullable(properties)
                .map(HashMap::new)
                .orElseGet(HashMap::new);
        messageProperties.put(MessageHelper.APP_PROPERTY_DEVICE_ID, device.getDeviceId());
        messageProperties.put(MessageHelper.APP_PROPERTY_QOS, qos.ordinal());

        final var propsWithDefaults = new DownstreamMessageProperties(
                endpointName,
                isDefaultsEnabled ? tenant.getDefaults().getMap() : null,
                isDefaultsEnabled ? device.getDefaults() : null,
                messageProperties,
                tenant.getResourceLimits())
            .asMap();

        if (contentType != null) {
            propsWithDefaults.put(MessageHelper.SYS_PROPERTY_CONTENT_TYPE, contentType);
        } else if (payload != null) {
            propsWithDefaults.putIfAbsent(
                    MessageHelper.SYS_PROPERTY_CONTENT_TYPE,
                    MessageHelper.CONTENT_TYPE_OCTET_STREAM);
        }

        return propsWithDefaults;
    }
}
