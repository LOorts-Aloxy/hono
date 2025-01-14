/*******************************************************************************
 * Copyright (c) 2016, 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.hono.adapter.mqtt;

import org.eclipse.hono.config.ProtocolAdapterProperties;

/**
 * Properties for configuring an MQTT based protocol adapter.
 *
 */
public class MqttProtocolAdapterProperties extends ProtocolAdapterProperties {

    /**
     * The default number of milliseconds to wait for PUBACK.
     */
    protected static final int DEFAULT_COMMAND_ACK_TIMEOUT = 100;
    /**
     * The amount of time (in milliseconds) to wait for a device to acknowledge receiving a command message.
     */
    protected static final long DEFAULT_SEND_MESSAGE_TO_DEVICE_TIMEOUT = 1000L; // ms

    private int commandAckTimeout = DEFAULT_COMMAND_ACK_TIMEOUT;
    private long sendMessageToDeviceTimeout = DEFAULT_SEND_MESSAGE_TO_DEVICE_TIMEOUT;

    /**
     * Creates properties using default values.
     */
    public MqttProtocolAdapterProperties() {
        super();
    }

    /**
     * Creates properties using existing options.
     *
     * @param options The options to copy.
     */
    public MqttProtocolAdapterProperties(final MqttProtocolAdapterOptions options) {
        super(options.adapterOptions());
        setCommandAckTimeout(options.commandAckTimeout());
        setSendMessageToDeviceTimeout(options.sendMessageToDeviceTimeout());
    }

    /**
     * Gets the waiting for acknowledgement timeout in milliseconds for commands published with QoS 1.
     * <p>
     * This timeout is used by the MQTT adapter for commands published with QoS 1. If there is no acknowledgement
     * within this time limit, then the command is settled with the <em>released</em> outcome.
     * <p>
     * The default value is {@link #DEFAULT_COMMAND_ACK_TIMEOUT}.
     *
     * @deprecated Use {@link #getSendMessageToDeviceTimeout()} instead.
     *
     * @return The timeout in milliseconds.
     */
    @Deprecated(forRemoval = true)
    public final int getCommandAckTimeout() {
        return commandAckTimeout;
    }

    /**
     * Sets the waiting for acknowledgement timeout in milliseconds for commands published with QoS 1.
     * <p>
     * This timeout is used by the MQTT adapter for commands published with QoS 1. If there is no acknowledgement
     * within this time limit, then the command is settled with the <em>released</em> outcome.
     * <p>
     * The default value is {@link #DEFAULT_COMMAND_ACK_TIMEOUT}.
     *
     * @deprecated Use {@link #setSendMessageToDeviceTimeout(long)} ()} instead.
     *
     * @param timeout The timeout in milliseconds.
     * @throws IllegalArgumentException if the timeout is negative.
     */
    @Deprecated(forRemoval = true)
    public final void setCommandAckTimeout(final int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        this.commandAckTimeout = timeout;
    }

    /**
     * Gets the waiting for acknowledgement timeout in milliseconds for commands published with QoS 1.
     * <p>
     * This timeout is used by the MQTT adapter for commands published with QoS 1. If there is no acknowledgement
     * within this time limit, then the command is settled with the <em>released</em> outcome.
     * <p>
     * The default value of this property is {@link #DEFAULT_SEND_MESSAGE_TO_DEVICE_TIMEOUT}.
     *
     * @return The timeout in milliseconds.
     */
    public final long getSendMessageToDeviceTimeout() {
        return sendMessageToDeviceTimeout;
    }

    /**
     * Sets the waiting for acknowledgement timeout in milliseconds for commands published with QoS 1.
     * <p>
     * This timeout is used by the MQTT adapter for commands published with QoS 1. If there is no acknowledgement
     * within this time limit, then the command is settled with the <em>released</em> outcome.
     * <p>
     * The default value of this property is {@link #DEFAULT_SEND_MESSAGE_TO_DEVICE_TIMEOUT}.
     *
     * @param sendMessageToDeviceTimeout The timeout in milliseconds.
     * @throws IllegalArgumentException if the timeout is negative.
     */
    public final void setSendMessageToDeviceTimeout(final long sendMessageToDeviceTimeout) {
        if (sendMessageToDeviceTimeout < 0) {
            throw new IllegalArgumentException("timeout must not be negative");
        }

        this.sendMessageToDeviceTimeout = sendMessageToDeviceTimeout;
    }

    /**
     * Gets the effective timeout for waiting for acknowledgement in milliseconds for commands published with QoS 1
     * by taking the {@link #sendMessageToDeviceTimeout} and {@link #commandAckTimeout} properties into account.
     *
     * Can be removed when the deprecated {@link #commandAckTimeout} property is removed.
     *
     * @return The timeout in milliseconds.
     */
    long getEffectiveSendMessageToDeviceTimeout() {
        if (sendMessageToDeviceTimeout == DEFAULT_SEND_MESSAGE_TO_DEVICE_TIMEOUT
                && commandAckTimeout != DEFAULT_COMMAND_ACK_TIMEOUT) {
            return commandAckTimeout;
        } else {
            return sendMessageToDeviceTimeout;
        }
    }

}
