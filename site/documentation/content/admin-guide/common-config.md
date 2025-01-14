+++
title = "Common Configuration"
weight = 300
+++

Many Hono components support a common set of configuration options. This section
describes those options. <!--more-->

Each component which supports the following options explicitly states so. If
it doesn't, then these options are not supported by this component.


## Java VM Options

The Java VM started in Hono's components can be configured with arbitrary command line options by means of setting the
`JDK_JAVA_OPTIONS` environment variable.

| Environment Variable | Mandatory | Default | Description |
| :------------------- | :-------: | :------ | :-----------|
| `JDK_JAVA_OPTIONS`     | no        | -       | Any options that should be passed to the Java VM on the command line, e.g. `-Xmx128m` |

## Vert.x Options

The vert.x framework instance used to run Hono's components can be configured using the following environment variables
or corresponding system properties:

| OS Environment Variable<br>Java System Property | Mandatory | Default | Description                                 |
| :---------------------------------------------- | :-------: | :------ | :------------------------------------------ |
| `HONO_VERTX_DNSQUERYTIMEOUT`<br>`hono.vertx.dnsQueryTimeout` | no | `5000` | The amount of time (in milliseconds) after which a DNS query is considered to be failed. Setting this variable to a smaller value may help to reduce the time required to establish connections to the services this adapter depends on. However, setting it to a value that is too small for any DNS query to succeed will effectively prevent any connections to be established at all. |
| `HONO_VERTX_MAXEVENTLOOPEXECUTETIME`<br>`hono.vertx.maxEventLoopExecuteTime` | no | `PT2S` | The maximum duration that a task on the event loop may run without being considered to block the event loop. The value needs to be a string that represents an [ISO-8601 Duration](https://en.wikipedia.org/wiki/ISO_8601#Durations). |
| `HONO_VERTX_PREFERNATIVE`<br>`hono.vertx.preferNative`| no | `false` | Enables/disables *epoll()* support on Linux/MacOS. See the [notes below](./#epoll) for an explanation of the benefits of enabling *epoll*. It is generally safe to set this property to `true` because Netty will disable native transport if the platform doesn't support it. |

<a name="epoll"></a>
### Using Native Transport on Linux/MacOS

Using `epoll()` on Linux/MacOS may provide better performance for applications which
have a high I/O throughput. Especially when the application supports an
asynchronous I/O model. This is true for most Hono components and applications using Hono.

The *Netty* framework supports using `epoll()` on Linux/MacOS x86_64 based systems.

In order to use *epoll*, the `HONO_VERTX_PREFERNATIVE` environment variable needs to be set to `true` on startup.

## Protocol Adapter Options

### AMQP 1.0 Messaging Network Connection Configuration

Protocol adapters require a connection to the *AMQP 1.0 Messaging Network* in order to forward telemetry data and
events received from devices to downstream consumers.

The connection to the messaging network is configured according to [Hono Client Configuration]({{< relref "hono-client-configuration.md" >}})
with `HONO_MESSAGING` being used as `${PREFIX}`. Since there are no responses being received, the properties for
configuring response caching can be ignored.

### Kafka based Messaging Configuration

Protocol adapters can be configured to allow publishing messages to an *Apache Kafka&reg; cluster* instead of an
AMQP Messaging Network. Which messaging to be used, can be configured at the tenant _or_ for all tenants if a protocol
adapter is only configured with one messaging system. The connection to the Kafka cluster is configured according
to [Hono Kafka Client Configuration]({{< relref "hono-kafka-client-configuration.md" >}}).

The following table provides an overview of the prefixes to be used to individually configure the Kafka clients used by
a protocol adapter.

| OS Environment Variable Prefix<br>Java System Property Prefix | Description                  |
| :---------------------------------------------- | :----------------------------------------- |
| `HONO_KAFKA_COMMAND_CONSUMERCONFIG_`<br>`hono.kafka.command.consumerConfig.` | Configures the Kafka consumer that receives command messages. |
| `HONO_KAFKA_COMMANDINTERNAL_ADMINCLIENTCONFIG_`<br>`hono.kafka.commandInternal.adminClientConfig.` | Configures the Kafka admin client that creates Hono internal topics. |
| `HONO_KAFKA_COMMANDRESPONSE_PRODUCERCONFIG_`<br>`hono.kafka.commandResponse.producerConfig.` | Configures the Kafka producer that publishes command response messages. |
| `HONO_KAFKA_EVENT_PRODUCERCONFIG_`<br>`hono.kafka.event.producerConfig.` | Configures the Kafka producer that publishes event messages. |
| `HONO_KAFKA_TELEMETRY_PRODUCERCONFIG_`<br>`hono.kafka.telemetry.producerConfig.` | Configures the Kafka producer that publishes telemetry messages. |

### Command & Control Connection Configuration

Protocol adapters require an additional connection to the *AMQP 1.0 Messaging Network* in order to receive
commands from downstream applications and send responses to commands back to applications.

The connection is configured according to [Hono Client Configuration]({{< relref "hono-client-configuration.md" >}})
with `HONO_COMMAND` being used as `${PREFIX}`. The properties for configuring response caching can be ignored.

### Tenant Service Connection Configuration

Protocol adapters require a connection to an implementation of Hono's [Tenant API]({{< ref "/api/tenant" >}})
in order to retrieve information for a tenant.

The connection to the Tenant Service is configured according to [Hono Client Configuration]({{< relref "hono-client-configuration.md" >}})
where the `${PREFIX}` is set to `HONO_TENANT` and the additional values for response caching apply.

The adapter caches the responses from the service according to the *cache directive* included in the response.
If the response doesn't contain a *cache directive* no data will be cached.

### Device Registration Service Connection Configuration

Protocol adapters require a connection to an implementation of Hono's [Device Registration API]({{< relref "/api/device-registration" >}})
in order to retrieve registration status assertions for connected devices.

The connection to the Device Registration Service is configured according to [Hono Client Configuration]({{< relref "hono-client-configuration.md" >}})
where the `${PREFIX}` is set to `HONO_REGISTRATION`.

The adapter caches the responses from the service according to the *cache directive* included in the response.
If the response doesn't contain a *cache directive* no data will be cached.

Note that the adapter uses a single cache for all responses from the service regardless of the tenant identifier.
Consequently, the Device Registration Service client configuration's *responseCacheMinSize* and *responseCacheMaxSize* properties
determine the overall number of responses that can be cached.

### Credentials Service Connection Configuration

Protocol adapters require a connection to an implementation of Hono's [Credentials API]({{< relref "/api/credentials" >}})
in order to retrieve credentials stored for devices that needs to be authenticated. During connection establishment,
the adapter uses the Credentials API to retrieve the credentials on record for the device and matches that with the
credentials provided by a device.

The connection to the Credentials Service is configured according to [Hono Client Configuration]({{< relref "hono-client-configuration.md" >}})
where the `${PREFIX}` is set to `HONO_CREDENTIALS`.

The adapter caches the responses from the service according to the *cache directive* included in the response.
If the response doesn't contain a *cache directive* no data will be cached.

Note that the adapter uses a single cache for all responses from the service regardless of the tenant identifier.
Consequently, the Credentials Service client configuration's *responseCacheMinSize* and *responseCacheMaxSize* properties
determine the overall number of responses that can be cached.

<a name="device-connection-service-connection-configuration"></a>
### Device Connection Service Connection Configuration

{{% notice info %}}
The Device Connection service is deprecated and will be removed from Hono completely in version 2.0.0.
Protocol adapters use the [Command Router service]({{< relref "/admin-guide/command-router-config" >}}) by default and
thus require a [Command Router Service Connection]({{< relref "#command-router-service-connection-configuration" >}})
to be configured.
For backwards compatibility, adapters can still be configured to use the Device Connection service as described in this
section.
{{% /notice %}}

Protocol adapters connect to an implementation of Hono's [Device Connection API]({{< relref "/api/device-connection" >}})
in order to determine the gateway that a device is connected via to a protocol adapter. This information is required in order to
forward commands issued by applications to the protocol adapter instance that the gateway is connected to.

The connection to the Device Connection service is configured according to
[Hono Client Configuration]({{< relref "hono-client-configuration.md" >}}) where the `${PREFIX}` is set to
`HONO_DEVICECONNECTION`.

Responses from the Device Connection service are never cached, so the properties for configuring the cache are ignored.

#### Direct Connection to Data Grid

Protocol adapters can alternatively be configured to directly access a data grid for storing and retrieving
device connection information using [Infinispan](https://infinispan.org)'s Hotrod protocol.
This has the advantage of saving the network hop to the Device Connection service. However,
this is only beneficial if the Device Connection service implementation itself uses a remote service (like a data grid) for
storing the data.

The following table provides an overview of the configuration variables and corresponding system properties for configuring
the connection to the data grid:

| OS Environment Variable<br>Java System property | Mandatory | Default | Description                                |
| :------------------------------------------ | :-------: | :------ | :--------------------------------------------- |
| `HONO_DEVICECONNECTION_SERVERLIST`<br>`hono.deviceConnection.serverList` | yes | - | A list of remote servers in the form: `host1[:port][;host2[:port]]....`. |
| `HONO_DEVICECONNECTION_AUTHSERVERNAME`<br>`hono.deviceConnection.authServerName` | yes | - | The server name to indicate in the SASL handshake when authenticating to the server. |
| `HONO_DEVICECONNECTION_AUTHUSERNAME`<br>`hono.deviceConnection.authUsername` | yes | - | The username to use for authenticating to the server. |
| `HONO_DEVICECONNECTION_AUTHPASSWORD`<br>`hono.deviceConnection.authPassword` | yes | - | The password to use for authenticating to the server. |

In general, the service supports all configuration properties of the
[Infinispan Hotrod client](https://docs.jboss.org/infinispan/12.1/apidocs/org/infinispan/client/hotrod/configuration/package-summary.html#package.description)
using `hono.deviceConnection` instead of the `infinispan.client.hotrod` prefix.

<a name="command-router-service-connection-configuration"></a>
### Command Router Service Connection Configuration

{{% notice tip %}}
The Command Router service is the successor to the deprecated Device Connection service.
However, for backwards compatibility, adapters can still be configured to use the Device Connection service instead as
described [above]({{< relref "#device-connection-service-connection-configuration" >}}).
{{% /notice %}}

Protocol adapters connect to an implementation of Hono's [Command Router API]({{< relref "/api/command-router" >}})
in order to supply information with which a Command Router service component can route command & control messages to
the protocol adapters that the target devices are connected to.

The connection to the Command Router service is configured according to [Hono Client Configuration]({{< relref "hono-client-configuration.md" >}})
where the `${PREFIX}` is set to `HONO_COMMANDROUTER`.

Responses from the Command Router service are never cached, so the properties for configuring the cache are ignored.

### Resource Limits Checker Configuration

The adapter can use metrics collected by a Prometheus server to enforce certain limits set at the tenant level like
the overall number of connected devices allowed per tenant.

The following table provides an overview of the configuration variables and corresponding system properties for
configuring the checker.

| OS Environment Variable<br>Java System Property | Mandatory | Default Value | Description  |
| :---------------------------------------------- | :-------: | :------------ | :------------|
| `HONO_RESOURCELIMITS_PROMETHEUSBASED_HOST`<br>`hono.resourceLimits.prometheusBased.host` | no | `localhost` | The host name or IP address of the Prometheus server to retrieve the metrics data from. This property needs to be set in order to enable the Prometheus based checks. |
| `HONO_RESOURCELIMITS_PROMETHEUSBASED_PORT`<br>`hono.resourceLimits.prometheusBased.port` | no | `9090` | The port of the Prometheus server to retrieve metrics data from. |
| `HONO_RESOURCELIMITS_PROMETHEUSBASED_CACHEMINSIZE`<br>`hono.resourceLimits.prometheusBased.cacheMinSize` | no | `20`   | The minimum size of the cache to store the metrics data retrieved from the Prometheus server. The cache is used for storing the current amount of data exchanged with devices of tenants. |
| `HONO_RESOURCELIMITS_PROMETHEUSBASED_CACHEMAXSIZE`<br>`hono.resourceLimits.prometheusBased.cacheMaxSize` | no | `1000` | The maximum size of the cache to store the metrics data retrieved from the Prometheus server. |
| `HONO_RESOURCELIMITS_PROMETHEUSBASED_CACHETIMEOUT`<br>`hono.resourceLimits.prometheusBased.cacheTimeout` | no | `60`   | The number of seconds after which the cached metrics data should be considered invalid. |
| `HONO_RESOURCELIMITS_PROMETHEUSBASED_CONNECTTIMEOUT`<br>`hono.resourceLimits.prometheusBased.connectTimeout` | no | `1000` | The maximum number of milliseconds that the adapter waits for a TCP connection to a Prometheus server to be established.|
| `HONO_RESOURCELIMITS_PROMETHEUSBASED_QUERYTIMEOUT`<br>`hono.resourceLimits.prometheusBased.queryTimeout` | no | `500`  | The number of milliseconds after which a request to a Prometheus server is closed. Setting zero or a negative value disables the timeout.|

In addition to the properties listed above, the resource limit checker also supports the properties listed below as
documented in the [Hono Client Configuration]({{< relref "hono-client-configuration.md" >}}). These properties might
be useful if a reverse proxy in front of the Prometheus server requires the client to use TLS and/or provide credentials
for authentication.

* `HONO_RESOURCELIMITS_PROMETHEUSBASED_CREDENTIALSPATH`
* `HONO_RESOURCELIMITS_PROMETHEUSBASED_HOSTNAMEVERIFICATIONREQUIRED`
* `HONO_RESOURCELIMITS_PROMETHEUSBASED_KEYPATH`
* `HONO_RESOURCELIMITS_PROMETHEUSBASED_KEYSTOREPASSWORD`
* `HONO_RESOURCELIMITS_PROMETHEUSBASED_KEYSTOREPATH`
* `HONO_RESOURCELIMITS_PROMETHEUSBASED_PASSWORD`
* `HONO_RESOURCELIMITS_PROMETHEUSBASED_SECUREPROTOCOLS`
* `HONO_RESOURCELIMITS_PROMETHEUSBASED_TLSENABLED`
* `HONO_RESOURCELIMITS_PROMETHEUSBASED_TRUSTSTOREPATH`
* `HONO_RESOURCELIMITS_PROMETHEUSBASED_TRUSTSTOREPASSWORD`
* `HONO_RESOURCELIMITS_PROMETHEUSBASED_USERNAME`

### Connection Event Producer Configuration

Some of the protocol adapters report the establishment and termination of a connection with a device by means of a
[Connection Event Producer]({{< relref "/concepts/connection-events.md" >}}).

The producer being used by the adapter can be configured as follows:

| OS Environment Variable<br>Java System Property | Mandatory | Default Value | Description  |
| :---------------------------------------------- | :-------: | :------------ | :------------|
| `HONO_CONNECTIONEVENTS_PRODUCER`<br>`hono.connectionEvents.producer` | no | `logging` | The type of connection event producer to use for reporting the establishment/termination of device connections. Supported values are<br>`none` - No information is reported at all.<br>`logging` - All information is reported at *INFO* level via the logging framework.<br>`events` - All information is being sent downstream as [Connection Events]({{< relref "/api/event#connection-event" >}}). |
| `HONO_CONNECTIONEVENTS_LOGLEVEL`<br>`hono.connectionEvents.logLevel` | no | `info`    | The level to log connection information at. Supported values are `debug` and `info`. |

The `events` based connection event producer sets the TTL of event messages that it emits to the *max TTL*
[configured at the tenant level]({{< relref "/api/tenant#resource-limits-configuration-format" >}}).
