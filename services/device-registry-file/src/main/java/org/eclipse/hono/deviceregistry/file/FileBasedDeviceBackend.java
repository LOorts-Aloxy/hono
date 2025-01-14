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
package org.eclipse.hono.deviceregistry.file;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.hono.client.ServiceInvocationException;
import org.eclipse.hono.deviceregistry.service.tenant.TenantInformationService;
import org.eclipse.hono.service.management.Filter;
import org.eclipse.hono.service.management.Id;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.Result;
import org.eclipse.hono.service.management.SearchResult;
import org.eclipse.hono.service.management.Sort;
import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.service.management.device.Device;
import org.eclipse.hono.service.management.device.DeviceAndGatewayAutoProvisioner;
import org.eclipse.hono.service.management.device.DeviceWithId;
import org.eclipse.hono.service.registration.RegistrationService;
import org.eclipse.hono.tracing.TracingHelper;
import org.eclipse.hono.util.Constants;
import org.eclipse.hono.util.CredentialsResult;
import org.eclipse.hono.util.Lifecycle;
import org.eclipse.hono.util.RegistrationResult;
import org.eclipse.hono.util.RegistryManagementConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

import io.opentracing.Span;
import io.opentracing.noop.NoopSpan;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * A device backend that keeps all data in memory but is backed by a file. This is done by leveraging and unifying
 * {@link FileBasedRegistrationService} and {@link FileBasedCredentialsService}
 */
public class FileBasedDeviceBackend implements DeviceBackend, RegistrationService, Lifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(FileBasedDeviceBackend.class);

    private final FileBasedRegistrationService registrationService;
    private final FileBasedCredentialsService credentialsService;
    private final TenantInformationService tenantInformationService;
    private DeviceAndGatewayAutoProvisioner deviceAndGatewayAutoProvisioner;

    /**
     * Create a new instance.
     *
     * @param registrationService an implementation of registration service.
     * @param credentialsService an implementation of credentials service.
     * @param tenantInformationService an implementation of tenant information service.
     * @throws NullPointerException if any of the services are {@code null}.
     */
    @Autowired
    public FileBasedDeviceBackend(
            final FileBasedRegistrationService registrationService,
            final FileBasedCredentialsService credentialsService,
            final TenantInformationService tenantInformationService) {
        Objects.requireNonNull(registrationService);
        Objects.requireNonNull(credentialsService);
        Objects.requireNonNull(tenantInformationService);

        LOG.debug("using registration service instance: {}", registrationService);
        LOG.debug("using credentials service instance: {}", credentialsService);
        LOG.debug("using tenant information service instance: {}", tenantInformationService);

        this.registrationService = registrationService;
        this.credentialsService = credentialsService;
        this.tenantInformationService = tenantInformationService;
    }

    /**
     * Sets the service to use for auto-provisioning devices and gateways.
     * <p>
     * If the service is not configured, auto-provisioning will be disabled.
     *
     * @param deviceAndGatewayAutoProvisioner The service to use for auto-provisioning devices and gateways.
     * @throws NullPointerException if the service is {@code null}.
     */
    public void setDeviceAndGatewayAutoProvisioner(
            final DeviceAndGatewayAutoProvisioner deviceAndGatewayAutoProvisioner) {
        this.deviceAndGatewayAutoProvisioner = Objects.requireNonNull(deviceAndGatewayAutoProvisioner);
    }

    @Override
    public Future<Void> start() {
        @SuppressWarnings("rawtypes")
        final List<Future> services = new ArrayList<>();

        LOG.debug("starting up services");
        services.add(registrationService.start());
        services.add(credentialsService.start());
        Optional.ofNullable(deviceAndGatewayAutoProvisioner)
                .map(DeviceAndGatewayAutoProvisioner::start)
                .ifPresent(services::add);

        return CompositeFuture.all(services)
                .mapEmpty();
    }

    @Override
    public Future<Void> stop() {
        @SuppressWarnings("rawtypes")
        final List<Future> services = new ArrayList<>();

        LOG.debug("stopping services");
        services.add(registrationService.start());
        services.add(credentialsService.start());
        Optional.ofNullable(deviceAndGatewayAutoProvisioner)
                .map(DeviceAndGatewayAutoProvisioner::start)
                .ifPresent(services::add);

        return CompositeFuture.join(services).mapEmpty();
    }

    // DEVICES

    @Override
    public Future<RegistrationResult> assertRegistration(final String tenantId, final String deviceId) {
        return registrationService.assertRegistration(tenantId, deviceId);
    }

    @Override
    public Future<RegistrationResult> assertRegistration(final String tenantId, final String deviceId,
            final String gatewayId) {
        return registrationService.assertRegistration(tenantId, deviceId, gatewayId);
    }

    @Override
    public Future<OperationResult<Device>> readDevice(final String tenantId, final String deviceId, final Span span) {
        return registrationService.readDevice(tenantId, deviceId, span);
    }

    @Override
    public Future<Result<Void>> deleteDevice(final String tenantId, final String deviceId,
            final Optional<String> resourceVersion,
            final Span span) {

        return registrationService.deleteDevice(tenantId, deviceId, resourceVersion, span)
                .compose(r -> credentialsService.remove(tenantId, deviceId, span)
                        // this means that the registry is configured to not allow modification of credentials
                        // and this has already been logged to the span
                        // it also means that modification of devices is allowed which seems to be a mis-configuration
                        // in any case, we cannot roll back the deletion of the device so we'll have to live
                        // with the "orphaned" credentials here
                        .otherwise(t -> r)
                        .map(r));
    }

    @Override
    public Future<Result<Void>> deleteDevicesOfTenant(final String tenantId, final Span span) {

        return registrationService.deleteDevicesOfTenant(tenantId, span);
    }

    @Override
    public Future<OperationResult<Id>> createDevice(
            final String tenantId,
            final Optional<String> deviceId,
            final Device device,
            final Span span) {

        return registrationService.createDevice(tenantId, deviceId, device, span)
                .compose(r -> {

                    if (r.getStatus() != HttpURLConnection.HTTP_CREATED) {
                        return Future.succeededFuture(r);
                    }

                    // now create the empty credentials set and pass on the original result
                    return credentialsService.updateCredentials(
                            tenantId,
                            r.getPayload().getId(),
                            Collections.emptyList(),
                            Optional.empty(),
                            span
                    ).map(r);

                });
    }

    @Override
    public Future<OperationResult<Id>> updateDevice(final String tenantId, final String deviceId, final Device device,
            final Optional<String> resourceVersion, final Span span) {
        return registrationService.updateDevice(tenantId, deviceId, device, resourceVersion, span);
    }

    @Override
    public Future<OperationResult<SearchResult<DeviceWithId>>> searchDevices(final String tenantId, final int pageSize,
            final int pageOffset, final List<Filter> filters, final List<Sort> sortOptions, final Span span) {
        return registrationService.searchDevices(tenantId, pageSize, pageOffset, filters, sortOptions, span);
    }

    // CREDENTIALS

    @Override
    public final Future<CredentialsResult<JsonObject>> get(final String tenantId, final String type,
            final String authId) {
        return credentialsService.get(tenantId, type, authId);
    }

    @Override
    public Future<CredentialsResult<JsonObject>> get(final String tenantId, final String type, final String authId,
            final Span span) {
        return credentialsService.get(tenantId, type, authId, span);
    }

    @Override
    public final Future<CredentialsResult<JsonObject>> get(final String tenantId, final String type,
            final String authId, final JsonObject clientContext) {
        return get(tenantId, type, authId, clientContext, NoopSpan.INSTANCE);
    }

    @Override
    public Future<CredentialsResult<JsonObject>> get(final String tenantId, final String type, final String authId,
            final JsonObject clientContext, final Span span) {

        return this.tenantInformationService.getTenant(tenantId, span)
                .compose(tenant -> credentialsService.get(tenantId, type, authId, clientContext, span)
                        .compose(credentialsResult -> {
                            if (deviceAndGatewayAutoProvisioner != null) {
                                if (credentialsResult.isNotFound()) {
                                    return deviceAndGatewayAutoProvisioner.provisionIfEnabled(
                                            tenantId,
                                            tenant,
                                            authId,
                                            clientContext,
                                            span);
                                } else if (credentialsResult.isError()) {
                                    return Future.succeededFuture(credentialsResult);
                                } else {
                                    final String deviceId = credentialsResult.getPayload()
                                            .getString(RegistryManagementConstants.FIELD_PAYLOAD_DEVICE_ID);
                                    return deviceAndGatewayAutoProvisioner
                                            .sendAutoProvisioningEventIfNeeded(tenantId, tenant, deviceId, span)
                                            .map(ok -> credentialsResult);
                                }
                            }
                            return Future.succeededFuture(credentialsResult);
                        }))
                .recover(error -> {
                    LOG.debug("error getting credentials [tenant: {}, type: {}, auth-id: {}]", tenantId, type, authId, error);
                    TracingHelper.logError(span, error);
                    final var result = CredentialsResult.from(
                            ServiceInvocationException.extractStatusCode(error),
                            new JsonObject().put(Constants.JSON_FIELD_DESCRIPTION, error.getMessage()));
                    return Future.succeededFuture(result);
                });
    }

    @Override
    public Future<OperationResult<Void>> updateCredentials(final String tenantId, final String deviceId,
            final List<CommonCredential> credentials, final Optional<String> resourceVersion,
            final Span span) {
        //TODO check if device exists
        return credentialsService.updateCredentials(tenantId, deviceId, credentials, resourceVersion, span);
    }

    @Override
    public Future<OperationResult<List<CommonCredential>>> readCredentials(
            final String tenantId,
            final String deviceId,
            final Span span) {

        return credentialsService.readCredentials(tenantId, deviceId, span)
                .recover(t -> registrationService.readDevice(tenantId, deviceId, span)
                        .map(OperationResult.ok(
                                HttpURLConnection.HTTP_OK,
                                List.of(),
                                Optional.empty(),
                                Optional.empty())));
    }

    Future<Void> saveToFile() {
        return CompositeFuture.all(
                this.registrationService.saveToFile(),
                this.credentialsService.saveToFile())
            .mapEmpty();
    }

    Future<Void> loadFromFile() {
        return CompositeFuture.all(
                this.registrationService.loadRegistrationData(),
                this.credentialsService.loadCredentials())
            .mapEmpty();
    }

    /**
     * Removes all credentials from the registry.
     */
    public void clear() {
        registrationService.clear();
        credentialsService.clear();
    }

    /**
     * Creator for {@link ToStringHelper}.
     *
     * @return A new instance for this instance.
     */
    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("credentialsService", this.credentialsService)
                .add("registrationService", this.registrationService);
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }

}
