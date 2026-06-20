package org.dspace.app.iiif.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * This service implements methods for executing a solr search and creating IIIF search result annotations.
 * <p>
 * https://github.com/dbmdz/solr-ocrhighlighting
 */
@Scope("prototype")
@Component
public class CollSpecWordHighlightSolrSearch implements SearchAnnotationService {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(CollSpecWordHighlightSolrSearch.class);

    private String endpoint;
    private String manifestId;

    @Override
    public boolean useSearchPlugin(String className) {
        return className.contentEquals(CollSpecWordHighlightSolrSearch.class.getCanonicalName());
    }

    @Override
    public void initializeQuerySettings(String endpoint, String manifestId) {
        this.endpoint = endpoint;
        this.manifestId = manifestId;
    }

    @Override
    public String getSearchResponse(UUID uuid, String query) {
        String json = "";
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        String solrService = configurationService.getProperty("iiif.search.url");
        boolean validationEnabled = configurationService
                .getBooleanProperty("discovery.solr.url.validation.enabled");
        UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);

        if (urlValidator.isValid(solrService) || validationEnabled) {
            HttpSolrClient solrServer = new HttpSolrClient.Builder(solrService).build();
            solrServer.setUseMultiPartPost(true);
            SolrQuery solrQuery = getSolrQuery(adjustQuery(query), manifestId);
            QueryRequest req = new QueryRequest(solrQuery);
            req.setResponseParser(new NoOpResponseParser("json"));
            NamedList<Object> resp;
            try {
                resp = solrServer.request(req);
                json = (String) resp.get("response");
            } catch (SolrServerException | IOException e) {
                throw new RuntimeException("Unable to retrieve search response.", e);
            }
        } else {
            log.error("Error while initializing solr, invalid url: {}", solrService);
        }
        return getAnnotationList(uuid, json, query);
    }

    /**
     * Wraps multi-word queries in parens.
     * @param query the search query
     * @return adjusted query
     */
    private String adjustQuery(String query) {
        if (query != null && query.split(" ").length > 1) {
            return '(' + query + ')';
        }
        return query;
    }

    /**
     * Constructs a solr search URL.
     *
     * @param query the search terms
     * @param manifestId the id of the manifest in which to search
     * @return solr query
     */
    private SolrQuery getSolrQuery(String query, String manifestId) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.set("q", "ocr_text:" + query + " AND manifest_url:\"" + manifestId + "\"");
        solrQuery.set(CommonParams.WT, "json");
        solrQuery.set("hl", "true");
        solrQuery.set("hl.ocr.fl", "ocr_text");
        solrQuery.set("hl.ocr.contextBlock", "line");
        solrQuery.set("hl.ocr.contextSize", "0");
        solrQuery.set("hl.snippets", "8192");
        solrQuery.set("hl.ocr.maxPassages", "8192");
        solrQuery.set("hl.ocr.trackPages", "on");
        solrQuery.set("hl.ocr.limitBlock", "page");
        solrQuery.set("hl.ocr.absoluteHighlights", "true");
        solrQuery.set("hl.weightMatches", "true");
        solrQuery.set("hl.fl", "");
        return solrQuery;
    }

    private String getAnnotationList(UUID uuid, String json, String query) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode body = null;
        try {
            body = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Unable to process json response.", e);
        }

        ObjectNode result = mapper.createObjectNode();
        ArrayNode context = mapper.createArrayNode();
        context.add("http://iiif.io/api/presentation/2/context.json");
        context.add("http://iiif.io/api/search/1/context.json");
        result.set("@context", context);
        result.put("@id", manifestId + "/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
        result.put("@type", "sc:AnnotationList");

        ArrayNode resources = mapper.createArrayNode();
        ArrayNode hits = mapper.createArrayNode();

        if (body == null) {
            result.set("resources", resources);
            result.set("hits", hits);
            return result.toString();
        }

        JsonNode highs = body.get("ocrHighlighting");
        if (highs != null && highs.isObject()) {
            Iterator<String> fieldNames = highs.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode highEntry = highs.get(fieldName);
                JsonNode ocrNode = highEntry != null ? highEntry.get("ocr_text") : null;
                if (ocrNode == null) {
                    continue;
                }

                JsonNode snippets = ocrNode.get("snippets");
                if (snippets == null || !snippets.isArray()) {
                    continue;
                }

                for (final JsonNode snippet : snippets) {
                    if (snippet == null) {
                        continue;
                    }

                    String pageId = getCanvasId(snippet.get("pages"));
                    if (pageId == null) {
                        continue;
                    }

                    String fullText = snippet.get("text") != null ? cleanSnippetText(snippet.get("text").asText()) : "";
                    JsonNode highlights = snippet.get("highlights");
                    if (highlights == null || !highlights.isArray()) {
                        continue;
                    }

                    for (final JsonNode highlightGroup : highlights) {
                        if (highlightGroup == null || !highlightGroup.isArray()) {
                            continue;
                        }

                        for (final JsonNode highlight : highlightGroup) {
                            if (highlight == null) {
                                continue;
                            }

                            String match = extractMatchText(highlight, query);
                            String annoId = buildAnnotationId(uuid, pageId, highlight);
                            String on = buildCanvasTarget(uuid, pageId, highlight);

                            if (annoId == null || on == null || match == null || match.isBlank()) {
                                continue;
                            }

                            ObjectNode resource = mapper.createObjectNode();
                            resource.put("@id", annoId);
                            resource.put("@type", "oa:Annotation");
                            resource.put("motivation", "sc:painting");

                            ObjectNode cnt = mapper.createObjectNode();
                            cnt.put("@type", "cnt:ContentAsText");
                            cnt.put("chars", match);
                            resource.set("resource", cnt);
                            resource.put("on", on);
                            resources.add(resource);

                            ObjectNode hit = mapper.createObjectNode();
                            ArrayNode annotations = mapper.createArrayNode();
                            annotations.add(annoId);
                            hit.set("annotations", annotations);
                            hit.put("match", match);
                            hit.put("before", extractBefore(fullText, match));
                            hit.put("after", extractAfter(fullText, match));
                            hit.put("@type", "search:Hit");
                            hits.add(hit);
                        }
                    }
                }
            }
        }

        result.set("resources", resources);
        result.set("hits", hits);
        return result.toString();
    }

    private String extractMatchText(JsonNode highlight, String fallbackQuery) {
        JsonNode textNode = highlight.get("text");
        if (textNode != null && !textNode.asText().isBlank()) {
            return cleanSnippetText(textNode.asText());
        }
        return cleanSnippetText(fallbackQuery);
    }

    private String extractBefore(String fullText, String match) {
        if (fullText == null || match == null) {
            return "";
        }
        int idx = fullText.indexOf(match);
        return idx >= 0 ? fullText.substring(0, idx) : "";
    }

    private String extractAfter(String fullText, String match) {
        if (fullText == null || match == null) {
            return "";
        }
        int idx = fullText.indexOf(match);
        return idx >= 0 ? fullText.substring(idx + match.length()) : "";
    }

    private String buildAnnotationId(UUID uuid, String pageId, JsonNode highlight) {
        String params = getParams(highlight);
        if (params == null) {
            return null;
        }
        return this.endpoint + uuid + "/annot/" + pageId + "-" + params;
    }

    private String buildCanvasTarget(UUID uuid, String pageId, JsonNode highlight) {
        String params = getParams(highlight);
        if (params == null) {
            return null;
        }
        return this.endpoint + uuid + "/canvas/" + pageId + "#xywh=" + params;
    }

    private String getParams(JsonNode highlight) {
        int ulx = highlight.get("ulx") != null ? highlight.get("ulx").asInt() : -1;
        int uly = highlight.get("uly") != null ? highlight.get("uly").asInt() : -1;
        int lrx = highlight.get("lrx") != null ? highlight.get("lrx").asInt() : -1;
        int lry = highlight.get("lry") != null ? highlight.get("lry").asInt() : -1;

        String w = (lrx >= 0 && ulx >= 0) ? Integer.toString(lrx - ulx) : null;
        String h = (lry >= 0 && uly >= 0) ? Integer.toString(lry - uly) : null;

        if (w != null && h != null) {
            return ulx + "," + uly + "," + w + "," + h;
        }
        return null;
    }

    private String cleanSnippetText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value;
        cleaned = cleaned.replaceAll("<[^>]*>", "");
        cleaned = cleaned.replace("''", "'");
        cleaned = cleaned.replace("“", "");
        cleaned = cleaned.replace("”", "");
        cleaned = cleaned.replace("\"", "");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }

    /**
     * Returns position of canvas. Uses the "pages" id attribute.
     *
     * Assumes page ids like "page_23" and maps them to IIIF canvas ids like "c23".
     *
     * @param pagesNode the pages node
     * @return canvas id or null if node was null
     */
    private String getCanvasId(JsonNode pagesNode) {
        if (pagesNode != null) {
            JsonNode page = pagesNode.get(0);
            if (page != null) {
                JsonNode pageId = page.get("id");
                if (pageId != null) {
                    String[] identArr = pageId.asText().split("_");
                    if (identArr.length > 1) {
                        return "c" + identArr[1];
                    }
                }
            }
        }
        return null;
    }
}