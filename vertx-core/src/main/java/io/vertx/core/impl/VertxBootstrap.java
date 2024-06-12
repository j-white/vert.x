/*
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.core.impl;

import io.vertx.core.*;
import io.vertx.core.impl.transports.EpollTransport;
import io.vertx.core.impl.transports.JDKTransport;
import io.vertx.core.impl.transports.KQueueTransport;
import io.vertx.core.spi.*;
import io.vertx.core.spi.file.FileResolver;
import io.vertx.core.file.impl.FileResolverImpl;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.transport.Transport;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.core.spi.cluster.NodeSelector;
import io.vertx.core.spi.cluster.impl.DefaultNodeSelector;
import io.vertx.core.spi.metrics.VertxMetrics;
import io.vertx.core.spi.tracing.VertxTracer;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Vertx builder for creating vertx instances with SPI overrides.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxBootstrap implements VertxFactory {

  private static final Logger log = LoggerFactory.getLogger(VertxFactory.class);

  private VertxOptions options = new VertxOptions();
  private JsonObject config;
  private Transport transport;
  private ClusterManager clusterManager;
  private NodeSelector clusterNodeSelector;
  private VertxTracerFactory tracerFactory;
  private VertxTracer tracer;
  private VertxThreadFactory threadFactory;
  private ExecutorServiceFactory executorServiceFactory;
  private VertxMetricsFactory metricsFactory;
  private VertxMetrics metrics;
  private FileResolver fileResolver;

  /**
   * @return the vertx options
   */
  public VertxOptions options() {
    return options;
  }

  @Override
  public VertxBootstrap options(VertxOptions options) {
    this.options = options;
    return this;
  }

  /**
   * @return the optional config when instantiated from the command line or {@code null}
   */
  public JsonObject config() {
    return config;
  }

  /**
   * @return the transport to use
   */
  public Transport findTransport() {
    return transport;
  }

  /**
   * Set the transport to for building Vertx.
   * @param transport the transport
   * @return this builder instance
   */
  public VertxBootstrap findTransport(Transport transport) {
    this.transport = transport;
    return this;
  }

  /**
   * @return the cluster manager to use
   */
  public ClusterManager clusterManager() {
    return clusterManager;
  }

  /**
   * Set the cluster manager to use.
   * @param clusterManager the cluster manager
   * @return this builder instance
   */
  public VertxBootstrap clusterManager(ClusterManager clusterManager) {
    this.clusterManager = clusterManager;
    return this;
  }

  public VertxBootstrap metricsFactory(VertxMetricsFactory factory) {
    this.metricsFactory = factory;
    return this;
  }

  /**
   * @return the node selector to use
   */
  public NodeSelector clusterNodeSelector() {
    return clusterNodeSelector;
  }

  /**
   * Set the cluster node selector to use.
   * @param selector the selector
   * @return this builder instance
   */
  public VertxBootstrap clusterNodeSelector(NodeSelector selector) {
    this.clusterNodeSelector = selector;
    return this;
  }

  public VertxBootstrap tracerFactory(VertxTracerFactory factory) {
    this.tracerFactory = factory;
    return this;
  }

  /**
   * @return the tracer instance to use
   */
  public VertxTracer tracer() {
    return tracer;
  }

  /**
   * Set the tracer to use.
   * @param tracer the tracer
   * @return this builder instance
   */
  public VertxBootstrap tracer(VertxTracer tracer) {
    this.tracer = tracer;
    return this;
  }

  /**
   * @return the metrics instance to use
   */
  public VertxMetrics metrics() {
    return metrics;
  }

  /**
   * Set the metrics instance to use.
   * @param metrics the metrics
   * @return this builder instance
   */
  public VertxBootstrap metrics(VertxMetrics metrics) {
    this.metrics = metrics;
    return this;
  }

  /**
   * @return the {@code FileResolver} instance to use
   */
  public FileResolver fileResolver() {
    return fileResolver;
  }

  /**
   * Set the {@code FileResolver} instance to use.
   * @param resolver the file resolver
   * @return this builder instance
   */
  public VertxBootstrap fileResolver(FileResolver resolver) {
    this.fileResolver = resolver;
    return this;
  }

  /**
   * @return the {@code VertxThreadFactory} to use
   */
  public VertxThreadFactory threadFactory() {
    return threadFactory;
  }

  /**
   * Set the {@code VertxThreadFactory} instance to use.
   * @param factory the metrics
   * @return this builder instance
   */
  public VertxBootstrap threadFactory(VertxThreadFactory factory) {
    this.threadFactory = factory;
    return this;
  }

  /**
   * @return the {@code ExecutorServiceFactory} to use
   */
  public ExecutorServiceFactory executorServiceFactory() {
    return executorServiceFactory;
  }

  /**
   * Set the {@code ExecutorServiceFactory} instance to use.
   * @param factory the factory
   * @return this builder instance
   */
  public VertxBootstrap executorServiceFactory(ExecutorServiceFactory factory) {
    this.executorServiceFactory = factory;
    return this;
  }

  /**
   * Build and return the vertx instance
   */
  public Vertx vertx() {
    checkBeforeInstantiating();
    VertxImpl vertx = new VertxImpl(
      options,
      null,
      null,
      metrics,
      tracer,
      transport,
      fileResolver,
      threadFactory,
      executorServiceFactory);
    vertx.init();
    return vertx;
  }

  /**
   * Build and return the clustered vertx instance
   */
  public Future<Vertx> clusteredVertx() {
    checkBeforeInstantiating();
    if (clusterManager == null) {
      throw new IllegalStateException("No ClusterManagerFactory instances found on classpath");
    }
    VertxImpl vertx = new VertxImpl(
      options,
      clusterManager,
      clusterNodeSelector == null ? new DefaultNodeSelector() : clusterNodeSelector,
      metrics,
      tracer,
      transport,
      fileResolver,
      threadFactory,
      executorServiceFactory);
    return vertx.initClustered(options);
  }

  /**
   * Initialize the service providers.
   * @return this builder instance
   */
  public VertxBootstrap init() {
    initTransport();
    initMetrics();
    initTracing();
    List<VertxServiceProvider> providers = ServiceHelper.loadFactories(VertxServiceProvider.class);
    initProviders(providers);
    initThreadFactory();
    initExecutorServiceFactory();
    initFileResolver();
    return this;
  }

  private void initProviders(Collection<VertxServiceProvider> providers) {
    for (VertxServiceProvider provider : providers) {
      if (provider instanceof VertxMetricsFactory && (options.getMetricsOptions() == null || !options.getMetricsOptions().isEnabled())) {
        continue;
      } else if (provider instanceof VertxTracerFactory && (options.getTracingOptions() == null)) {
        continue;
      }
      provider.init(this);
    }
  }

  private void initMetrics() {
    VertxMetricsFactory provider = metricsFactory;
    if (provider != null) {
      provider.init(this);
    }
  }

  private void initTracing() {
    VertxTracerFactory provider = tracerFactory;
    if (provider != null) {
      provider.init(this);
    }
  }

  private void initTransport() {
    if (transport != null) {
      return;
    }
    transport = findTransport(options.getPreferNativeTransport());
  }

  private void initFileResolver() {
    if (fileResolver != null) {
      return;
    }
    fileResolver = new FileResolverImpl(options.getFileSystemOptions());
  }

  private void initThreadFactory() {
    if (threadFactory != null) {
      return;
    }
    threadFactory = VertxThreadFactory.INSTANCE;
  }

  private void initExecutorServiceFactory() {
    if (executorServiceFactory != null) {
      return;
    }
    executorServiceFactory = ExecutorServiceFactory.INSTANCE;
  }

  private void checkBeforeInstantiating() {
    checkTracing();
    checkMetrics();
  }

  private void checkTracing() {
    if (options.getTracingOptions() != null && this.tracer == null) {
      log.warn("Tracing options are configured but no tracer is instantiated. " +
        "Make sure you have the VertxTracerFactory in your classpath and META-INF/services/io.vertx.core.spi.VertxServiceProvider " +
        "contains the factory FQCN, or tracingOptions.getFactory() returns a non null value");
    }
  }

  private void checkMetrics() {
    if (options.getMetricsOptions() != null && options.getMetricsOptions().isEnabled() && this.metrics == null) {
      log.warn("Metrics options are configured but no metrics object is instantiated. " +
        "Make sure you have the VertxMetricsFactory in your classpath and META-INF/services/io.vertx.core.spi.VertxServiceProvider " +
        "contains the factory FQCN, or metricsOptions.getFactory() returns a non null value");
    }
  }

  /**
   * The native transport, it may be {@code null} or failed.
   */
  public static Transport nativeTransport() {
    Transport transport = null;
    try {
      Transport epoll = new EpollTransport();
      if (epoll.isAvailable()) {
        return epoll;
      } else {
        transport = epoll;
      }
    } catch (Throwable ignore) {
      // Jar not here
    }
    try {
      Transport kqueue = new KQueueTransport();
      if (kqueue.isAvailable()) {
        return kqueue;
      } else if (transport == null) {
        transport = kqueue;
      }
    } catch (Throwable ignore) {
      // Jar not here
    }
    return transport;
  }

  static Transport findTransport(boolean preferNative) {
    if (preferNative) {
      Collection<Transport> transports = ServiceHelper.loadFactories(Transport.class);
      Iterator<Transport> it = transports.iterator();
      while (it.hasNext()) {
        Transport transport = it.next();
        if (transport.isAvailable()) {
          return transport;
        }
      }
      Transport nativeTransport = nativeTransport();
      if (nativeTransport != null && nativeTransport.isAvailable()) {
        return nativeTransport;
      } else {
        return JDKTransport.INSTANCE;
      }
    } else {
      return JDKTransport.INSTANCE;
    }
  }
}