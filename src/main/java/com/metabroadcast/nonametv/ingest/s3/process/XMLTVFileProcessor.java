package com.metabroadcast.nonametv.ingest.s3.process;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.atlasapi.client.AtlasWriteClient;
import org.atlasapi.media.entity.simple.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metabroadcast.common.ingest.s3.process.FileProcessor;
import com.metabroadcast.nonametv.ingest.s3.process.translate.ProgrammeToItemTranslator;
import com.metabroadcast.nonametv.ingest.s3.process.translate.TranslationException;
import com.metabroadcast.nonametv.xml.Programme;
import com.metabroadcast.nonametv.xml.Tv;

/**
 * @author will
 */
public class XMLTVFileProcessor implements FileProcessor {

    private static final Logger log = LoggerFactory.getLogger(XMLTVFileProcessor.class);

    private final AtlasWriteClient atlasWriteClient;
    private final ProgrammeToItemTranslator programmeToItemTranslator;
    private StringBuilder result;

    public XMLTVFileProcessor(AtlasWriteClient atlasWriteClient, ProgrammeToItemTranslator programmeToItemTranslator) {
        this.atlasWriteClient = atlasWriteClient;
        this.programmeToItemTranslator = programmeToItemTranslator;
        result = new StringBuilder();
    }

    @Override
    public void process(File file) {
        result.append("Started processing an XMLTV feed file");

        JAXBContext context;
        Unmarshaller unmarshaller;
        Tv tv;
        try {
            context = JAXBContext.newInstance(Tv.class);
            unmarshaller = context.createUnmarshaller();
            tv = (Tv)unmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            String error = "Unable to deserialise the input file as XMLTV-compliant XML";
            result.append(error);
            log.error(error, e);
            return;
        }

        if (tv == null) {
            String error = "Unable to deserialise a 'tv' element from the feed file";
            result.append(error);
            log.error(error);
            return;
        }

        for (Programme programme : tv.getProgramme()) {
            Item item;
            try {
                item = programmeToItemTranslator.translate(programme);
            } catch (TranslationException e) {
                result.append("Unable to translate programme into item: " + e.getMessage());
                log.error("Unable to translate programme {} into item", programme, e);
                continue;
            }

            try {
                atlasWriteClient.writeItem(item);
            } catch (RuntimeException e) {
                result.append("Unable to insert into Atlas: " + e.getMessage());
                log.error("Unable to insert into Atlas", e);
                continue;
            }

            log.debug("Successfully deserialised a programme and posted an item: {}", item);
            result.append("Successfully deserialised a programme and posted an item");
        }
    }

    @Override
    public String getResult() {
        return result.toString();
    }

}
