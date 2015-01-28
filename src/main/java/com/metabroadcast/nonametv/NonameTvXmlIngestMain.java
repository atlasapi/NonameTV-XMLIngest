package com.metabroadcast.nonametv;

import org.atlasapi.client.AtlasWriteClient;
import org.atlasapi.client.GsonAtlasClient;

import com.amazonaws.auth.BasicAWSCredentials;
import com.google.common.base.Optional;
import com.google.common.net.HostSpecifier;
import com.metabroadcast.common.ingest.MessageStreamer;
import com.metabroadcast.common.properties.Configurer;
import com.metabroadcast.nonametv.ingest.s3.process.XMLTVFileProcessor;

/**
 * @author will
 */
public class NonameTvXmlIngestMain {

    private static final String S3_BUCKET_NAME = "mbst-watchfolders-dev";

    public static void main (String... args) {
        HostSpecifier host = HostSpecifier.fromValid("stage.atlas.metabroadcast.com");
        Optional apiKey = Optional.of(Configurer.get("atlas.apiKey").get());

        AtlasWriteClient atlasClient = new GsonAtlasClient(host, apiKey);

        MessageStreamer messageStreamer = new MessageStreamer(new BasicAWSCredentials(
            Configurer.get("aws.accessKey").get(),
            Configurer.get("aws.secretKey").get()));

        messageStreamer.registerFileProcessor(S3_BUCKET_NAME, new XMLTVFileProcessor(atlasClient));

        while (true) {
            messageStreamer.streamNextMessage();
        }
    }

}
