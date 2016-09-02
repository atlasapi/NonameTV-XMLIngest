package com.metabroadcast.nonametv.ingest.process;

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

import com.metabroadcast.common.ingest.monitorclient.model.Entity;
import com.metabroadcast.common.ingest.s3.process.FileProcessor;
import com.metabroadcast.common.ingest.s3.process.ProcessingResult;
import com.metabroadcast.nonametv.ingest.process.translate.BrandFactory;
import com.metabroadcast.nonametv.ingest.process.translate.ProgrammeToItemTranslator;
import com.metabroadcast.nonametv.ingest.process.translate.TranslationResult;
import com.metabroadcast.nonametv.xml.Programme;
import com.metabroadcast.nonametv.xml.Tv;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author will
 */
public class XmlTvFileProcessor implements FileProcessor {

    private static final Logger log = LoggerFactory.getLogger(XmlTvFileProcessor.class);

    private final AtlasWriteClient atlasWriteClient;
    private final ProgrammeToItemTranslator programmeToItemTranslator;
    private final BrandFactory brandFactory;

    private boolean lastRunSuccessful;

    public XmlTvFileProcessor(AtlasWriteClient atlasWriteClient,
        ProgrammeToItemTranslator programmeToItemTranslator,
        BrandFactory brandFactory) {
        this.atlasWriteClient = checkNotNull(atlasWriteClient);
        this.programmeToItemTranslator = checkNotNull(programmeToItemTranslator);
        this.brandFactory = checkNotNull(brandFactory);

        lastRunSuccessful = true;
    }

    @Override
    public ProcessingResult process(String originalFilename, File file) {
        log.debug("Started processing an XMLTV feed file");
        ProcessingResult.Builder resultBuilder = ProcessingResult.builder();

        Tv tv;
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
            resultBuilder.error(String.format("input file: %s", error));
            return resultBuilder.build();
        }

        if (tv == null) {
            String error = "Unable to deserialise a 'tv' element from the feed file";
            log.error(error);
            lastRunSuccessful = false;
            resultBuilder.error(String.format("input file: %s", error));
            return resultBuilder.build();
        }

        for (Programme programme : tv.getProgramme()) {
            TranslationResult translationResult = programmeToItemTranslator.translate(programme);
            Item item = translationResult.getItem();

            String programmeId = programme.getChannel() + programme.getStart() + programme.getStop();

            switch (translationResult.getStatus()) {
            case ERROR:
                log.debug("Error(s) translating programme {}", programmeId);
                for (String error : translationResult.getErrors()) {
                    resultBuilder.error(String.format("%s: %s", programmeId, error));
                }
                continue;
            }

            try {
                atlasWriteClient.writeItem(brandFactory.createFrom(programme));
                atlasWriteClient.writeItem(item);
            } catch (RuntimeException e) {
                log.debug("Unable to insert into Atlas programme {}", programmeId, e);
                resultBuilder.error(String.format("%s%nUnable to insert into Atlas: %s", programmeId, Throwables.getStackTraceAsString(e)));
                continue;
            }

            switch (translationResult.getStatus()) {
            case SUCCESS:
                Entity entity = Entity.success().build();
                resultBuilder.addEntity(entity);
                break;
            case WARNING:
                for (String warning : translationResult.getErrors()) {
                    resultBuilder.error(String.format("%s%n%s", programmeId, warning));
                }
            }

            log.debug("Successfully posted programme {} item {}", programme, item);
            lastRunSuccessful = true;
        }
        return resultBuilder.build();
    }

    public boolean wasLastRunSuccessful() {
        return lastRunSuccessful;
    }

}
