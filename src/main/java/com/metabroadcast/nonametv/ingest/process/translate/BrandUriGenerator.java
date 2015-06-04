package com.metabroadcast.nonametv.ingest.process.translate;

import com.google.common.collect.Iterables;
import com.metabroadcast.nonametv.xml.Programme;

public class BrandUriGenerator {

    private static final String URL_PREFIX = "http://nonametv.org/";

    public String generate(Programme programme) {
        return URL_PREFIX + Iterables.getOnlyElement(programme.getTitle()).getvalue();
    }

}
