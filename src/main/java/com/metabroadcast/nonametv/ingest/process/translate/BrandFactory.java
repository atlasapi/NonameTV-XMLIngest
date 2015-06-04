package com.metabroadcast.nonametv.ingest.process.translate;

import com.metabroadcast.nonametv.xml.Programme;
import org.atlasapi.media.entity.simple.Item;
import org.atlasapi.media.entity.simple.PublisherDetails;

public class BrandFactory {

    private final BrandUriGenerator brandUriGenerator;

    public BrandFactory(BrandUriGenerator brandUriGenerator) {
        this.brandUriGenerator = brandUriGenerator;
    }

    public Item createFrom(Programme programme) {
        Item brand = new Item(brandUriGenerator.generate(programme));
        brand.setPublisher(new PublisherDetails("nonametv"));
        brand.setType("brand");

        return brand;
    }

}
