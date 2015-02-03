package com.metabroadcast.nonametv.ingest.s3.process.translate;

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
import org.atlasapi.media.entity.simple.Item;
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
        Item item = translator.translate(validProgramme);
        assertEquals("episode", item.getType());
    }

    @Test
    public void translate_setsUriConstructedFromUrlPrefixChannelStartAndEnd() {
        Item item = translator.translate(validProgramme);
        assertEquals(ITEM_URI, item.getUri());
    }

    @Test
    public void translate_setsTitle() {
        Item item = translator.translate(validProgramme);
        assertEquals("Medium", item.getTitle());
    }

    @Test
    public void translate_setsEnglishDescription() {
        Item item = translator.translate(validProgramme);
        assertEquals("Description of Medium", item.getDescription());
    }

    @Test
    public void translate_setsForeignLanguageDescription() {
        Item item = translator.translate(validProgramme);
        Set<LocalizedDescription> descriptions = item.getDescriptions();
        assertEquals(1, descriptions.size());
        assertEquals("Joe prøver å finne ut av ting når Allison er besatt av en annen kvinne.",
            ((LocalizedDescription)(descriptions.toArray()[0])).getDescription());
    }

    @Test
    public void translate_setsPeople() {
        Item item = translator.translate(validProgramme);
        List<Person> people = item.getPeople();
        assertEquals(2, people.size());
    }

    @Test
    public void translate_setsGenres() {
        Item item = translator.translate(validProgramme);
        Set<String> genres = item.getGenres();
        assertEquals(2, genres.size());
    }

    @Test
    public void translate_setsAliases() {
        Item item = translator.translate(validProgramme);
        Set<String> aliases = item.getAliases();
        assertEquals(1, aliases.size());
        assertEquals("http://thetvdb.com/?tab=episode&seriesid=73265&seasonid=16881&id=315802&lid=9", aliases.toArray()[0]);
    }

    @Test
    public void translate_setsEpisodeNumber() {
        Item item = translator.translate(validProgramme);
        Integer episodeNumber = item.getEpisodeNumber();
        assertEquals(12, episodeNumber.intValue());
    }

    @Test
    public void translate_setsSeriesNumber() {
        Item item = translator.translate(validProgramme);
        Integer seriesNumber = item.getSeriesNumber();
        assertEquals(3, seriesNumber.intValue());
    }

    @Test
    public void translate_setsYear() {
        Item item = translator.translate(validProgramme);
        assertEquals(Integer.valueOf(2005), item.getYear());
    }

    @Test
    public void translate_setsRating() {
        Item item = translator.translate(validProgramme);
        Set<Rating> ratings = item.getRatings();
        assertEquals(1, ratings.size());
        assertEquals(6.9 / 9, ((Rating)(ratings.toArray()[0])).getValue().floatValue(), 0.1);
    }

    @Test
    public void translate_setsCountriesOfOrigin() {
        Item item = translator.translate(validProgramme);
        Set<Country> countriesOfOrigin = item.getCountriesOfOrigin();
        assertEquals(1, countriesOfOrigin.size());
        assertEquals("France", ((Country)(countriesOfOrigin.toArray()[0])).getName());
    }

    @Test
    public void translate_setsBroadcast() {
        Item item = translator.translate(validProgramme);
        SortedSet<Broadcast> broadcasts = item.getBroadcasts();
        assertEquals(1, broadcasts.size());
        assertEquals("http://foxtv.no/", ((Broadcast)(broadcasts.toArray()[0])).getBroadcastOn());
    }

}
