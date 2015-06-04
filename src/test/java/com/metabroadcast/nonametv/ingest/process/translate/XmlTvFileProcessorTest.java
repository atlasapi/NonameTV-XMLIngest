package com.metabroadcast.nonametv.ingest.process.translate;

import com.metabroadcast.nonametv.ingest.process.XmlTvFileProcessor;
import org.atlasapi.client.AtlasWriteClient;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class XmlTvFileProcessorTest {

    private XmlTvFileProcessor xmlTvFileProcessor;

    @Mock
    private AtlasWriteClient atlasWriteClient;

    @Mock
    private ProgrammeToItemTranslator programmeToItemTranslator;

    @Mock
    private BrandFactory brandFactory;

    @Before
    public void setUp() throws Exception {
        xmlTvFileProcessor = new XmlTvFileProcessor(atlasWriteClient, programmeToItemTranslator, brandFactory);
    }
}
