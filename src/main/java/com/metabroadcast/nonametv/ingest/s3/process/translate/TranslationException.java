package com.metabroadcast.nonametv.ingest.s3.process.translate;

/**
 * @author will
 */
public class TranslationException extends RuntimeException {

    public TranslationException(String message) {
        super(message);
    }

    public TranslationException(Throwable t) {
        super(t);
    }

    public TranslationException(String message, Throwable cause) {
        super(message, cause);
    }

}
