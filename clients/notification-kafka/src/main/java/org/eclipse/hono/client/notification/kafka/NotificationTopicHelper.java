/*
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hono.client.notification.kafka;

import org.eclipse.hono.client.kafka.HonoTopic;
import org.eclipse.hono.notification.AbstractNotification;
import org.eclipse.hono.notification.deviceregistry.AllDevicesOfTenantDeletedNotification;
import org.eclipse.hono.notification.deviceregistry.CredentialsChangeNotification;
import org.eclipse.hono.notification.deviceregistry.DeviceChangeNotification;
import org.eclipse.hono.notification.deviceregistry.TenantChangeNotification;

/**
 * Utility methods to determine the Kafka topic for a given type of notification.
 */
public final class NotificationTopicHelper {

    private static final String TENANT_CHANGE_TOPIC = getTopicNameForAddress(TenantChangeNotification.ADDRESS);
    private static final String DEVICE_CHANGE_TOPIC = getTopicNameForAddress(DeviceChangeNotification.ADDRESS);
    private static final String CREDENTIALS_CHANGE_TOPIC = getTopicNameForAddress(CredentialsChangeNotification.ADDRESS);
    private static final String ALL_DEVICES_OF_TENANT_CHANGE_TOPIC = getTopicNameForAddress(AllDevicesOfTenantDeletedNotification.ADDRESS);

    private NotificationTopicHelper() {
        // prevent instantiation
    }

    /**
     * Gets the topic name for a notification type.
     *
     * @param notificationType The class of the notification.
     * @param <T> The type of notification.
     * @return The topic name.
     * @throws IllegalArgumentException If the given type is not a known subclass of {@link AbstractNotification}.
     */
    public static <T extends AbstractNotification> String getTopicName(final Class<T> notificationType) {
        final String topic;
        if (TenantChangeNotification.class.equals(notificationType)) {
            topic = TENANT_CHANGE_TOPIC;
        } else if (DeviceChangeNotification.class.equals(notificationType)) {
            topic = DEVICE_CHANGE_TOPIC;
        } else if (CredentialsChangeNotification.class.equals(notificationType)) {
            topic = CREDENTIALS_CHANGE_TOPIC;
        } else if (AllDevicesOfTenantDeletedNotification.class.equals(notificationType)) {
            topic = ALL_DEVICES_OF_TENANT_CHANGE_TOPIC;
        } else {
            throw new IllegalArgumentException("Unknown notification type " + notificationType.getName());
        }
        return topic;
    }

    private static String getTopicNameForAddress(final String address) {
        return new HonoTopic(HonoTopic.Type.NOTIFICATION, address).toString();
    }
}
