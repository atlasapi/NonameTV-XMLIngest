package com.metabroadcast.nonametv.ingest;

import java.util.concurrent.Executor;

import org.atlasapi.client.AtlasWriteClient;
import org.atlasapi.client.GsonAtlasClient;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletProperties;

import com.amazonaws.auth.BasicAWSCredentials;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostSpecifier;
import com.metabroadcast.common.ingest.MessageStreamer;
import com.metabroadcast.common.properties.Configurer;
import com.metabroadcast.nonametv.ingest.process.XmlTvFileProcessor;
import com.metabroadcast.nonametv.ingest.process.translate.ProgrammeToItemTranslator;

public class JettyMain {

    public static void main(String[] args) throws Exception {
        Server server = createServer();
        ServletContextHandler ctx = new ServletContextHandler(server, "/");

        ServletHolder jerseyServlet = ctx.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/*");
        jerseyServlet.setInitOrder(1);
        jerseyServlet.setInitParameters(ImmutableMap.of(
            ServletProperties.JAXRS_APPLICATION_CLASS, ApplicationConfig.class.getCanonicalName(),
            "com.sun.jersey.api.json.POJOMappingFeature", "true",
            ServerProperties.MEDIA_TYPE_MAPPINGS, "json : application/json"
        ));

        HealthModule healthModule = new HealthModule();
        ServletHolder healthHolder = new ServletHolder(healthModule.healthController());
        ctx.addServlet(healthHolder, "/system/health");

        /*
         * Configure and start message streamer
         */

        server.setHandler(ctx);
        server.start();

        HostSpecifier host = HostSpecifier.fromValid(Configurer.get("atlas.host").get());
        Optional apiKey = Optional.of(Configurer.get("atlas.apiKey").get());

        AtlasWriteClient atlasClient = new GsonAtlasClient(host, apiKey);

        MessageStreamer messageStreamer = new MessageStreamer(Configurer.get("aws.sqsQueueName").get(),
            new BasicAWSCredentials(Configurer.get("aws.accessKey").get(),
                Configurer.get("aws.secretKey").get()));

        messageStreamer.registerFileProcessor(Configurer.get("aws.s3BucketName").get(), new XmlTvFileProcessor(atlasClient, new ProgrammeToItemTranslator()));

        messageStreamer.start();

    }

    private static Server createServer() {
        final int port = Integer.parseInt(Configurer.get("server.port").get());

        final int maxThreads = 500;
        final int acceptQueueSize = 2048;
        final int acceptors = Runtime.getRuntime().availableProcessors();
        final int selectors = 0;

        final HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setRequestHeaderSize(1024);
        httpConfig.setResponseHeaderSize(1024);

        final ThreadPool queuedThreadPool = new QueuedThreadPool(maxThreads);
        final Server server = new Server(queuedThreadPool);

        final Executor defaultExecutor = null;
        final Scheduler defaultScheduler = null;
        final ByteBufferPool defaultByteBufferPool = null;

        final ServerConnector connector = new ServerConnector(
                server, defaultExecutor, defaultScheduler, defaultByteBufferPool,
                acceptors, selectors, new HttpConnectionFactory(httpConfig)
        );

        connector.setPort(port);
        connector.setAcceptQueueSize(acceptQueueSize);
        server.setConnectors(new Connector[] { connector });

        return server;
    }

}
