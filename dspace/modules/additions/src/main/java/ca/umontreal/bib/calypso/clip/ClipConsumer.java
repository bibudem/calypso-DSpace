package ca.umontreal.bib.calypso.clip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Base64;

import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.access.status.DefaultAccessStatusHelper;
import org.dspace.access.status.factory.AccessStatusServiceFactory;
import org.dspace.access.status.service.AccessStatusService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.factory.DSpaceServicesFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.HttpClientBuilder;

public class ClipConsumer implements Consumer {

    // Pour récupérer le contenu d'un Bitstream
    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    // Un logger
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(ClipConsumer.class);

    // L'URL de notre serveur CLIP
    private String clipServerUrl;

    // Un objet pour produire du Json
    private ObjectMapper mapper;

    // Un objet qui vérifie la visibilité d'un item
    AccessStatusService accessStatusService;

    // La méthode initialize est appelée une seule fois
    @Override
    public void initialize() throws Exception {
        clipServerUrl = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("calypso.clip.server.url");
        mapper = new ObjectMapper();
        accessStatusService = AccessStatusServiceFactory.getInstance().getAccessStatusService();
    }

    /*
     *
     * Dépôt d'un document, première étape (avant de cliquer "Déposer").
     * On a un EventType=CREATE, SubjectType=BITSTREAM. Mais à ce moment, item.isArchived()
     * est false. D'ailleurs on n'a pas encore son nom.
     * Une fois qu'on clique sur "Déposer", on n'a pas de CREATE sur le Bitstream, on a des
     * MODIFY, mais on a un INSTALL pour l'Item, donc on doit réagir sur cet événement
     * et associer tous les Bitstream à ce moment.
     *
     * Si on ajoute un Bitstream à un Item, on a un EventType=CREATE, SubjectType=BITSTREAM
     * et on a un item.isArchived() = true, et son nom, donc on peut l'ajouter.
     *
     * Si on retire un Bitstream à un Item, on a un EventType=DELETE, SubjectType=BITSTREAM,
     * donc on peut le supprimer de l'index CLIP.
     *
     * Si on édite un bitstream, on peut seulement modifier ses métadonnées, donc aucun impact
     * sur l'indexation CLIP.
     *
     * Si on retire (withdraw) un item, on n'a pas de CREATE/DELETE sur les Bitstream. On doit donc
     * réagir sur le EventType=MODIFY, SubjectType=ITEM, detail="WITHDRAW" pour supprimer
     * tous les Bitstream de CLIP.
     *
     * Si on réintègre un item, on n'a pas de CREATE/DELETE sur les Bitstream, on doit donc
     * réagir sur le EventType=MODIFY, SubjectType=ITEM, detail="REINSTATE".
     *
     * Si on supprime un item, on aura des DELETE sur les Bitstream, on peut réagir
     * là-dessus.
     *
     * TODO: il reste à observer et gérer les événements suivants:
     * - on déplace un item dans une autre collection
     * - on modifie le dc:title d'un item (donc getName()): mais on n'a pas cet événement spécifique,
     *      si on ajoute une métadonnée, on a son nom dans detail, mais si on la modifie detail = null.
     */

     // Méthode principale qui doit traiter l'événement (si nécessaire)
    @Override
    public void consume(Context context, Event event) throws Exception {

        // Pour déterminer si on doit agir et ce qu'on doit faire, on a besoin
        // du type de sujet (item, bitstream, ...), du type d'événement
        // et parfois des détails
        int subjectType = event.getSubjectType();
        int eventType = event.getEventType();
        String detail = event.getDetail();
        log.info("Consume additions: eventType " + eventType);

        if (
            ( subjectType == Constants.ITEM && (eventType == Event.INSTALL || (eventType == Event.MODIFY && detail != null && detail.equals("REINSTATE"))) )
            ||
            ( subjectType == Constants.COLLECTION && eventType == Event.ADD ) ) {

            // Un item est déposé ou réintégré ou ajouté dans une collection
            Item item;
            if ( subjectType == Constants.ITEM ) item = (Item)event.getSubject(context);
            else item = (Item)event.getObject(context);

            // On vérifie si l'item est visible publiquement ou pas et s'il est archivé (pour éviter les ajouts
            // initiaux à la collection)
            if (item.isArchived() && accessStatusService.getAccessStatus(context, item).equals(DefaultAccessStatusHelper.OPEN_ACCESS)) {

                String itemId = item.getID().toString();
                String itemName = item.getName().toString();
                String itemHandle = item.getHandle().toString();    // Attention ça pourrait être null
                String itemCollectionId = item.getOwningCollection().getID().toString();    // On prend pour acquis que c'est la collection principale qu'on veut

                // On va faire une boucle sur ses Bundle puis ses Bitstream en traitant
                // uniquement ceux qui nous intéressent (les images)
                for (Bundle bdl: item.getBundles()) {
                    // On ignore le bundle THUMBNAIL et le bundle LICENCE
                    if (!(bdl.getName().equals("THUMBNAIL") || bdl.getName().equals("LICENCE"))) {
                        for (Bitstream bts: bdl.getBitstreams()) {
                            actionBitstream(bts, context, itemId, itemName, itemHandle, itemCollectionId,"POST");
                        }
                    }
                }
            }
        }
        else if (subjectType == Constants.BITSTREAM && eventType == Event.CREATE) {

            // On a l'ajout d'un Bitstream, mais on le traite uniquement si l'item est déjà
            // archivé. Ça correspond à ajouter un Bitstream à un item existant.
            Bitstream bts = (Bitstream)event.getSubject(context);
            Bundle bdl = bts.getBundles().get(0);
            Item item = bdl.getItems().get(0);
            if (accessStatusService.getAccessStatus(context, item).equals(DefaultAccessStatusHelper.OPEN_ACCESS)) {
                if ( item.isArchived()) {
                    if (!(bdl.getName().equals("THUMBNAIL") || bdl.getName().equals("LICENCE"))) {
                        actionBitstream(bts, context, item.getID().toString(), item.getName(), item.getHandle(), item.getOwningCollection().getID().toString(),"POST");
                    }
                }
            }
        }

        else if ( subjectType == Constants.BITSTREAM && eventType == Event.MODIFY && detail != null && detail.equals("WITHDRAW")) {

            // Un item est retiré, on retire les images de l'index CLIP
            Item item = (Item)event.getSubject(context);
            for (Bundle bdl: item.getBundles()) {
                // On ignore le bundle THUMBNAIL et le bundle LICENCE
                if (!(bdl.getName().equals("THUMBNAIL") || bdl.getName().equals("LICENCE"))) {
                    for (Bitstream bts: bdl.getBitstreams()) {
                        deleteBitstream(bts, context);
                    }
                }
            }
        }

        else if ( subjectType == Constants.BITSTREAM && eventType == Event.DELETE ) {

            // Un Bitstream est supprimé, soit lui-même soit parce que son Item l'est
            // On n'a pas nécessairement beaucoup de contexte (bundle, ...), donc on va
            // toujours essayer de le supprimer de l'index CLIP
            Bitstream bts = (Bitstream)event.getSubject(context);
            deleteBitstream(bts, context);
        }

         else if (subjectType == Constants.BITSTREAM && eventType == Event.MODIFY) {
                // Mise à jour d'un Bitstream, utilisez l'événement comme nécessaire
                log.info("Consume MODIFY_METADATA " + eventType);
                Item item = (Item) event.getSubject(context);
                log.info("Consume MODIFY_METADATA2 " + item.getID().toString());
                // la mise à jour du Bitstream ici...
                if (accessStatusService.getAccessStatus(context, item).equals(DefaultAccessStatusHelper.OPEN_ACCESS)) {
                    for (Bundle bdl : item.getBundles()) {
                        // On ignore le bundle THUMBNAIL et le bundle LICENCE
                        if (!(bdl.getName().equals("THUMBNAIL") || bdl.getName().equals("LICENCE"))) {
                            for (Bitstream bts : bdl.getBitstreams()) {
                                actionBitstream(bts, context, item.getID().toString(), item.getName(), item.getHandle(), item.getOwningCollection().getID().toString(), "PUT");
                            }
                        }
                    }
                }
         }

    }

    // Ajout (si nécessaire) d'un Bitstream
    private void actionBitstream(Bitstream bts, Context context, String itemId, String itemName, String itemHandle, String itemCollectionId, String method) throws IOException, SQLException, AuthorizeException {

        // Pour l'instant on suppose qu'on supporte tout ce qui est une image
        String mimeType = bts.getFormat(context).getMIMEType();
        if (mimeType.indexOf("image/") == 0) {

            // On doit passer le contenu en base64 dans le Json car
            // l'URL du bitstream n'est pas encore accessible publiquement
            InputStream is = bitstreamService.retrieve(context, bts);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Utils.bufferedCopy(is, bos);
            bos.close();
            String content = Base64.getEncoder().encodeToString(bos.toByteArray());
            String uuid = bts.getID().toString();
            ClipImage img = new ClipImage(uuid, itemId, itemName, itemHandle, itemCollectionId, content);
            String imgJson = mapper.writeValueAsString(img);
            String endpoint = "/" + uuid;
            sendClipRequest(endpoint, method, imgJson);
        }
    }

    // Retrait (si nécessaire) d'un Bitstream
    private void deleteBitstream(Bitstream bts, Context context) throws IOException {
        sendClipRequest("/" + bts.getID().toString(), "DELETE", null);
    }

    // Envoi d'une requête à l'API CLIP
    private void sendClipRequest(String endpoint, String method, String body) throws ClientProtocolException, IOException {
        String url = clipServerUrl + endpoint;
        HttpUriRequest req;

        // On construit la requête
        switch (method) {
            case "POST":
                req = RequestBuilder.post(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .setEntity(new StringEntity(body, ContentType.APPLICATION_JSON))
                    .build();
                break;
            case "PUT":
                req = RequestBuilder.put(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .setEntity(new StringEntity(body, ContentType.APPLICATION_JSON))
                    .build();
                break;
            case "DELETE":
                req = RequestBuilder.delete(url)
                    .addHeader("Accept", "application/json")
                    .build();
                break;
            default:
                log.warn("Méthode HTTP non supportée: " + method);
                return;
        }

        // On exécute la requête
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(req);

        // On log la réponse s'il y a erreur
        if (response.getStatusLine().getStatusCode() != 200) {
                handleClipResponse(response);
            }
     }

    // Méthode pour traiter la réponse de l'API CLIP et gérer les erreurs de validation
    private void handleClipResponse(HttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 422) {
            // Erreur de validation
            String responseBody = EntityUtils.toString(response.getEntity());
            throw new IOException("Clip API Error: " + statusCode + ", " + responseBody);
        } else if (statusCode != 200) {
            // Gérer d'autres erreurs ici si nécessaire
            log.error(statusCode + " / " + response.getStatusLine().getReasonPhrase());
        }
    }

    @Override
    public void end(Context ctx) throws Exception {
    }

    @Override
    public void finish(Context ctx) throws Exception {
    }


    /**
     * Une classe toute simple qui permet de représenter une image à indexer
     * avec ses propriétés, afin de la convertir en JSon pour utiliser
     * l'API dspace-clip-api.
     */
    private class ClipImage {

        // Les propriétés de l'image
        private String uuid = null;
        private String itemId = null;
        private String itemHandle = null;
        private String itemName = null;
        private String collectionId = null;
        private String content = null;

        // Une image avec toutes ses propriétés
        public ClipImage(String uuid, String itemId, String itemName, String itemHandle, String itemCollectionId, String content) {
            setUuid(uuid);
            setItemId(itemId);
            setItemName(itemName);
            setItemHandle(itemHandle);
            setCollectionId(itemCollectionId);
            setContent(content);
        }

        // L'identifiant
        public String getUuid() {
            return this.uuid;
        }
        public void setUuid(String id) {
            this.uuid = id;
        }

        // L'identifiant de l'item
        public String getItemId() {
            return this.itemId;
        }
        public void setItemId(String id) {
            this.itemId = id;
        }

        // Le titre de l'item
        public String getItemName() {
            return this.itemName;
        }
        public void setItemName(String name) {
            this.itemName = name;
        }

        // Le handle de l'item
        public String getItemHandle() {
            return this.itemHandle;
        }
        public void setItemHandle(String handle) {
            this.itemHandle = handle;
        }

        // La collection
        public String getCollectionId() {
            return this.collectionId;
        }
        public void setCollectionId(String id) {
            this.collectionId = id;
        }

        // Le contenu (en base64)
        public String getContent() {
            return this.content;
        }
        public void setContent(String content) {
            this.content = content;
        }

    }
}
