package com.metabroadcast.nonametv.ingest.process.translate;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.atlasapi.media.entity.simple.Broadcast;
import org.atlasapi.media.entity.simple.LocalizedDescription;
import org.atlasapi.media.entity.simple.Person;
import org.atlasapi.media.entity.simple.Rating;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.common.io.Resources;
import com.metabroadcast.common.intl.Country;
import com.metabroadcast.nonametv.xml.Programme;

/**
 * @author will
 */
@RunWith(JUnit4.class)
public class ProgrammeToItemTranslatorTest {

    private static final String ITEM_URI = "http://nonametv.org/foxtv.no20150106014000+010020150106023500+0100";

    private ProgrammeToItemTranslator translator;

    private Programme validProgramme;

    @Before
    public void setUp() throws URISyntaxException, FileNotFoundException, JAXBException {
        JAXBContext context;
        Unmarshaller unmarshaller;
        context = JAXBContext.newInstance(Programme.class);
        unmarshaller = context.createUnmarshaller();

        URL validProgrammeUrl = Resources.getResource(getClass(), "validProgramme.xml");
        validProgramme = (Programme)unmarshaller.unmarshal(validProgrammeUrl);

        translator = new ProgrammeToItemTranslator();
    }

    @Test
    public void translate_setsTypeToEpisode() {
        TranslationResult result  = translator.translate(validProgramme);
        assertEquals(TranslationResult.Status.SUCCESS, result.getStatus());
        assertEquals("episode", result.getItem() .getType());
    }

    @Test
    public void translate_setsUriConstructedFromUrlPrefixChannelStartAndEnd() {
        TranslationResult result  = translator.translate(validProgramme);
        assertEquals(TranslationResult.Status.SUCCESS, result.getStatus());
        assertEquals(ITEM_URI, result.getItem() .getUri());
    }

    @Test
    public void translate_setsTitle() {
        TranslationResult result  = translator.translate(validProgramme);
        assertEquals(TranslationResult.Status.SUCCESS, result.getStatus());
        assertEquals("Medium", result.getItem() .getTitle());
    }

    @Test
    public void translate_setsEnglishDescription() {
        TranslationResult result  = translator.translate(validProgramme);
        assertEquals(TranslationResult.Status.SUCCESS, result.getStatus());
        assertEquals("Description of Medium", result.getItem() .getDescription());
    }

    @Test
    public void translate_setsForeignLanguageDescription() {
        TranslationResult result  = translator.translate(validProgramme);
        Set<LocalizedDescription> descriptions = result.getItem() .getDescriptions();
        assertEquals(TranslationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, descriptions.size());
        assertEquals("Joe prøver å finne ut av ting når Allison er besatt av en annen kvinne.",
            ((LocalizedDescription)(descriptions.toArray()[0])).getDescription());
    }

    @Test
    public void translate_setsPeople() {
        TranslationResult result  = translator.translate(validProgramme);
        List<Person> people = result.getItem() .getPeople();
        assertEquals(TranslationResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, people.size());
    }

    @Test
    public void translate_setsGenres() {
        TranslationResult result  = translator.translate(validProgramme);
        Set<String> genres = result.getItem() .getGenres();
        assertEquals(TranslationResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, genres.size());
    }

    @Test
    public void translate_setsAliases() {
        TranslationResult result  = translator.translate(validProgramme);
        Set<String> aliases = result.getItem() .getAliases();
        assertEquals(TranslationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, aliases.size());
        assertEquals("http://thetvdb.com/?tab=episode&seriesid=73265&seasonid=16881&id=315802&lid=9", aliases.toArray()[0]);
    }

    @Test
    public void translate_setsEpisodeNumber() {
        TranslationResult result  = translator.translate(validProgramme);
        Integer episodeNumber = result.getItem() .getEpisodeNumber();
        assertEquals(TranslationResult.Status.SUCCESS, result.getStatus());
        assertEquals(12, episodeNumber.intValue());
    }

    @Test
    public void translate_setsSeriesNumber() {
        TranslationResult result  = translator.translate(validProgramme);
        Integer seriesNumber = result.getItem() .getSeriesNumber();
        assertEquals(TranslationResult.Status.SUCCESS, result.getStatus());
        assertEquals(3, seriesNumber.intValue());
    }

    @Test
    public void translate_setsYear() {
        TranslationResult result  = translator.translate(validProgramme);
        assertEquals(TranslationResult.Status.SUCCESS, result.getStatus());
        assertEquals(Integer.valueOf(2005), result.getItem() .getYear());
    }

    @Test
    public void translate_setsRating() {
        TranslationResult result  = translator.translate(validProgramme);
        Set<Rating> ratings = result.getItem() .getRatings();
        assertEquals(TranslationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, ratings.size());
        assertEquals(6.9 / 9, ((Rating)(ratings.toArray()[0])).getValue().floatValue(), 0.1);
    }

    @Test
    public void translate_setsCountriesOfOrigin() {
        TranslationResult result  = translator.translate(validProgramme);
        Set<Country> countriesOfOrigin = result.getItem() .getCountriesOfOrigin();
        assertEquals(TranslationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, countriesOfOrigin.size());
        assertEquals("France", ((Country)(countriesOfOrigin.toArray()[0])).getName());
    }

    @Test
    public void translate_setsBroadcast() {
        TranslationResult result  = translator.translate(validProgramme);
        SortedSet<Broadcast> broadcasts = result.getItem() .getBroadcasts();
        assertEquals(TranslationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, broadcasts.size());
        assertEquals("http://foxtv.no/", ((Broadcast)(broadcasts.toArray()[0])).getBroadcastOn());
    }

}
