package com.metabroadcast.nonametv.ingest.process.translate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.atlasapi.media.entity.simple.Item;

public class TranslationResult {

    private Item item;
    private Status status;
    private List<String> errors;

    public TranslationResult(Item item) {
        this.item = item;
        status = Status.SUCCESS;
    }

    public TranslationResult(Item item, Status status, String... errors) {
        this.item = item;
        assert status != Status.SUCCESS;
        this.status = status;
        this.errors = Arrays.asList(errors);
    }

    public TranslationResult(String error) {
        status = Status.ERROR;
        errors = Arrays.asList(error);
    }

    public void addWarning(String warning) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(warning);
    }

    public Item getItem() {
        return item;
    }

    public Status getStatus() {
        return status;
    }

    public List<String> getErrors() {
        return errors;
    }

    public enum Status {
        SUCCESS, WARNING, ERROR
    }

}
