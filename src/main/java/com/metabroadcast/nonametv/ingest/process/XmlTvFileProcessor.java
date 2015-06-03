package com.metabroadcast.nonametv.ingest.process;

import com.google.common.base.Preconditions;
import com.metabroadcast.common.ingest.s3.process.FileProcessor;
import com.metabroadcast.common.ingest.s3.process.ProcessingResult;
import com.metabroadcast.nonametv.ingest.process.translate.ProgrammeToItemTranslator;
import com.metabroadcast.nonametv.ingest.process.translate.TranslationResult;
import com.metabroadcast.nonametv.xml.Programme;
import com.metabroadcast.nonametv.xml.Tv;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import org.atlasapi.client.AtlasWriteClient;
import org.atlasapi.media.entity.simple.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * @author will
 */
public class XmlTvFileProcessor implements FileProcessor {

    private static final Logger log = LoggerFactory.getLogger(XmlTvFileProcessor.class);

    private final AtlasWriteClient atlasWriteClient;
    private final ProgrammeToItemTranslator programmeToItemTranslator;

    private boolean lastRunSuccessful;

    public XmlTvFileProcessor(AtlasWriteClient atlasWriteClient,
        ProgrammeToItemTranslator programmeToItemTranslator) {
        this.atlasWriteClient = Preconditions.checkNotNull(atlasWriteClient);
        this.programmeToItemTranslator = Preconditions.checkNotNull(programmeToItemTranslator);

        lastRunSuccessful = true;
    }

    @Override
    public ProcessingResult process(String originalFilename, File file) {
        log.debug("Started processing an XMLTV feed file");
        ProcessingResult processingResult = new ProcessingResult();

        Tv tv = null;
        try {
            JAXBContext context = JAXBContext.newInstance(Tv.class);

            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            spf.setFeature("http://apache.org/xml/features/validation/schema", false);
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            XMLReader xmlReader = spf.newSAXParser().getXMLReader();
            InputSource inputSource = new InputSource(new FileReader(file));
            SAXSource source = new SAXSource(xmlReader, inputSource);

            Unmarshaller unmarshaller = context.createUnmarshaller();

            tv = (Tv)unmarshaller.unmarshal(source);
        } catch (JAXBException | ParserConfigurationException | SAXException | FileNotFoundException e) {
            String error = "Unable to deserialise the input file as XMLTV-compliant XML";
            log.error(error, e);
            lastRunSuccessful = false;
            processingResult.error("input file", error);
            return processingResult;
        }

        if (tv == null) {
            String error = "Unable to deserialise a 'tv' element from the feed file";
            log.error(error);
            lastRunSuccessful = false;
            processingResult.error("input file", error);
            return processingResult;
        }

        for (Programme programme : tv.getProgramme()) {
            TranslationResult translationResult = programmeToItemTranslator.translate(programme);
            Item item = translationResult.getItem();

            String programmeId = programme.getChannel() + programme.getStart() + programme.getStop();

            switch (translationResult.getStatus()) {
            case ERROR:
                log.debug("Error(s) translating programme {}", programmeId);
                for (String error : translationResult.getErrors()) {
                    processingResult.error(programmeId, error);
                }
                continue;
            }

            try {
                atlasWriteClient.writeItem(item);
            } catch (RuntimeException e) {
                log.debug("Unable to insert into Atlas programme {}", programmeId, e);
                processingResult.error(programmeId, "Unable to insert into Atlas: " + e.getMessage());
                continue;
            }

            switch (translationResult.getStatus()) {
            case SUCCESS:
                processingResult.success();
            case WARNING:
                for (String warning : translationResult.getErrors()) {
                    processingResult.warning(programmeId, warning);
                }
            }

            log.debug("Successfully posted programme {} item {}", programme, item);
            lastRunSuccessful = true;
        }
        return processingResult;
    }

    public boolean wasLastRunSuccessful() {
        return lastRunSuccessful;
    }

}
