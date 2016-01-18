package eu.epitech.andyet.web.rest;

import com.codahale.metrics.annotation.Timed;
import eu.epitech.andyet.domain.News;
import eu.epitech.andyet.domain.Subscription;
import eu.epitech.andyet.domain.User;
import eu.epitech.andyet.repository.NewsRepository;
import eu.epitech.andyet.repository.SubscriptionRepository;
import eu.epitech.andyet.service.MailService;
import eu.epitech.andyet.web.rest.util.HeaderUtil;
import eu.epitech.andyet.web.rest.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for managing News.
 */
@RestController
@RequestMapping("/api")
public class NewsResource {

    private final Logger log = LoggerFactory.getLogger(NewsResource.class);

    @Inject
    private NewsRepository newsRepository;

    @Inject
    private MailService mailService;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    /**
     * POST  /newss -> Create a new news.
     */
    @RequestMapping(value = "/newss",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<News> createNews(@Valid @RequestBody News news) throws URISyntaxException {
        log.debug("REST request to save News : {}", news);
        if (news.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("news", "idexists", "A new news cannot already have an ID")).body(null);
        }
        News result = newsRepository.save(news);
        List<Subscription> subscriptions = subscriptionRepository.findAll().stream().filter(item -> (item.getIdMarketPlace().equals(result.getMarketPlace().getId()))).collect(Collectors.toList());
        String title = result.getMarketPlace().getName() + " vous a envoyé une News !";
        String content = result.getTitle() + "\n\n\n" + result.getContent();

        for (Subscription subscription : subscriptions) {
            mailService.sendEmail(subscription.getUser().getEmail(), title, content, false, false);
        }
        return ResponseEntity.created(new URI("/api/newss/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("news", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /newss -> Updates an existing news.
     */
    @RequestMapping(value = "/newss",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<News> updateNews(@Valid @RequestBody News news) throws URISyntaxException {
        log.debug("REST request to update News : {}", news);
        if (news.getId() == null) {
            return createNews(news);
        }
        News result = newsRepository.save(news);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("news", news.getId().toString()))
            .body(result);
    }

    /**
     * GET  /newss -> get all the newss.
     */
    @RequestMapping(value = "/newss",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<News>> getAllNewss(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Newss");
        Page<News> page = newsRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/newss");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /newss/:id -> get the "id" news.
     */
    @RequestMapping(value = "/newss/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<News> getNews(@PathVariable Long id) {
        log.debug("REST request to get News : {}", id);
        News news = newsRepository.findOne(id);
        return Optional.ofNullable(news)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /newss/:id -> delete the "id" news.
     */
    @RequestMapping(value = "/newss/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
        log.debug("REST request to delete News : {}", id);
        newsRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("news", id.toString())).build();
    }

    /**
     * GET  /marketPlace/:id/news -> get all the news for the "id" marketPlace.
     */
    @RequestMapping(value = "/marketPlaceNews/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<News> getMarketPlaceNews(@PathVariable Long id) {
        return newsRepository.findAll().stream().filter(item -> item.getMarketPlace().getId().equals(id)).collect(Collectors.toList());
    }
}
