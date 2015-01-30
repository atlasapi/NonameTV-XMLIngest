package com.metabroadcast.nonametv.ingest.s3.process;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.atlasapi.client.AtlasWriteClient;
import org.atlasapi.media.entity.simple.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metabroadcast.common.ingest.s3.process.FileProcessor;
import com.metabroadcast.common.ingest.s3.process.ProcessableFile;
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
    private String result;

    public XMLTVFileProcessor(AtlasWriteClient atlasWriteClient, ProgrammeToItemTranslator programmeToItemTranslator) {
        this.atlasWriteClient = atlasWriteClient;
        this.programmeToItemTranslator = programmeToItemTranslator;
    }

    @Override
    public void process(ProcessableFile processableFile) {
        result = "Failure";

        JAXBContext context = null;
        Unmarshaller unmarshaller = null;
        Tv tv = null;
        try {
            context = JAXBContext.newInstance(Tv.class);
            unmarshaller = context.createUnmarshaller();
            tv = (Tv)unmarshaller.unmarshal(processableFile.getFile());
        } catch (JAXBException e) {
            result = "Unable to deserialise the input file as XMLTV-compliant XML";
            log.error(result, e);
            return;
        }

        if (tv == null) {
            result = "Unable to deserialise a 'tv' element from the feed file";
            log.error(result);
            return;
        }

        for(Programme programme : tv.getProgramme()) {
            Item item = null;
            try {
                item = programmeToItemTranslator.translate(programme);
            } catch (TranslationException e) {
                result = "Unable to translate programme into item\n" + e.getMessage();
                log.error("Unable to translate programme {} into item", programme, e);
                return;
            }

            try {
                atlasWriteClient.writeItem(item);
            } catch (RuntimeException e) {
                result = "Unable to insert into Atlas\n" + e.getMessage();
                log.error("Unable to insert into Atlas", e);
                return;
            }

            log.debug("Successfully deserialised a programme and posted an item: {}", item);
            result = "Success";
        }
    }

    @Override
    public String getResult() {
        return result;
    }

}
