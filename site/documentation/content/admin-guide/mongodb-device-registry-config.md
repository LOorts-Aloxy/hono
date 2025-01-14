+++
title = "MongoDB Based Device Registry Configuration"
weight = 310
+++

The MongoDB based Device Registry component provides an implementation of Eclipse Hono&trade;'s [Device Registration]({{< relref "/api/device-registration" >}}),
[Credentials]({{< relref "/api/credentials" >}}) and [Tenant]({{< relref "/api/tenant" >}}) APIs. Protocol adapters use these APIs to determine a device's registration status, e.g. if it is enabled and if it is registered with a particular tenant, and to authenticate a device before accepting any data for processing from it. In addition to the above, this Device Registry also provides an implementation of [Device Registry Management APIs]({{< relref "/api/management" >}}) for managing tenants, registration information and credentials of devices.

The Device Registry is implemented as a Spring Boot application, and the data is persisted in a MongoDB database. It can be run either directly from the command line or by means of starting the corresponding [Docker image](https://hub.docker.com/r/eclipse/hono-service-device-registry-mongodb/) created from it.

## Service Configuration

The following table provides an overview of the configuration variables and corresponding system properties for
configuring the MongoDB based Device Registry.

| OS Environment Variable<br>Java System Property | Mandatory | Default | Description                                  |
| :---------------------------------------------- | :-------: | :------ | :------------------------------------------- |
| `HONO_CREDENTIALS_SVC_CACHEMAXAGE`<br>`hono.credentials.svc.cacheMaxAge` | no | `180` | The maximum period of time (seconds) that information returned by the service's operations may be cached for. |
| `HONO_CREDENTIALS_SVC_COLLECTIONNAME`<br>`hono.credentials.svc.collectionName` | no | `credentials` | The name of the MongoDB collection where the server stores credentials of devices.|
| `HONO_CREDENTIALS_SVC_ENCRYPTIONKEYFILE`<br>`hono.credentials.svc.encryptionKeyFile` | no | - | The path to the YAML file that [encryption keys]({{< relref "#encrypting-secrets" >}}) should be read from. |
| `HONO_CREDENTIALS_SVC_HASHALGORITHMSWHITELIST`<br>`hono.credentials.svc.hashAlgorithmsWhitelist` | no | `empty` | An array of supported hashing algorithms to be used with the `hashed-password` type of credentials. When not set, all values will be accepted. |
| `HONO_CREDENTIALS_SVC_MAXBCRYPTCOSTFACTOR`<br>`hono.credentials.svc.maxBcryptCostFactor` | no | `10` | The maximum cost factor that is supported in password hashes using the BCrypt hash function. This limit is enforced by the device registry when adding or updating corresponding credentials. Increasing this number allows for potentially more secure password hashes to be used. However, the time required to compute the hash increases exponentially with the cost factor. |
| `HONO_CREDENTIALS_SVC_MAXBCRYPTITERATIONS`<br>`hono.credentials.svc.maxBcryptIterations` | no | `10` | DEPRECATED Please use `HONO_CREDENTIALS_SVC_MAXBCRYPTCOSTFACTOR` instead.<br>The maximum cost factor that is supported in password hashes using the BCrypt hash function. This limit is enforced by the device registry when adding or updating corresponding credentials. Increasing this number allows for potentially more secure password hashes to be used. However, the time required to compute the hash increases exponentially with the cost factor. |
| `HONO_MONGODB_CONNECTIONSTRING`<br>`hono.mongodb.connectionString` | no | - | The connection string used by the Device Registry application to connect to the MongoDB database. If this property is set, it overrides the other MongoDB connection settings.<br>See [Connection String URI Format](https://docs.mongodb.com/manual/reference/connection-string/) for more information.|
| `HONO_MONGODB_CONNECTIONTIMEOUTINMS`<br>`hono.mongodb.connectionTimeoutInMs` | no | `10000` | The time in milliseconds to attempt a connection before timing out.|
| `HONO_MONGODB_DBNAME`<br>`hono.mongodb.dbName` | no | - | The name of the MongoDB database that should be used by the Device Registry application. |
| `HONO_MONGODB_HOST`<br>`hono.mongodb.host` | no | `localhost` | The host name or IP address of the MongoDB instance.|
| `HONO_MONGODB_PORT`<br>`hono.mongodb.port` | no | `27017` | The port that the MongoDB instance is listening on.|
| `HONO_MONGODB_PASSWORD`<br>`hono.mongodb.password` | no | - | The password to use for authenticating to the MongoDB instance.|
| `HONO_MONGODB_SERVERSELECTIONTIMEOUTINMS`<br>`hono.mongodb.serverSelectionTimeoutInMs` | no | `1000` | The time in milliseconds that the mongo driver will wait to select a server for an operation before raising an error.|
| `HONO_MONGODB_USERNAME`<br>`hono.mongodb.username` | no | - | The user name to use for authenticating to the MongoDB instance.|
| `HONO_REGISTRY_AMQP_BINDADDRESS`<br>`hono.registry.amqp.bindAddress` | no | `127.0.0.1` | The IP address of the network interface that the secure AMQP port should be bound to.<br>See [Port Configuration]({{< relref "#port-configuration" >}}) below for details. |
| `HONO_REGISTRY_AMQP_CERTPATH`<br>`hono.registry.amqp.certPath` | no | - | The absolute path to the PEM file containing the certificate that the server should use for authenticating to clients. This option must be used in conjunction with `HONO_REGISTRY_AMQP_KEYPATH`.<br>Alternatively, the `HONO_REGISTRY_AMQP_KEYSTOREPATH` option can be used to configure a key store containing both the key as well as the certificate. |
| `HONO_REGISTRY_AMQP_INSECUREPORT`<br>`hono.registry.amqp.insecurePort` | no | - | The insecure port the server should listen on for AMQP 1.0 connections.<br>See [Port Configuration]({{< relref "#port-configuration" >}}) below for details. |
| `HONO_REGISTRY_AMQP_INSECUREPORTBINDADDRESS`<br>`hono.registry.amqp.insecurePortBindAddress` | no | `127.0.0.1` | The IP address of the network interface that the insecure AMQP port should be bound to.<br>See [Port Configuration]({{< relref "#port-configuration" >}}) below for details. |
| `HONO_REGISTRY_AMQP_INSECUREPORTENABLED`<br>`hono.registry.amqp.insecurePortEnabled` | no | `false` | If set to `true` the server will open an insecure port (not secured by TLS) using either the port number set via `HONO_REGISTRY_AMQP_INSECUREPORT` or the default AMQP port number (`5672`) if not set explicitly.<br>See [Port Configuration]({{< relref "#port-configuration" >}}) below for details. |
| `HONO_REGISTRY_AMQP_KEYPATH`<br>`hono.registry.amqp.keyPath` | no | - | The absolute path to the (PKCS8) PEM file containing the private key that the server should use for authenticating to clients. This option must be used in conjunction with `HONO_REGISTRY_AMQP_CERTPATH`. Alternatively, the `HONO_REGISTRY_AMQP_KEYSTOREPATH` option can be used to configure a key store containing both the key as well as the certificate. |
| `HONO_REGISTRY_AMQP_KEYSTOREPASSWORD`<br>`hono.registry.amqp.keyStorePassword` | no | - | The password required to read the contents of the key store. |
| `HONO_REGISTRY_AMQP_KEYSTOREPATH`<br>`hono.registry.amqp.keyStorePath` | no | - | The absolute path to the Java key store containing the private key and certificate that the server should use for authenticating to clients. Either this option or the `HONO_REGISTRY_AMQP_KEYPATH` and `HONO_REGISTRY_AMQP_CERTPATH` options need to be set in order to enable TLS secured connections with clients. The key store format can be either `JKS` or `PKCS12` indicated by a `.jks` or `.p12` file suffix respectively. |
| `HONO_REGISTRY_AMQP_NATIVETLSREQUIRED`<br>`hono.registry.amqp.nativeTlsRequired` | no | `false` | The server will probe for OpenSLL on startup if a secure port is configured. By default, the server will fall back to the JVM's default SSL engine if not available. However, if set to `true`, the server will fail to start at all in this case. |
| `HONO_REGISTRY_AMQP_PORT`<br>`hono.registry.amqp.port` | no | `5671` | The secure port that the server should listen on for AMQP 1.0 connections.<br>See [Port Configuration]({{< relref "#port-configuration" >}}) below for details. |
| `HONO_REGISTRY_AMQP_RECEIVERLINKCREDIT`<br>`hono.registry.amqp.receiverLinkCredit` | no | `100` | The number of credits to (initially) flow to a client connecting to one of the registry's endpoints. |
| `HONO_REGISTRY_AMQP_SECUREPROTOCOLS`<br>`hono.registry.amqp.secureProtocols` | no | `TLSv1.3,TLSv1.2` | A (comma separated) list of secure protocols (in order of preference) that are supported when negotiating TLS sessions. Please refer to the [vert.x documentation](https://vertx.io/docs/vertx-core/java/#ssl) for a list of supported protocol names. |
| `HONO_REGISTRY_AMQP_SUPPORTEDCIPHERSUITES`<br>`hono.registry.amqp.supportedCipherSuites` | no | - | A (comma separated) list of names of cipher suites (in order of preference) that are supported when negotiating TLS sessions. Please refer to [JSSE Cipher Suite Names](https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#jsse-cipher-suite-names) for a list of supported names. |
| `HONO_REGISTRY_HTTP_AUTH_COLLECTIONNAME`<br>`hono.registry.http.auth.collectionName` | no | `user` | The name of the Mongo collection that contains the user accounts that are authorized to access the HTTP endpoint. Please refer to the [vert.x documentation](https://vertx.io/docs/vertx-auth-mongo/java/) for details. |
| `HONO_REGISTRY_HTTP_AUTH_PASSWORDFIELD`<br>`hono.registry.http.auth.passwordField` | no | `password` | The name of the property that contains an account's password. Please refer to the [vert.x documentation](https://vertx.io/docs/vertx-auth-mongo/java/) for details. |
| `HONO_REGISTRY_HTTP_AUTH_SALTFIELD`<br>`hono.registry.http.auth.saltField` | no | `salt` | The name of the property that contains a password hash's salt (if the `HONO_REGISTRY_HTTP_AUTH_SALTSTYLE` has value `COLUMN`). Please refer to the [vert.x documentation](https://vertx.io/docs/vertx-auth-mongo/java/) for details. |
| `HONO_REGISTRY_HTTP_AUTH_SALTSTYLE`<br>`hono.registry.http.auth.saltStyle` | no | `COLUMN` | The strategy to use for storing password salt values. Please refer to the [vert.x documentation](https://vertx.io/docs/vertx-auth-mongo/java/) for details. |
| `HONO_REGISTRY_HTTP_AUTH_USERNAMEFIELD`<br>`hono.registry.http.auth.usernameField` | no | `username` | The name of the property that contains an account's user name. Please refer to the [vert.x documentation](https://vertx.io/docs/vertx-auth-mongo/java/) for details. |
| `HONO_REGISTRY_HTTP_AUTHENTICATIONREQUIRED`<br>`hono.registry.http.authenticationRequired` | no | `true` | If set to `true` the HTTP endpoint of the Device Registry requires clients to authenticate when connecting to the Device Registry. The MongoDB based Device Registry currently supports basic authentication with user credentials being read from a Mongo collection defined by `HONO_REGISTRY_HTTP_AUTH_COLLECTIONNAME`. <br>For more information on how to manage users please refer to [Mongo Auth Provider](https://vertx.io/docs/vertx-auth-mongo/java/). |
| `HONO_REGISTRY_HTTP_BINDADDRESS`<br>`hono.registry.http.bindAddress` | no | `127.0.0.1` | The IP address of the network interface that the secure HTTP port should be bound to.<br>See [Port Configuration]({{< relref "#port-configuration" >}}) below for details. |
| `HONO_REGISTRY_HTTP_CERTPATH`<br>`hono.registry.http.certPath` | no | - | The absolute path to the PEM file containing the certificate that the server should use for authenticating to clients. This option must be used in conjunction with `HONO_REGISTRY_HTTP_KEYPATH`.<br>Alternatively, the `HONO_REGISTRY_HTTP_KEYSTOREPATH` option can be used to configure a key store containing both the key as well as the certificate. |
| `HONO_REGISTRY_HTTP_DEVICEIDPATTERN`<br>`hono.registry.http.deviceIdPattern` | no | `^[a-zA-Z0-9-_\.:]+$` | The regular expression to use to validate device ID. Please refer to the [java pattern documentation](https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html). |
| `HONO_REGISTRY_HTTP_INSECUREPORT`<br>`hono.registry.http.insecurePort` | no | - | The insecure port the server should listen on for HTTP requests.<br>See [Port Configuration]({{< relref "#port-configuration" >}}) below for details. |
| `HONO_REGISTRY_HTTP_INSECUREPORTBINDADDRESS`<br>`hono.registry.http.insecurePortBindAddress` | no | `127.0.0.1` | The IP address of the network interface that the insecure HTTP port should be bound to.<br>See [Port Configuration]({{< relref "#port-configuration" >}}) below for details. |
| `HONO_REGISTRY_HTTP_INSECUREPORTENABLED`<br>`hono.registry.http.insecurePortEnabled` | no | `false` | If set to `true` the server will open an insecure port (not secured by TLS) using either the port number set via `HONO_REGISTRY_HTTP_INSECUREPORT` or the default AMQP port number (`5672`) if not set explicitly.<br>See [Port Configuration]({{< relref "#port-configuration" >}}) below for details. |
| `HONO_REGISTRY_HTTP_KEYPATH`<br>`hono.registry.http.keyPath` | no | - | The absolute path to the (PKCS8) PEM file containing the private key that the server should use for authenticating to clients. This option must be used in conjunction with `HONO_REGISTRY_HTTP_CERTPATH`. Alternatively, the `HONO_REGISTRY_HTTP_KEYSTOREPATH` option can be used to configure a key store containing both the key as well as the certificate. |
| `HONO_REGISTRY_HTTP_KEYSTOREPASSWORD`<br>`hono.registry.http.keyStorePassword` | no | - | The password required to read the contents of the key store. |
| `HONO_REGISTRY_HTTP_KEYSTOREPATH`<br>`hono.registry.http.keyStorePath` | no | - | The absolute path to the Java key store containing the private key and certificate that the server should use for authenticating to clients. Either this option or the `HONO_REGISTRY_HTTP_KEYPATH` and `HONO_REGISTRY_HTTP_CERTPATH` options need to be set in order to enable TLS secured connections with clients. The key store format can be either `JKS` or `PKCS12` indicated by a `.jks` or `.p12` file suffix respectively. |
| `HONO_REGISTRY_HTTP_MAXPAYLOADSIZE`<br>`hono.registry.http.maxPayloadSize` | no | `16000` | The maximum size of an HTTP request body in bytes that is accepted by the registry. |
| `HONO_REGISTRY_HTTP_PORT`<br>`hono.registry.http.port` | no | `5671` | The secure port that the server should listen on for HTTP requests.<br>See [Port Configuration]({{< relref "#port-configuration" >}}) below for details. |
| `HONO_REGISTRY_HTTP_SECUREPROTOCOLS`<br>`hono.registry.http.secureProtocols` | no | `TLSv1.3,TLSv1.2` | A (comma separated) list of secure protocols (in order of preference) that are supported when negotiating TLS sessions. Please refer to the [vert.x documentation](https://vertx.io/docs/vertx-core/java/#ssl) for a list of supported protocol names. |
| `HONO_REGISTRY_HTTP_SUPPORTEDCIPHERSUITES`<br>`hono.registry.http.supportedCipherSuites` | no | - | A (comma separated) list of names of cipher suites (in order of preference) that are supported when negotiating TLS sessions. Please refer to [JSSE Cipher Suite Names](https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#jsse-cipher-suite-names) for a list of supported names. |
| `HONO_REGISTRY_HTTP_TENANTIDPATTERN`<br>`hono.registry.http.tenantIdPattern` | no | `^[a-zA-Z0-9-_\.]+$` | The regular expression to use to validate tenant ID. Please refer to the [java pattern documentation](https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html). |
| `HONO_REGISTRY_SVC_CACHEMAXAGE`<br>`hono.registry.svc.cacheMaxAge` | no | `180` | The maximum period of time (seconds) that information returned by the service's operations may be cached for. |
| `HONO_REGISTRY_SVC_COLLECTIONNAME`<br>`hono.registry.svc.collectionName` | no | `devices` | The name of the MongoDB collection where the server stores registered device information.|
| `HONO_REGISTRY_SVC_MAXDEVICESPERTENANT`<br>`hono.registry.svc.maxDevicesPerTenant` | no | `-1` | The number of devices that can be registered for each tenant. It is an error to set this property to a value < -1. The value `-1` indicates that no limit is set.|
| `HONO_REGISTRY_SVC_USERNAMEPATTERN`<br>`hono.registry.svc.usernamePattern` | no | `^[a-zA-Z0-9-_=\\.]+$` | The regular expression to use for validating authentication identifiers (user names) of hashed-password credentials. |
| `HONO_TENANT_SVC_CACHEMAXAGE`<br>`hono.tenant.svc.cacheMaxAge` | no | `180` | The maximum period of time (seconds) that information returned by the service's operations may be cached for. |
| `HONO_TENANT_SVC_COLLECTIONNAME`<br>`hono.tenant.svc.collectionName` | no | `tenants` | The name of the MongoDB collection where the server stores tenants information.|

The variables only need to be set if the default value does not match your environment.

In addition to the options described in the table above, this component supports the following standard configuration
options:

* [Common Java VM Options]({{< relref "common-config.md/#java-vm-options" >}})
* [Common vert.x Options]({{< relref "common-config.md/#vertx-options" >}})
* [Monitoring Options]({{< relref "monitoring-tracing-config.md" >}})

## Port Configuration

The Device Registry supports configuration of both, an AMQP based endpoint as well as an HTTP based endpoint proving RESTful resources for managing registration information and credentials. Both endpoints can be configured to listen for connections on

* a secure port only (default) or
* an insecure port only or
* both a secure and an insecure port (dual port configuration)

See [Port Configuration]({{< relref "file-based-device-registry-config#port-configuration" >}}) for more information. 

{{% notice tip %}}
The environment variables to use for configuring the REST endpoint are the same as the ones for the AMQP endpoint, substituting `_AMQP_` with `_HTTP_`.
{{% /notice %}}

## Authentication Service Connection Configuration

See [Authentication Service Connection Configuration]({{< relref "file-based-device-registry-config#authentication-service-connection-configuration" >}}) for more information.

## Metrics Configuration

See [Monitoring & Tracing Admin Guide]({{< relref "monitoring-tracing-config.md" >}}) for details on how to configure the reporting of metrics.

## Encrypting Secrets

Hono's CoAP protocol adapter supports authentication of devices during the DTLS handshake based on a *pre-shared key*.
A pre-shared key is an arbitrary sequence of bytes which both the device as well as the protocol adapter need to present
during the handshake in order to prove their identity.
The Mongo DB based registry implementation supports managing these keys by means of PSK credentials which can be set for
devices. By default, the bytes representing the key are stored as a base64 encoded string property of the credentials
document. In order to better protect these keys from unintended disclosure, the registry can be configured to encrypt
these keys before they are written to the database collection. During read operations the keys are then decrypted
again before they are returned to an authorized client.

In order to activate this transparent encryption/decryption, the `HONO_CREDENTIALS_SVC_ENCRYPTIONKEYFILE` configuration
variable needs to be set to the path to a YAML file containing the definition of the symmetric keys that should be used
for encryption. The file is expected to have the following format:

```yaml
defaultKey: 2
keys:
- version: 1
  key: hqHKBLV83LpCqzKpf8OvutbCs+O5wX5BPu3btWpEvXA=
- version: 2
  key: ge2L+MA9jLA8UiUJ4z5fUoK+Lgj2yddlL6EzYIBqb1Q=
```

The file needs to contain at least all versions of the key that values in the database collection have been encrypted with.
Otherwise the registry will not be able to decrypt these values during reading. The `defaultKey` property indicates the
version of the key that should be used when encrypting values. Please refer to the
[CryptVault project](https://github.com/bolcom/cryptvault) for additional information regarding key rotation.
