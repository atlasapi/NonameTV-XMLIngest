package com.metabroadcast.nonametv.ingest.s3.process.translate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.entity.simple.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.nonametv.xml.*;

/**
 * @author will
 */
public class ProgrammeToItemTranslator {

    private static final String URL_PREFIX = "http://nonametv.org/";
    private static final String XMLTV_NS_EPISODE_NUM_SYSTEM = "xmltv_ns";
    private static final Pattern XMLTV_NS_SEASON_AND_EPISODE_NUMBER = Pattern.compile("(\\d+)\\s+\\.\\s+(\\d+)\\s+\\.");
    private static final Pattern XMLTV_STAR_RATING = Pattern.compile("([\\d\\.]+)\\s+/\\s+([\\d\\.]+)");
    private static final DateTimeFormatter XMLTV_DATE_FORMAT = DateTimeFormat.forPattern("yyyyMMddHHmmss Z");

    public Item translate(Programme programme) throws TranslationException {
        Item item = new Item();

        item.setType("episode");

        String itemUri = URL_PREFIX + programme.getChannel() + programme.getStart() + programme.getStop();
        itemUri = itemUri.replace(" ", "");
        item.setUri(itemUri);

        item.setTitle(programme.getTitle().get(0).getvalue());

        Set<LocalizedDescription> descriptionSet = new HashSet<>();
        for (Desc desc : programme.getDesc()) {
            if ("en".equals(desc.getLang())) {
                item.setDescription(desc.getvalue());
            } else {
                LocalizedDescription localizedDescription = new LocalizedDescription();
                localizedDescription.setDescription(desc.getvalue());
                descriptionSet.add(localizedDescription);
            }
        }
        item.setDescriptions(descriptionSet);
        List<Person> personList = new ArrayList<>();
        Credits credits = programme.getCredits();
        for (Actor actor : credits.getActor()) {
            Person person = new Person();
            person.setName(actor.getvalue());
            person.setRole(actor.getRole());
            person.setUri(itemUri + "/" + actor.getvalue());
            personList.add(person);
        }
        for (Adapter adapter : credits.getAdapter()) {
            Person person = new Person();
            person.setName(adapter.getvalue());
            person.setUri(itemUri + "/" + adapter.getvalue());
            personList.add(person);
        }
        for (Commentator commentator : credits.getCommentator()) {
            Person person = new Person();
            person.setName(commentator.getvalue());
            person.setUri(itemUri + "/" + commentator.getvalue());
            personList.add(person);
        }
        for (Composer composer : credits.getComposer()) {
            Person person = new Person();
            person.setName(composer.getvalue());
            person.setUri(itemUri + "/" + composer.getvalue());
            personList.add(person);
        }
        for (Director director : credits.getDirector()) {
            Person person = new Person();
            person.setName(director.getvalue());
            person.setUri(itemUri + "/" + director.getvalue());
            personList.add(person);
        }
        for (Editor editor : credits.getEditor()) {
            Person person = new Person();
            person.setName(editor.getvalue());
            person.setUri(itemUri + "/" + editor.getvalue());
            personList.add(person);
        }
        for (Guest guest : credits.getGuest()) {
            Person person = new Person();
            person.setName(guest.getvalue());
            person.setUri(itemUri + "/" + guest.getvalue());
            personList.add(person);
        }
        for (Presenter presenter : credits.getPresenter()) {
            Person person = new Person();
            person.setName(presenter.getvalue());
            person.setUri(itemUri + "/" + presenter.getvalue());
            personList.add(person);
        }
        for (Producer producer : credits.getProducer()) {
            Person person = new Person();
            person.setName(producer.getvalue());
            person.setUri(itemUri + "/" + producer.getvalue());
            personList.add(person);
        }
        for (Writer writer : credits.getWriter()) {
            Person person = new Person();
            person.setName(writer.getvalue());
            person.setUri(itemUri + "/" + writer.getvalue());
            personList.add(person);
        }
        item.setPeople(personList);
        try {
            item.setYear(Integer.parseInt(programme.getDate()));
        } catch (NumberFormatException e) {
            throw new TranslationException("Unable to parse integer year from date element", e);
        }
        List<String> genreList = new ArrayList<>(programme.getCategory().size());
        for (Category category : programme.getCategory()) {
            genreList.add(URL_PREFIX + category.getvalue());
        }
        item.setGenres(genreList);
        Set<String> aliasSet = new HashSet<>();
        for (Url url : programme.getUrl()) {
            aliasSet.add(url.getvalue());
        }
        item.setAliases(aliasSet);

        for (EpisodeNum episodeNum : programme.getEpisodeNum()) {
            if (XMLTV_NS_EPISODE_NUM_SYSTEM.equals(episodeNum.getSystem())) {
                Matcher matcher = XMLTV_NS_SEASON_AND_EPISODE_NUMBER.matcher(episodeNum.getvalue());
                if (matcher.matches()) {
                    item.setSeriesNumber(Integer.parseInt(matcher.group(1)) + 1);
                    item.setEpisodeNumber(Integer.parseInt(matcher.group(2)) + 1);
                } else {
                    throw new TranslationException("episode-num system=\"xmltv_ns\" tag was present but contained a value in an unexpected format: " + episodeNum.getvalue());
                }
            }
        }

        try {
            item.setYear(Integer.parseInt(programme.getDate()));
        } catch (NumberFormatException e) {
            throw new TranslationException("Unable to parse integer year from date element", e);
        }

        List<org.atlasapi.media.entity.simple.Rating> ratingList = new ArrayList<>();
        for (StarRating starRating : programme.getStarRating()) {
            Matcher matcher = XMLTV_STAR_RATING.matcher(starRating.getValue());
            if (matcher.matches()) {
                double numerator = Double.parseDouble(matcher.group(1));
                double denominator = Double.parseDouble(matcher.group(2));
                org.atlasapi.media.entity.simple.Rating rating = new org.atlasapi.media.entity.simple.Rating();
                rating.setValue((float) (numerator / denominator));
                rating.setPublisherDetails(new PublisherDetails("nonametv"));
                ratingList.add(rating);
            } else {
                throw new TranslationException("star-rating tag was present but contained a value in an unexpected format: {}" + starRating.getValue());
            }
        }
        item.setRatings(ratingList);

        List<com.metabroadcast.common.intl.Country> countryList = new ArrayList<>();
        for (Country country : programme.getCountry()) {
            com.metabroadcast.common.intl.Country countryListEntry = Countries.fromCode(country.getvalue());
            countryList.add(countryListEntry);
        }
        item.setCountriesOfOrigin(countryList);

        List<Broadcast> broadcastList = new ArrayList<>();
        Broadcast broadcast = new Broadcast("http://" + programme.getChannel() + "/",
            XMLTV_DATE_FORMAT.parseDateTime(programme.getStart()),
            XMLTV_DATE_FORMAT.parseDateTime(programme.getStop()));
        broadcastList.add(broadcast);
        item.setBroadcasts(broadcastList);

        item.setPublisher(new PublisherDetails("nonametv"));

        return item;
    }

}
