package ca.udem.bibudem.calypso;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.Bitstream;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.ConfigurationService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import java.sql.SQLException;

import org.apache.http.message.BasicNameValuePair;
import org.dspace.discovery.IndexableObject;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.springframework.beans.factory.annotation.Autowired;
import ca.udem.bibudem.calypso.Image;

public class ClipAddEventConsumer implements Consumer {

    private int originalMode;

    private Set<String> uniqueIdsToDelete = new HashSet<>();
    private List<IndexableObject> objectsToUpdate = new ArrayList<>();
    private List<IndexableObject> createdItemsToUpdate = new ArrayList<>();

    @Override
    public void initialize() throws SQLException {
        // Code d'initialisation si nécessaire
    }

    private void callApiFastAPIUpdate(String itemId, String uuid, String itemHandle, String itemName, String collectionId, String url) throws IOException {
        String apiUrl = "http://localhost:8000/" + itemId;
        String requestBody = buildRequestBodyForUpdate(itemId, uuid, itemHandle, itemName, collectionId, url);

        // Appel de la méthode mise à jour de votre API FastAPI
        callApiFastAPI(requestBody, apiUrl);
    }

    private String buildRequestBodyForUpdate(String itemId, String uuid, String itemHandle, String itemName, String collectionId, String url) {
        // Construct the request body for the update
        Image image = new Image(itemId, uuid, itemHandle, itemName, collectionId, url); // Ajoutez un septième paramètre si nécessaire
        return image.toString();
    }
    private String getCollectionIdForBitstream(Context context, Bitstream bitstream) {
        try {
            // Accédez à la collection associée au bitstream
            String collectionId = bitstream.getBundles().get(0).getItems().get(0).getOwningCollection().getID().toString();
            return collectionId;
        } catch (SQLException e) {
            // Gérer l'exception selon les besoins
            e.printStackTrace();
            return null;
        }
    }


    private String buildBitstreamUrl(Context context, Bitstream bitstream) {
        try {
            ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
            String baseUrl = configurationService.getProperty("dspace.url");

            // Utilisation du premier bundle associé au bitstream
            String bundleName = bitstream.getBundles().get(0).getName();

            // Utilisation du handle du premier item associé au bitstream
            String handle = bitstream.getBundles().get(0).getItems().get(0).getHandle();

            // Construire l'URL du bitstream en utilisant le handle, le nom du bundle et l'ID du bitstream
            return baseUrl + "/bitstream/" + handle + "/" + bundleName + "/" + bitstream.getID() + "/" + bitstream.getName();
        } catch (SQLException e) {
            // Gérer l'exception selon les besoins
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void consume(Context context, Event event) throws IOException, SQLException, SolrServerException {
        // Obtenir le service ItemService depuis le contexte
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();

        if (event.getEventType() == Event.MODIFY) {
            // Obtenir le bitstream modifié à partir de l'événement
            Bitstream modifiedBitstream = (Bitstream) event.getSubject(context);

            // Récupérer les informations nécessaires du bitstream modifié
            String bitstreamID = modifiedBitstream.getID().toString();
            String bitstreamUUID = modifiedBitstream.getInternalId();
            String bundleName = modifiedBitstream.getBundles().get(0).getName();
            String collectionId = getCollectionIdForBitstream(context, modifiedBitstream);

            // Construire l'URL du bitstream modifié
            String url = buildBitstreamUrl(context, modifiedBitstream);

            // Appel de la méthode pour mettre à jour votre API FastAPI
            callApiFastAPIUpdate(
                    bitstreamID,
                    bitstreamUUID,
                    modifiedBitstream.getName(),
                    bundleName,
                    collectionId,
                    url
            );
        }
    }
    @Override
    public void end(Context ctx) throws Exception {
        try {
            for (String uid : uniqueIdsToDelete) {
                // ...
            }
            for (IndexableObject iu : objectsToUpdate) {
                indexObject(ctx, iu, false);
            }
            for (IndexableObject iu : createdItemsToUpdate) {
                indexObject(ctx, iu, true);
            }
        } finally {
            // ...
        }
    }

    @Override
    public void finish(Context ctx) throws Exception {
        // Finalisation si nécessaire
    }

    private String buildRequestBody(String fileName, String description) {
        return "{\"fileName\": \"" + fileName + "\", \"description\": \"" + description + "\"}";
    }

    private void callApiFastAPI(String requestBody, String endpoint) throws IOException {
        String apiUrl = "http://localhost:8000" + endpoint;
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(apiUrl);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setEntity(new StringEntity(requestBody));
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(entity.getContent()))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    System.out.println(line);
                }
            }
        }
    }

    private void performSearch(String query) {
        // Votre logique de recherche existante ici
    }

    // Ajout des méthodes manquantes
    private void indexObject(Context context, IndexableObject indexableObject, boolean isCreatedItem) {
        // Implémentez la logique d'indexation ici
    }
}
