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

package org.eclipse.hono.notification;

import java.time.Instant;
import java.util.Objects;

import org.eclipse.hono.annotation.HonoTimestamp;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

/**
 * A Hono internal notification that is published by one component to inform other components about events.
 *
 * Notifications are always sent as JSON.
 *
 * Subclasses must be added to {@link NotificationTypeResolver} for automatic handling of the type by Jackson.
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = NotificationConstants.JSON_FIELD_TYPE, visible = true)
@JsonTypeIdResolver(NotificationTypeResolver.class)
public abstract class AbstractNotification {

    private final String source;
    private final Instant creationTime;

    /**
     * Creates a new instance.
     *
     * @param source The canonical name of the component that publishes the notification.
     * @param creationTime The creation time of the event.
     * @throws NullPointerException If any of the parameters are {@code null}.
     */
    protected AbstractNotification(final String source, final Instant creationTime) {
        this.source = Objects.requireNonNull(source);
        this.creationTime = Objects.requireNonNull(creationTime);
    }

    /**
     * Gets the type of the notification.
     *
     * @return The type name.
     */
    @JsonIgnore
    public abstract String getType();

    /**
     * Gets the canonical name of the component that publishes the notification.
     *
     * @return The name of the component.
     */
    @JsonGetter(NotificationConstants.JSON_FIELD_SOURCE)
    public String getSource() {
        return source;
    }

    /**
     * Gets the creation time of the notification.
     *
     * @return The point in time.
     */
    @JsonGetter(NotificationConstants.JSON_FIELD_CREATION_TIME)
    @HonoTimestamp
    public Instant getCreationTime() {
        return creationTime;
    }

}
