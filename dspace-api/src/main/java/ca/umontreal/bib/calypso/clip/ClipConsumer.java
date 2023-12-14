package ca.umontreal.bib.calypso.clip;

import org.dspace.content.Bitstream;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.discovery.IndexableObject;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.service.impl.HttpConnectionPoolService;
import org.dspace.content.DSpaceObject;
import org.apache.logging.log4j.Logger;
import org.dspace.core.LogHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.umontreal.bib.calypso.clip.Image;

import com.fasterxml.jackson.core.JsonProcessingException;


public class ClipConsumer implements Consumer {

        private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();
        private HttpConnectionPoolService httpConnectionPoolService; // Ajout de la déclaration
        private String clipServerUrl;
        private ObjectMapper objectMapper = new ObjectMapper();
        private String itemId;
        private String uuid;
        private String itemHandle;
        private String itemName;
        private String collectionId;
        private String url;

    @Override
    public void initialize() throws Exception {
        log.debug("Initializing ClipAddEventConsumer");

        try {
            ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
            clipServerUrl = configurationService.getProperty("clip.server.url");
        } catch (Exception e) {
            log.error("Error during initialization", e);
            throw new RuntimeException("Error during initialization", e);
        }
    }

   @Override
   public void consume(Context context, Event event) throws Exception {

       int eventType = event.getEventType();
       int subjectType = event.getSubjectType();
       DSpaceObject subject = event.getSubject(context);
       DSpaceObject object = event.getObject(context);

       if (subject instanceof Bitstream) {
           Bitstream modifiedBitstream = (Bitstream) subject;

           log.info("Bitstream ID: {}", modifiedBitstream.getID());
           log.info("Bitstream Name: {}", modifiedBitstream.getName());
           log.info("Bitstream Bundle Name: {}", modifiedBitstream.getBundles().get(0).getName());

           // Vérifiez si le type d'événement est pris en charge
           if (!isValidEventType(eventType, subjectType)) {
               return;
           }

           try {
               switch (eventType) {
                   case Event.MODIFY:
                   case Event.MODIFY_METADATA:
                       log.info("Processing modified bitstream: {}", modifiedBitstream);
                       processModifiedBitstream(context, modifiedBitstream, "update");
                       break;

                   case Event.ADD:
                       log.info("Processing create bitstream: {}", modifiedBitstream);
                       processModifiedBitstream(context, modifiedBitstream, "index");
                       break;

                   case Event.DELETE:
                   case Event.REMOVE:
                       log.info("Processing delete bitstream: {}", modifiedBitstream);
                       processModifiedBitstream(context, modifiedBitstream, "delete");
                       break;

                   default:
                       log.warn("IndexConsumer received an unknown event type: " + eventType +
                               " for subject type: " + subjectType + ". Skipping event.");
                       break;
               }
           } catch (Exception e) {
               log.error("Error processing modified bitstream", e);
               throw new Exception("Error processing modified bitstream", e);
           }
       } else {
           log.warn("ClipAddEventConsumer received une erreur: " + subject);
           return;
       }
   }


   private boolean isValidEventType(int eventType, int subjectType) {
       log.info("Event value: ", eventType);
       return (eventType == Event.MODIFY || eventType == Event.MODIFY_METADATA ||
               eventType == Event.ADD || eventType == Event.DELETE || eventType == Event.REMOVE);
   }


    private String convertImageToJson(Image image) {
        try {
            return objectMapper.writeValueAsString(image);
        } catch (JsonProcessingException e) {
            log.error("Error converting image to JSON", e);
            // Gérer l'exception localement, par exemple, en enregistrant les détails dans les journaux.
            return null; // Ou une autre valeur par défaut, selon la logique de votre application.
        }
    }


    private void processModifiedBitstream(Context context, Bitstream modifiedBitstream, String eventType) throws  Exception{
        try {
            String bitstreamID = modifiedBitstream.getID().toString();
            String bitstreamUUID = modifiedBitstream.getInternalId();
            String bundleName = modifiedBitstream.getBundles().get(0).getName();
            String collectionId = getCollectionIdForBitstream(modifiedBitstream);
            String url = buildBitstreamUrl(context, modifiedBitstream);

            log.info("Processing details - Bitstream ID: {}, UUID: {}, Name: {}, Bundle: {}, Collection ID: {}, URL: {}",
                    bitstreamID, bitstreamUUID, modifiedBitstream.getName(), bundleName, collectionId, url);
            if (eventType.equals("update")) {
                        updateClipImage(bitstreamID, bitstreamUUID, modifiedBitstream.getName(), bundleName, collectionId, url);
            } else if (eventType.equals("index")) {
                indexClipImage(bitstreamID, bitstreamUUID, modifiedBitstream.getName(), bundleName, collectionId, url);
            } else if (eventType.equals("delete")) {
                deleteClipImage(bitstreamID);
            }

                }
             catch (Exception e) {
                log.error("Error processing modified bitstream", e);
                throw new Exception("Error processing modified bitstream", e);
            }
    }


    private void updateClipImage(String itemId, String uuid, String itemHandle, String itemName, String collectionId, String url) {
            try {
                String endpoint = "/" + itemId;
                String requestBody = buildRequestBody(itemId, uuid, itemHandle, itemName, collectionId, url);
                consumerClip(requestBody, endpoint, "PUT");
            } catch (Exception e) {
                log.error("Error updating clipImage", e);
                throw new RuntimeException("Error updating clipImage", e);
            }
        }


    private void indexClipImage(String itemId, String uuid, String itemHandle, String itemName, String collectionId, String url) throws IOException {
         String endpoint = "/" + itemId;
         String requestBody = buildRequestBody(itemId, uuid, itemHandle, itemName, collectionId, url);
         consumerClip(requestBody, endpoint, "POST");
    }

    private void deleteClipImage(String idImage) throws IOException {
            String endpoint = "/" + idImage;
            consumerClip(idImage, endpoint, "DELETE");
        }

    private String buildRequestBody(String itemId, String uuid, String itemHandle, String itemName, String collectionId, String url) {
        log.info("Building request body for update/create/delete");
        Image image = new Image(itemId, uuid, itemHandle, itemName, collectionId, url);
        String jsonBody = convertImageToJson(image);
        return jsonBody;
    }


    private String getCollectionIdForBitstream(Bitstream bitstream) throws SQLException {
        String collectionId = bitstream.getBundles().get(0).getItems().get(0).getOwningCollection().getID().toString();
        return collectionId;
    }

    private String buildBitstreamUrl(Context context, Bitstream bitstream) {

        try {
            ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
            String baseUrl = configurationService.getProperty("dspace.server.url");
            String bundleName = bitstream.getBundles().get(0).getName();
            String handle = bitstream.getBundles().get(0).getItems().get(0).getHandle();
            log.info("Building bitstream URL"+baseUrl);
            return baseUrl + "/api/core/bitstream/" + bitstream.getID() + "/content" ;
        } catch (SQLException e) {
            log.error("Error building bitstream URL", e);
            return null;
        }
    }

    private CloseableHttpClient getClient() {
        return HttpClients.createDefault();
    }

    /**
     * Envoie une requête HTTP avec le corps de la requête donné au serveur spécifié.
     *
     * @param requestBody Le corps de la requête.
     * @param endpoint    L'URL de l'API à atteindre.
     * @param method      La méthode HTTP (POST, PUT, DELETE, etc.).
     * @throws IOException En cas d'erreur d'entrée/sortie.
     */
    public void consumerClip(String requestBody, String endpoint, String method) throws IOException {
        String apiUrl = clipServerUrl + endpoint;
        URL url = new URL(apiUrl);

        try (CloseableHttpClient httpClient = getClient()) {
            // Configurer la requête HTTP POST/PUT/DELETE
            HttpRequestBase request;
            switch (method.toUpperCase()) {
                case "POST":
                    request = new HttpPost(url.toURI());
                    break;
                case "PUT":
                    request = new HttpPut(url.toURI());
                    break;
                case "DELETE":
                    request = new HttpDelete(url.toURI());
                    break;
                default:
                    throw new IllegalArgumentException("Méthode HTTP non supportée: " + method);
            }

            request.setHeader("Content-Type", "application/json");

            if ("POST".equals(method) || "PUT".equals(method)) {
                StringEntity entity = new StringEntity(requestBody);
                ((HttpEntityEnclosingRequestBase) request).setEntity(entity);
            }

            // Exécuter la requête
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                // Lire la réponse du serveur
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    try (InputStream content = responseEntity.getContent()) {
                        // Process the content as needed
                    }
                }

                // Handle HTTP status code
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    log.info("Request was successful. Status code: {}", statusCode);
                } else {
                    log.warn("Request failed with status code: {}", statusCode);
                    // Handle error response
                }
            }
        } catch (IOException | IllegalArgumentException | URISyntaxException e) {
            log.error("Error during HTTP request", e);
            // Handle the exception as needed
        }
    }


    @Override
    public void end(Context ctx) {
        try {
            log.info("End of ClipAddEventConsumer");
        } catch (Exception e) {
            log.error("Error during end", e);
        } finally {
            // ...
        }
    }

    @Override
    public void finish(Context ctx) {
        // Implémentation de la méthode finish
        log.info("Finishing ClipAddEventConsumer");
        // Finalization if needed
    }

}
