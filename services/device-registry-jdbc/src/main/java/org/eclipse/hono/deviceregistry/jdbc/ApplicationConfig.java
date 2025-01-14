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

package org.eclipse.hono.deviceregistry.jdbc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.hono.auth.HonoPasswordEncoder;
import org.eclipse.hono.auth.SpringBasedHonoPasswordEncoder;
import org.eclipse.hono.client.HonoConnection;
import org.eclipse.hono.client.SendMessageSampler;
import org.eclipse.hono.client.kafka.CommonKafkaClientConfigProperties;
import org.eclipse.hono.client.kafka.producer.CachingKafkaProducerFactory;
import org.eclipse.hono.client.kafka.producer.KafkaProducerFactory;
import org.eclipse.hono.client.kafka.producer.MessagingKafkaProducerConfigProperties;
import org.eclipse.hono.client.notification.amqp.ProtonBasedNotificationSender;
import org.eclipse.hono.client.notification.kafka.KafkaBasedNotificationSender;
import org.eclipse.hono.client.notification.kafka.NotificationKafkaProducerConfigProperties;
import org.eclipse.hono.client.telemetry.EventSender;
import org.eclipse.hono.client.telemetry.amqp.ProtonBasedDownstreamSender;
import org.eclipse.hono.client.telemetry.kafka.KafkaBasedEventSender;
import org.eclipse.hono.client.util.MessagingClientProvider;
import org.eclipse.hono.client.util.ServiceClient;
import org.eclipse.hono.config.ApplicationConfigProperties;
import org.eclipse.hono.config.ClientConfigProperties;
import org.eclipse.hono.config.ServerConfig;
import org.eclipse.hono.config.ServiceConfigProperties;
import org.eclipse.hono.config.VertxProperties;
import org.eclipse.hono.deviceregistry.jdbc.config.DeviceServiceProperties;
import org.eclipse.hono.deviceregistry.jdbc.config.TenantServiceProperties;
import org.eclipse.hono.deviceregistry.jdbc.impl.ClasspathSchemaCreator;
import org.eclipse.hono.deviceregistry.jdbc.impl.CredentialsManagementServiceImpl;
import org.eclipse.hono.deviceregistry.jdbc.impl.CredentialsServiceImpl;
import org.eclipse.hono.deviceregistry.jdbc.impl.DeviceManagementServiceImpl;
import org.eclipse.hono.deviceregistry.jdbc.impl.NoOpSchemaCreator;
import org.eclipse.hono.deviceregistry.jdbc.impl.RegistrationServiceImpl;
import org.eclipse.hono.deviceregistry.jdbc.impl.TenantManagementServiceImpl;
import org.eclipse.hono.deviceregistry.jdbc.impl.TenantServiceImpl;
import org.eclipse.hono.deviceregistry.server.DeviceRegistryAmqpServer;
import org.eclipse.hono.deviceregistry.server.DeviceRegistryHttpServer;
import org.eclipse.hono.deviceregistry.service.credentials.AbstractCredentialsManagementService;
import org.eclipse.hono.deviceregistry.service.device.AbstractDeviceManagementService;
import org.eclipse.hono.deviceregistry.service.device.AutoProvisionerConfigProperties;
import org.eclipse.hono.deviceregistry.service.device.EdgeDeviceAutoProvisioner;
import org.eclipse.hono.deviceregistry.service.tenant.AbstractTenantManagementService;
import org.eclipse.hono.deviceregistry.service.tenant.DefaultTenantInformationService;
import org.eclipse.hono.deviceregistry.service.tenant.NoopTenantInformationService;
import org.eclipse.hono.deviceregistry.service.tenant.TenantInformationService;
import org.eclipse.hono.deviceregistry.util.ServiceClientAdapter;
import org.eclipse.hono.notification.NotificationSender;
import org.eclipse.hono.service.HealthCheckServer;
import org.eclipse.hono.service.VertxBasedHealthCheckServer;
import org.eclipse.hono.service.amqp.AbstractAmqpEndpoint;
import org.eclipse.hono.service.base.jdbc.config.JdbcDeviceStoreProperties;
import org.eclipse.hono.service.base.jdbc.config.JdbcProperties;
import org.eclipse.hono.service.base.jdbc.config.JdbcTenantStoreProperties;
import org.eclipse.hono.service.base.jdbc.store.device.DeviceStores;
import org.eclipse.hono.service.base.jdbc.store.device.TableAdapterStore;
import org.eclipse.hono.service.base.jdbc.store.device.TableManagementStore;
import org.eclipse.hono.service.base.jdbc.store.tenant.AdapterStore;
import org.eclipse.hono.service.base.jdbc.store.tenant.ManagementStore;
import org.eclipse.hono.service.base.jdbc.store.tenant.Stores;
import org.eclipse.hono.service.credentials.DelegatingCredentialsAmqpEndpoint;
import org.eclipse.hono.service.http.AbstractHttpEndpoint;
import org.eclipse.hono.service.http.HttpServiceConfigProperties;
import org.eclipse.hono.service.management.credentials.CredentialsManagementService;
import org.eclipse.hono.service.management.credentials.DelegatingCredentialsManagementHttpEndpoint;
import org.eclipse.hono.service.management.device.DelegatingDeviceManagementHttpEndpoint;
import org.eclipse.hono.service.management.device.DeviceAndGatewayAutoProvisioner;
import org.eclipse.hono.service.management.device.DeviceManagementService;
import org.eclipse.hono.service.management.tenant.DelegatingTenantManagementHttpEndpoint;
import org.eclipse.hono.service.management.tenant.TenantManagementService;
import org.eclipse.hono.service.metric.MetricsTags;
import org.eclipse.hono.service.metric.spring.PrometheusSupport;
import org.eclipse.hono.service.registration.DelegatingRegistrationAmqpEndpoint;
import org.eclipse.hono.service.tenant.DelegatingTenantAmqpEndpoint;
import org.eclipse.hono.service.tenant.TenantService;
import org.eclipse.hono.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ObjectFactoryCreatingFactoryBean;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerResolver;
import io.opentracing.noop.NoopTracerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.jdbc.JDBCAuthentication;
import io.vertx.ext.auth.jdbc.JDBCAuthenticationOptions;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;

/**
 * Spring Boot configuration for the JDBC based device registry application.
 */
@Configuration
@Import(PrometheusSupport.class)
public class ApplicationConfig {

    private static final String BEAN_NAME_AMQP_SERVER = "amqpServer";
    private static final String BEAN_NAME_HTTP_SERVER = "httpServer";

    /**
     * Exposes a Vert.x instance as a Spring bean.
     * <p>
     * This method creates new Vert.x default options and invokes
     * {@link VertxProperties#configureVertx(VertxOptions)} on the object returned
     * by {@link #vertxProperties()}.
     *
     * @return The Vert.x instance.
     */
    @Bean
    public Vertx vertx() {
        return Vertx.vertx(vertxProperties().configureVertx(new VertxOptions()));
    }

    /**
     * Exposes configuration properties for Vert.x.
     *
     * @return The properties.
     */
    @ConfigurationProperties("hono.vertx")
    @Bean
    public VertxProperties vertxProperties() {
        return new VertxProperties();
    }

    /**
     * Exposes an OpenTracing {@code Tracer} as a Spring Bean.
     * <p>
     * The Tracer will be resolved by means of a Java service lookup.
     * If no tracer can be resolved this way, the {@code NoopTracer} is
     * returned.
     *
     * @return The tracer.
     */
    @Bean
    public Tracer tracer() {
        return Optional.ofNullable(TracerResolver.resolveTracer())
                .orElse(NoopTracerFactory.create());
    }

    /**
     * Customizer for meter registry.
     *
     * @return The new meter registry customizer.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags() {
        return r -> r.config().commonTags(MetricsTags.forService(Constants.SERVICE_NAME_DEVICE_REGISTRY));
    }

    /**
     * Gets general properties for configuring the Device Registry Spring Boot application.
     *
     * @return The properties.
     */
    @Bean
    @ConfigurationProperties(prefix = "hono.app")
    public ApplicationConfigProperties applicationConfigProperties() {
        return new ApplicationConfigProperties();
    }

    /**
     * Exposes properties for configuring the health check as a Spring bean.
     *
     * @return The health check configuration properties.
     */
    @Bean
    @ConfigurationProperties(prefix = "hono.health-check")
    public ServerConfig healthCheckConfigProperties() {
        return new ServerConfig();
    }

    /**
     * Exposes the health check server as a Spring bean.
     *
     * @return The health check server.
     */
    @Bean
    public HealthCheckServer healthCheckServer() {
        return new VertxBasedHealthCheckServer(vertx(), healthCheckConfigProperties());
    }

    /**
     * Exposes a password encoder to use for encoding clear text passwords
     * and for matching password hashes.
     *
     * @return The encoder.
     */
    @Bean
    public HonoPasswordEncoder passwordEncoder() {
        return new SpringBasedHonoPasswordEncoder(deviceRegistryServiceProperties().getMaxBcryptCostfactor());
    }

    //
    //
    // JDBC store properties
    //
    //

    /**
     * Expose JDBC device registry service properties.
     *
     * @return The properties.
     */
    @Bean
    @ConfigurationProperties("hono.registry.jdbc")
    public JdbcDeviceStoreProperties devicesProperties() {
        return new JdbcDeviceStoreProperties();
    }

    /**
     * Expose JDBC tenant service properties.
     *
     * @return The properties.
     */
    @Bean
    @ConfigurationProperties("hono.tenant.jdbc")
    public JdbcTenantStoreProperties tenantsProperties() {
        return new JdbcTenantStoreProperties();
    }

    /**
     * Provider a new device backing store for the adapter facing service.
     *
     * @return A new store instance.
     * @throws IOException if reading the SQL configuration fails.
     */
    @Bean
    @Profile(Profiles.PROFILE_REGISTRY_ADAPTER)
    public TableAdapterStore devicesAdapterStore() throws IOException {
        return DeviceStores.store(vertx(), tracer(), devicesProperties(), JdbcDeviceStoreProperties::getAdapter, DeviceStores.adapterStoreFactory());
    }

    /**
     * Provider a new tenant backing store for the adapter facing service.
     *
     * @return A new store instance.
     * @throws IOException if reading the SQL configuration fails.
     */
    @Bean
    @Profile(Profiles.PROFILE_REGISTRY_ADAPTER + " & " + Profiles.PROFILE_TENANT_SERVICE)
    public AdapterStore tenantAdapterStore() throws IOException {
        return Stores.adapterStore(vertx(), tracer(), tenantsProperties().getAdapter());
    }

    /**
     * Provider a new device backing store for the management facing service.
     *
     * @return A new store instance.
     * @throws IOException if reading the SQL configuration fails.
     */
    @Bean
    @Profile(Profiles.PROFILE_REGISTRY_MANAGEMENT)
    public TableManagementStore devicesManagementStore() throws IOException {
        return DeviceStores.store(vertx(), tracer(), devicesProperties(), JdbcDeviceStoreProperties::getManagement, DeviceStores.managementStoreFactory());
    }

    /**
     * Provider a new tenant backing store for the management facing service.
     *
     * @return A new store instance.
     * @throws IOException if reading the SQL configuration fails.
     */
    @Bean
    @Profile(Profiles.PROFILE_REGISTRY_MANAGEMENT + " & " + Profiles.PROFILE_TENANT_SERVICE)
    public ManagementStore tenantManagementStore() throws IOException {
        return Stores.managementStore(vertx(), tracer(), tenantsProperties().getManagement());
    }

    //
    //
    // Service properties
    //
    //

    /**
     * Gets properties for configuring the device registry services.
     *
     * @return The properties.
     */
    @Bean
    @ConfigurationProperties("hono.registry.svc")
    public DeviceServiceProperties deviceRegistryServiceProperties() {
        return new DeviceServiceProperties();
    }

    /**
     * Gets properties for configuring the tenant services.
     *
     * @return The properties.
     */
    @Bean
    @ConfigurationProperties("hono.tenant.svc")
    @Profile(Profiles.PROFILE_TENANT_SERVICE)
    public TenantServiceProperties tenantServiceProperties() {
        return new TenantServiceProperties();
    }

    //
    //
    // AMQP endpoints
    //
    //

    /**
     * Gets properties for configuring the Device Registry's AMQP 1.0 endpoint.
     *
     * @return The properties.
     */
    @Qualifier(Constants.QUALIFIER_AMQP)
    @Bean
    @ConfigurationProperties(prefix = "hono.registry.amqp")
    @Profile(Profiles.PROFILE_REGISTRY_ADAPTER)
    public ServiceConfigProperties amqpServerProperties() {
        return new ServiceConfigProperties();
    }

    /**
     * Creates a new server for exposing the device registry's AMQP 1.0 based
     * endpoints.
     *
     * @param tenantService The tenant service instance.
     * @param registrationService The device registration service instance.
     * @param credentialsService The credentials service instance.
     * @param tenantManagementService The tenant management service instance.
     * @param deviceManagementService The device management service instance.
     * @param credentialsManagementService The credentials management service instance.
     * @param notificationSender The notification sender instance.
     * @return The server.
     */
    @Bean(name = BEAN_NAME_AMQP_SERVER)
    @Scope("prototype")
    @Profile(Profiles.PROFILE_REGISTRY_ADAPTER)
    public DeviceRegistryAmqpServer amqpServer(
            @Autowired(required = false) final TenantService tenantService,
            final RegistrationServiceImpl registrationService,
            final CredentialsServiceImpl credentialsService,
            @Autowired(required = false) final TenantManagementService tenantManagementService,
            @Autowired(required = false) final DeviceManagementService deviceManagementService,
            @Autowired(required = false) final CredentialsManagementService credentialsManagementService,
            @Autowired(required = false) final NotificationSender notificationSender) {

        final DeviceRegistryAmqpServer amqpServer = new DeviceRegistryAmqpServer();

        Optional.ofNullable(notificationSender).ifPresent(sender -> {
            amqpServer.setNotificationSender(sender);
            applyNotificationSender(sender, tenantManagementService, deviceManagementService, credentialsManagementService);
        });
        final TenantInformationService tenantInformationService = createAndApplyTenantInformationService(
                tenantManagementService, deviceManagementService, credentialsManagementService);

        if (deviceManagementService != null) {
            final var eventSenderProvider = eventSenderProvider();
            final EdgeDeviceAutoProvisioner edgeDeviceAutoProvisioner = new EdgeDeviceAutoProvisioner(
                    vertx(),
                    deviceManagementService,
                    eventSenderProvider,
                    autoProvisionerConfigProperties(),
                    tracer());
            registrationService.setEdgeDeviceAutoProvisioner(edgeDeviceAutoProvisioner);

            if (credentialsManagementService != null) {
                final DeviceAndGatewayAutoProvisioner deviceAndGatewayAutoProvisioner = new DeviceAndGatewayAutoProvisioner(
                        vertx(),
                        deviceManagementService,
                        credentialsManagementService,
                        eventSenderProvider);
                credentialsService.setDeviceAndGatewayAutoProvisioner(deviceAndGatewayAutoProvisioner);
            }
        }
        registrationService.setTenantInformationService(tenantInformationService);
        credentialsService.setTenantInformationService(tenantInformationService);

        // add endpoints
        final List<AbstractAmqpEndpoint<ServiceConfigProperties>> endpoints = new ArrayList<>();
        Optional.ofNullable(tenantService)
                .ifPresent(svc -> endpoints.add(new DelegatingTenantAmqpEndpoint<>(vertx(), svc)));
        endpoints.add(new DelegatingRegistrationAmqpEndpoint<>(vertx(), registrationService));
        endpoints.add(new DelegatingCredentialsAmqpEndpoint<>(vertx(), credentialsService));
        endpoints.forEach(ep -> {
            ep.setTracer(tracer());
            ep.setConfiguration(amqpServerProperties());
            amqpServer.addEndpoint(ep);
        });
        return amqpServer;
    }

    private void applyNotificationSender(
            final NotificationSender notificationSender,
            final TenantManagementService tenantManagementService,
            final DeviceManagementService deviceManagementService,
            final CredentialsManagementService credentialsManagementService) {

        if (tenantManagementService instanceof AbstractTenantManagementService) {
            ((AbstractTenantManagementService) tenantManagementService).setNotificationSender(notificationSender);
        }
        if (deviceManagementService instanceof AbstractDeviceManagementService) {
            ((AbstractDeviceManagementService) deviceManagementService).setNotificationSender(notificationSender);
        }
        if (credentialsManagementService instanceof AbstractCredentialsManagementService) {
            ((AbstractCredentialsManagementService) credentialsManagementService).setNotificationSender(
                    notificationSender);
        }
    }

    private TenantInformationService createAndApplyTenantInformationService(
            final TenantManagementService tenantManagementService,
            final DeviceManagementService deviceManagementService,
            final CredentialsManagementService credentialsManagementService) {

        final TenantInformationService tenantInformationService = Optional.ofNullable(tenantManagementService)
                .map(svc -> (TenantInformationService) new DefaultTenantInformationService(svc))
                .orElseGet(NoopTenantInformationService::new);
        if (deviceManagementService instanceof AbstractDeviceManagementService) {
            ((AbstractDeviceManagementService) deviceManagementService).setTenantInformationService(tenantInformationService);
        }
        if (credentialsManagementService instanceof AbstractCredentialsManagementService) {
            ((AbstractCredentialsManagementService) credentialsManagementService).setTenantInformationService(tenantInformationService);
        }
        return tenantInformationService;
    }

    /**
     * Exposes a factory for creating Device Connection service instances.
     *
     * @return The factory bean.
     */
    @Bean
    @Profile(Profiles.PROFILE_REGISTRY_ADAPTER)
    public ObjectFactoryCreatingFactoryBean amqpServerFactory() {
        final ObjectFactoryCreatingFactoryBean factory = new ObjectFactoryCreatingFactoryBean();
        factory.setTargetBeanName(BEAN_NAME_AMQP_SERVER);
        return factory;
    }

    /**
     * Gets properties for configuring gateway based auto-provisioning.
     *
     * @return The properties.
     */
    @Bean
    @ConfigurationProperties(prefix = "hono.autoprovisioning")
    public AutoProvisionerConfigProperties autoProvisionerConfigProperties() {
        return new AutoProvisionerConfigProperties();
    }

    /**
     * Creates a client for publishing events via the configured messaging systems.
     *
     * @return The client.
     */
    @Bean
    @Scope("prototype")
    public MessagingClientProvider<EventSender> eventSenderProvider() {

        final MessagingClientProvider<EventSender> result = new MessagingClientProvider<>();

        if (downstreamSenderConfig().isHostConfigured()) {
            result.setClient(new ProtonBasedDownstreamSender(
                    HonoConnection.newConnection(vertx(), downstreamSenderConfig(), tracer()),
                    SendMessageSampler.Factory.noop(),
                    true,
                    true));
        }

        if (kafkaEventConfig().isConfigured()) {
            final KafkaProducerFactory<String, Buffer> factory = CachingKafkaProducerFactory.sharedFactory(vertx());
            result.setClient(new KafkaBasedEventSender(factory, kafkaEventConfig(), true, tracer()));
        }

        healthCheckServer().registerHealthCheckResources(ServiceClientAdapter.forClient(result));
        return result;
    }

    /**
     * Exposes configuration properties for accessing the AMQP Messaging Network as a Spring bean.
     *
     * @return The properties.
     */
    @ConfigurationProperties(prefix = "hono.messaging")
    @Bean
    public ClientConfigProperties downstreamSenderConfig() {
        final ClientConfigProperties config = new ClientConfigProperties();
        config.setNameIfNotSet("Device Registry");
        config.setServerRoleIfUnknown("AMQP Messaging Network");
        return config;
    }

    /**
     * Exposes common configuration properties for a clients accessing the Kafka cluster as a Spring bean.
     *
     * @return The properties.
     */
    @ConfigurationProperties(prefix = "hono.kafka")
    @Bean
    public CommonKafkaClientConfigProperties commonKafkaClientConfig() {
        return new CommonKafkaClientConfigProperties();
    }

    /**
     * Exposes configuration properties for the Kafka producer that publishes events as a Spring bean.
     *
     * @return The properties.
     */
    @ConfigurationProperties(prefix = "hono.kafka.event")
    @Bean
    public MessagingKafkaProducerConfigProperties kafkaEventConfig() {
        final MessagingKafkaProducerConfigProperties configProperties = new MessagingKafkaProducerConfigProperties();
        configProperties.setCommonClientConfig(commonKafkaClientConfig());
        return configProperties;
    }

    /**
     * Provide a registration service.
     *
     * @param schemaCreator The schema creator.
     * @return The bean instance.
     * @throws IOException if reading the SQL configuration fails.
     */
    @Bean
    @Scope("prototype")
    @Profile(Profiles.PROFILE_REGISTRY_ADAPTER)
    public RegistrationServiceImpl registrationService(final SchemaCreator schemaCreator) throws IOException {
        return new RegistrationServiceImpl(devicesAdapterStore(), schemaCreator);
    }

    /**
     * Provide a credentials service.
     *
     * @return The bean instance.
     * @throws IOException if reading the SQL configuration fails.
     */
    @Bean
    @Scope("prototype")
    @Profile(Profiles.PROFILE_REGISTRY_ADAPTER)
    public CredentialsServiceImpl credentialsService() throws IOException {
        return new CredentialsServiceImpl(devicesAdapterStore(), deviceRegistryServiceProperties());
    }

    /**
     * Provide a tenant service.
     *
     * @return The bean instance.
     * @throws IOException if reading the SQL configuration fails.
     */
    @Bean
    @Scope("prototype")
    @Profile(Profiles.PROFILE_REGISTRY_ADAPTER + " & " + Profiles.PROFILE_TENANT_SERVICE)
    public TenantService tenantService() throws IOException {
        return new TenantServiceImpl(tenantAdapterStore(), tenantServiceProperties());
    }

    /**
     * Provide a registration management service.
     *
     * @return The bean instance.
     * @throws IOException if reading the SQL configuration fails.
     */
    @Bean
    @Scope("prototype")
    @Profile(Profiles.PROFILE_REGISTRY_MANAGEMENT)
    public DeviceManagementService registrationManagementService() throws IOException {
        return new DeviceManagementServiceImpl(devicesManagementStore(), deviceRegistryServiceProperties());
    }

    /**
     * Provide a credentials management service.
     *
     * @return The bean instance.
     * @throws IOException if reading the SQL configuration fails.
     */
    @Bean
    @Scope("prototype")
    @Profile(Profiles.PROFILE_REGISTRY_MANAGEMENT)
    public CredentialsManagementService credentialsManagementService() throws IOException {
        return new CredentialsManagementServiceImpl(vertx(), passwordEncoder(), devicesManagementStore(),
                deviceRegistryServiceProperties());
    }

    /**
     * Provide a tenant management service.
     *
     * @return The bean instance.
     * @throws IOException if reading the SQL configuration fails.
     */
    @Bean
    @Scope("prototype")
    @Profile(Profiles.PROFILE_REGISTRY_MANAGEMENT + " & " + Profiles.PROFILE_TENANT_SERVICE)
    public TenantManagementService tenantManagementService() throws IOException {
        return new TenantManagementServiceImpl(tenantManagementStore());
    }

    //
    //
    // Management endpoints
    //
    //

    /**
     * Gets properties for configuring the HTTP based Device Registry Management endpoint.
     *
     * @return The properties.
     */
    @Bean
    @ConfigurationProperties(prefix = "hono.registry.http")
    @Profile(Profiles.PROFILE_REGISTRY_MANAGEMENT)
    @Qualifier(Constants.QUALIFIER_HTTP)
    public HttpServiceConfigProperties httpServerProperties() {
        return new HttpServiceConfigProperties();
    }

    /**
     * Creates a new server for exposing the device registry's AMQP 1.0 based
     * endpoints.
     *
     * @param tenantManagementService The tenant management service instance.
     * @param deviceManagementService The device management service instance.
     * @param credentialsManagementService The credentials management service instance.
     * @param notificationSender The notification sender instance.
     * @return The server.
     */
    @Bean(name = BEAN_NAME_HTTP_SERVER)
    @Profile(Profiles.PROFILE_REGISTRY_MANAGEMENT)
    @Scope("prototype")
    public DeviceRegistryHttpServer httpServer(
            @Autowired(required = false) final TenantManagementService tenantManagementService,
            final DeviceManagementService deviceManagementService,
            final CredentialsManagementService credentialsManagementService,
            final NotificationSender notificationSender) {

        final DeviceRegistryHttpServer httpServer = new DeviceRegistryHttpServer();

        httpServer.setNotificationSender(notificationSender);
        applyNotificationSender(notificationSender, tenantManagementService, deviceManagementService,
                credentialsManagementService);
        createAndApplyTenantInformationService(tenantManagementService, deviceManagementService,
                credentialsManagementService);

        final List<AbstractHttpEndpoint<ServiceConfigProperties>> endpoints = new ArrayList<>();
        Optional.ofNullable(tenantManagementService)
                .ifPresent(svc -> endpoints.add(new DelegatingTenantManagementHttpEndpoint<>(vertx(), svc)));
        endpoints.add(new DelegatingDeviceManagementHttpEndpoint<>(vertx(), deviceManagementService));
        endpoints.add(new DelegatingCredentialsManagementHttpEndpoint<>(vertx(), credentialsManagementService));
        endpoints.forEach(ep -> {
            ep.setTracer(tracer());
            ep.setConfiguration(httpServerProperties());
            httpServer.addEndpoint(ep);
        });
        return httpServer;
    }

    /**
     * Exposes a factory for creating Device Connection service instances.
     *
     * @return The factory bean.
     */
    @Bean
    @Profile(Profiles.PROFILE_REGISTRY_MANAGEMENT)
    public ObjectFactoryCreatingFactoryBean httpServerFactory() {
        final ObjectFactoryCreatingFactoryBean factory = new ObjectFactoryCreatingFactoryBean();
        factory.setTargetBeanName(BEAN_NAME_HTTP_SERVER);
        return factory;
    }

    /**
     * Creates an authentication provider, backed by vert.x {@link io.vertx.ext.auth.jdbc.JDBCAuthentication}.
     *
     * @return The auth provider instance.
     */
    @Bean
    @Scope("prototype")
    public AuthenticationProvider authProvider() {
        final JDBCClient client = JdbcProperties.dataSource(vertx(), devicesProperties().getManagement());
        return JDBCAuthentication.create(client, new JDBCAuthenticationOptions());
    }

    /**
     * Creates a new handler supporting the Basic authentication scheme for the HTTP based
     * Device Registry Management endpoint.
     * <p>
     * This method creates a {@link BasicAuthHandler} using the authentication provider returned by
     * {@link #authProvider()} if the property corresponding to {@link HttpServiceConfigProperties#isAuthenticationRequired()}
     * is set to {@code true}.
     *
     * @param httpServiceConfigProperties The properties for configuring the HTTP based device registry
     *                                    management endpoint.
     * @return The created handler if the {@link HttpServiceConfigProperties#isAuthenticationRequired()}
     *         is {@code true} or {@code null} otherwise.
     * @see <a href="https://vertx.io/docs/vertx-auth-jdbc/java/">JDBC Auth Provider docs</a>
     */
    @Bean
    @Scope("prototype")
    public AuthenticationHandler createAuthHandler(final HttpServiceConfigProperties httpServiceConfigProperties) {
        if (httpServiceConfigProperties != null && httpServiceConfigProperties.isAuthenticationRequired()) {
            return BasicAuthHandler.create(
                    authProvider(),
                    httpServerProperties().getRealm());
        }
        return null;
    }

    /**
     * Exposes a database schema creator for device and tenant schema.
     *
     * @param vertx The Vert.x instance to use.
     * @param devicesProperties The configuration properties for the device store.
     * @param tenantsProperties The configuration properties for the tenant store.
     * @param tracer The tracer to use.
     * @return The schema creator.
     */
    @Bean
    @Profile(Profiles.PROFILE_CREATE_SCHEMA + " & " + Profiles.PROFILE_TENANT_SERVICE)
    public SchemaCreator deviceAndTenantSchemaCreator(final Vertx vertx,
            final JdbcDeviceStoreProperties devicesProperties,
            final JdbcTenantStoreProperties tenantsProperties,
            final Tracer tracer) {
        return new ClasspathSchemaCreator(vertx, tracer, devicesProperties.getAdapter(),
                tenantsProperties.getAdapter());
    }

    /**
     * Exposes a database schema creator for device schema.
     *
     * @param vertx The Vert.x instance to use.
     * @param devicesProperties The configuration properties for the device store.
     * @param tracer The tracer to use.
     * @return The schema creator.
     */
    @Bean
    @Profile(Profiles.PROFILE_CREATE_SCHEMA + " & !" + Profiles.PROFILE_TENANT_SERVICE)
    public SchemaCreator deviceSchemaCreator(final Vertx vertx,
            final JdbcDeviceStoreProperties devicesProperties,
            final Tracer tracer) {
        return new ClasspathSchemaCreator(vertx, tracer, devicesProperties.getAdapter(), null);
    }

    /**
     * Exposes a database schema creator that does nothing.
     *
     * @return The no-op schema creator.
     */
    @Bean
    @Profile("!" + Profiles.PROFILE_CREATE_SCHEMA)
    public SchemaCreator schemaCreator() {
        return new NoOpSchemaCreator();
    }

    /**
     * Exposes configuration properties for the Kafka producer that publishes notifications as a Spring bean.
     *
     * @return The properties.
     */
    @ConfigurationProperties(prefix = "hono.kafka.notification")
    @Bean
    public NotificationKafkaProducerConfigProperties notificationKafkaProducerConfig() {
        final var configProperties = new NotificationKafkaProducerConfigProperties();
        configProperties.setCommonClientConfig(commonKafkaClientConfig());
        return configProperties;
    }

    /**
     * Exposes a notification sender.
     *
     * @return The bean instance.
     */
    @Bean
    @Scope("prototype")
    @Profile(Profiles.PROFILE_REGISTRY_MANAGEMENT)
    public NotificationSender notificationSender() {
        final NotificationSender notificationSender;
        final var kafkaProducerConfig = notificationKafkaProducerConfig();
        if (kafkaProducerConfig.isConfigured()) {
            notificationSender = new KafkaBasedNotificationSender(CachingKafkaProducerFactory.sharedFactory(vertx()),
                    kafkaProducerConfig);
        } else {
            notificationSender = new ProtonBasedNotificationSender(
                    HonoConnection.newConnection(vertx(), downstreamSenderConfig(), tracer()));
        }
        if (notificationSender instanceof ServiceClient) {
            healthCheckServer()
                    .registerHealthCheckResources(ServiceClientAdapter.forClient((ServiceClient) notificationSender));
        }
        return notificationSender;
    }

}
