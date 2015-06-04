package com.metabroadcast.nonametv.ingest.process.translate;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.entity.simple.BrandSummary;
import org.atlasapi.media.entity.simple.Broadcast;
import org.atlasapi.media.entity.simple.Item;
import org.atlasapi.media.entity.simple.LocalizedDescription;
import org.atlasapi.media.entity.simple.Person;
import org.atlasapi.media.entity.simple.PublisherDetails;
import org.atlasapi.media.entity.simple.Rating;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.collect.Iterables;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.nonametv.xml.Actor;
import com.metabroadcast.nonametv.xml.Adapter;
import com.metabroadcast.nonametv.xml.Category;
import com.metabroadcast.nonametv.xml.Commentator;
import com.metabroadcast.nonametv.xml.Composer;
import com.metabroadcast.nonametv.xml.Country;
import com.metabroadcast.nonametv.xml.Credits;
import com.metabroadcast.nonametv.xml.Desc;
import com.metabroadcast.nonametv.xml.Director;
import com.metabroadcast.nonametv.xml.Editor;
import com.metabroadcast.nonametv.xml.EpisodeNum;
import com.metabroadcast.nonametv.xml.Guest;
import com.metabroadcast.nonametv.xml.Presenter;
import com.metabroadcast.nonametv.xml.Producer;
import com.metabroadcast.nonametv.xml.Programme;
import com.metabroadcast.nonametv.xml.StarRating;
import com.metabroadcast.nonametv.xml.Url;
import com.metabroadcast.nonametv.xml.Writer;

public class ProgrammeToItemTranslator {

    private static final String URL_PREFIX = "http://nonametv.org/";
    private static final String XMLTV_NS_EPISODE_NUM_SYSTEM = "xmltv_ns";
    private static final Pattern XMLTV_NS_SEASON_AND_EPISODE_NUMBER = Pattern.compile("(\\d+)\\s+\\.\\s+(\\d+)\\s+\\.");
    private static final Pattern XMLTV_STAR_RATING = Pattern.compile("([\\d\\.]+)\\s+/\\s+([\\d\\.]+)");
    private static final DateTimeFormatter XMLTV_DATE_FORMAT = DateTimeFormat.forPattern("yyyyMMddHHmmss Z");

    private BrandUriGenerator brandUriGenerator;

    public ProgrammeToItemTranslator(BrandUriGenerator brandUriGenerator) {
        this.brandUriGenerator = brandUriGenerator;
    }

    public TranslationResult translate(Programme programme) {
        List<String> warnings = new ArrayList<>();
        Item item = new Item();

        String itemUri = getUri(programme);

        item.setType("episode");

        item.setUri(itemUri);

        String title = getTitle(programme);
        item.setTitle(title);
        BrandSummary brandSummary = new BrandSummary(brandUriGenerator.generate(programme));
        item.setBrandSummary(brandSummary);

        item.setDescriptions(getLocalizedDescriptions(programme));
        item.setDescription(getDescription(programme));
        item.setPeople(getPeople(programme, itemUri));
        try {
            item.setYear(getYear(programme));
        } catch (NumberFormatException e) {
            warnings.add("Unable to parse year from programme");
        }
        item.setGenres(getGenres(programme));
        item.setAliases(getAliases(programme));
        if (hasSeriesAndEpisodeNumber(programme)) {
            try {
                item.setSeriesNumber(getSeriesNumber(programme));
            } catch (IllegalArgumentException e) {
                warnings.add(e.getMessage());
            }
            try {
                item.setEpisodeNumber(getEpisodeNumber(programme));
            } catch (IllegalArgumentException e) {
                warnings.add(e.getMessage());
            }
        }
        item.setRatings(getRatings(programme));
        item.setCountriesOfOrigin(getCountriesOfOrigin(programme));
        item.setBroadcasts(getBroadcasts(programme));
        item.setPublisher(new PublisherDetails("nonametv"));

        if (warnings.isEmpty()) {
            return new TranslationResult(item);
        } else {
            return new TranslationResult(item, TranslationResult.Status.WARNING, warnings.toArray(new String[] {}));
        }
    }

    private String getUri(Programme programme) {
        String itemUri = URL_PREFIX + programme.getChannel() + programme.getStart() + programme.getStop();
        itemUri = itemUri.replace(" ", "");
        return itemUri;
    }

    private String getTitle(Programme programme) {
        return Iterables.getOnlyElement(programme.getTitle()).getvalue();
    }

    private Set<LocalizedDescription> getLocalizedDescriptions(Programme programme) {
        Set<LocalizedDescription> descriptionSet = new HashSet<>();
        for (Desc desc : programme.getDesc()) {
            if (!"en".equals(desc.getLang())) {
                LocalizedDescription localizedDescription = new LocalizedDescription();
                localizedDescription.setDescription(desc.getvalue());
                descriptionSet.add(localizedDescription);
            }
        }
        return descriptionSet;
    }

    private String getDescription(Programme programme) {
        for (Desc desc : programme.getDesc()) {
            if ("en".equals(desc.getLang())) {
                return desc.getvalue();
            }
        }

        return "";
    }

    private List<Person> getPeople(Programme programme, String itemUri) {
        List<Person> personList = new ArrayList<>();
        Credits credits = programme.getCredits();
        if (credits == null) {
            return ImmutableList.of();
        }
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
        return personList;
    }

    private int getYear(Programme programme) throws NumberFormatException {
        return Integer.parseInt(programme.getDate());
    }

    private List<String> getGenres(Programme programme) {
        List<String> genreList = new ArrayList<>(programme.getCategory().size());
        for (Category category : programme.getCategory()) {
            genreList.add(URL_PREFIX + category.getvalue());
        }
        return genreList;
    }

    private Set<String> getAliases(Programme programme) {
        Set<String> aliasSet = new HashSet<>();
        for (Url url : programme.getUrl()) {
            aliasSet.add(url.getvalue());
        }
        return aliasSet;
    }

    private boolean hasSeriesAndEpisodeNumber(Programme programme) {
        for (EpisodeNum episodeNum : programme.getEpisodeNum()) {
            if (XMLTV_NS_EPISODE_NUM_SYSTEM.equals(episodeNum.getSystem())) {
                Matcher matcher = XMLTV_NS_SEASON_AND_EPISODE_NUMBER.matcher(episodeNum.getvalue());
                return matcher.matches();
            }
        }
        return false;
    }

    private int getSeriesNumber(Programme programme) {
        for (EpisodeNum episodeNum : programme.getEpisodeNum()) {
            if (XMLTV_NS_EPISODE_NUM_SYSTEM.equals(episodeNum.getSystem())) {
                Matcher matcher = XMLTV_NS_SEASON_AND_EPISODE_NUMBER.matcher(episodeNum.getvalue());
                if (matcher.matches()) {
                    return Integer.parseInt(matcher.group(1)) + 1;
                } else {
                    throw new IllegalArgumentException("episode-num system=\"xmltv_ns\" tag was present but contained a value in an unexpected format: " + episodeNum.getvalue());
                }
            }
        }
        throw new IllegalArgumentException("Unable to find episode-num system=\"xmltv_ns\" tag");
    }

    private int getEpisodeNumber(Programme programme) {
        for (EpisodeNum episodeNum : programme.getEpisodeNum()) {
            if (XMLTV_NS_EPISODE_NUM_SYSTEM.equals(episodeNum.getSystem())) {
                Matcher matcher = XMLTV_NS_SEASON_AND_EPISODE_NUMBER.matcher(episodeNum.getvalue());
                if (matcher.matches()) {
                    return Integer.parseInt(matcher.group(2)) + 1;
                } else {
                    throw new IllegalArgumentException("episode-num system=\"xmltv_ns\" tag was present but contained a value in an unexpected format: " + episodeNum.getvalue());
                }
            }
        }
        throw new IllegalArgumentException("Unable to find episode-num system=\"xmltv_ns\" tag");
    }

    private List<Rating> getRatings(Programme programme) {
        List<Rating> ratingList = new ArrayList<>();
        for (StarRating starRating : programme.getStarRating()) {
            Matcher matcher = XMLTV_STAR_RATING.matcher(starRating.getValue());
            if (matcher.matches()) {
                double numerator = Double.parseDouble(matcher.group(1));
                double denominator = Double.parseDouble(matcher.group(2));
                Rating rating = new Rating();
                rating.setValue((float) (numerator / denominator));
                rating.setPublisherDetails(new PublisherDetails("nonametv"));
                ratingList.add(rating);
            } else {
                throw new IllegalArgumentException("star-rating tag was present but contained a value in an unexpected format: " + starRating.getValue());
            }
        }
        return ratingList;
    }

    private List<com.metabroadcast.common.intl.Country> getCountriesOfOrigin(Programme programme) {
        List<com.metabroadcast.common.intl.Country> countryList = new ArrayList<>();
        for (Country country : programme.getCountry()) {
            com.metabroadcast.common.intl.Country countryListEntry = Countries.fromCode(country.getvalue());
            if (null != countryListEntry) {
                countryList.add(countryListEntry);
            }
        }
        return countryList;
    }

    private List<Broadcast> getBroadcasts(Programme programme) {
        List<Broadcast> broadcastList = new ArrayList<>();
        Broadcast broadcast = new Broadcast("http://" + programme.getChannel() + "/",
            XMLTV_DATE_FORMAT.parseDateTime(programme.getStart()),
            XMLTV_DATE_FORMAT.parseDateTime(programme.getStop()));
        broadcastList.add(broadcast);
        return broadcastList;
    }

}
