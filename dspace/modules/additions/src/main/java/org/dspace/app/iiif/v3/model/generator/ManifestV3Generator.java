/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.model.generator;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import info.freelibrary.iiif.presentation.v3.ImageContent;
import info.freelibrary.iiif.presentation.v3.OtherContent;
import info.freelibrary.iiif.presentation.v3.Manifest;
import info.freelibrary.iiif.presentation.v3.ContentResource;
import info.freelibrary.iiif.presentation.v3.Resource;
import info.freelibrary.iiif.presentation.v3.properties.Label;
import info.freelibrary.iiif.presentation.v3.properties.Metadata;
import info.freelibrary.iiif.presentation.v3.properties.Provider;
import info.freelibrary.iiif.presentation.v3.properties.Summary;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import jakarta.validation.constraints.NotNull;

/**
 * This generator wraps a domain model for the {@code Manifest}.
 * <p>
 * Please note that this is a request scoped bean. This mean that for each http request a
 * different instance will be initialized by Spring and used to serve this specific request.</p>
 * <p>
 *  The Manifest is an overall description of the structure and properties of the digital representation
 *  of an object. It carries information needed for the viewer to present the digitized content to the user,
 *  such as a title and other descriptive information about the object or the intellectual work that
 *  it conveys. Each manifest describes how to present a single object such as a book, a photograph,
 *  or a statue.</p>
 *
 * Please note that this is a request scoped bean. This means that for each http request a
 * different instance will be initialized by Spring and used to serve this specific request.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RequestScope
@Component
public class ManifestV3Generator  {

    private URI id;
    private Label label;
    private List<Metadata> metadata;
    private Summary summary;
    private List<ContentResource<?>> thumbnails;
    private Provider provider;
    private String identifier;
    private ImageContent logo;

    /**
     * Sets the mandatory manifest identifier.
     * @param identifier manifest identifier
     */
    public void setIdentifier(@NotNull String identifier) {

        if (identifier.isEmpty()) {
            throw new RuntimeException("Invalid manifest identifier. Cannot be an empty string.");
        }
        this.identifier = identifier;
    }

    public ManifestV3Generator(String id, String labelText) {
        this.id = validateURI(id);
        this.label = new Label(labelText);
        this.metadata = new ArrayList<>();
        this.summary = null;
        this.thumbnails = new ArrayList<>();
        this.provider = null;
    }

    // Méthode pour valider une URI
    private URI validateURI(String uriString) {
        try {
            return new URI(uriString);
        } catch (Exception e) {
            throw new IllegalArgumentException("L'ID fourni n'est pas une URI valide");
        }
    }

    // Méthode pour ajouter des métadonnées
    public void addMetadata(Metadata... metadataArray) {
        for (Metadata data : metadataArray) {
            if (data != null) {
                metadata.add(data);
            }
        }
    }

    // Méthode pour définir le résumé
    public void setSummary(String summaryText) {
        this.summary = new Summary(summaryText);
    }


    // Méthode pour obtenir les métadonnées
    public List<Metadata> getMetadata() {
        return metadata;
    }

    // Méthode pour obtenir le résumé
    public Summary getSummary() {
        return summary;
    }

     public Provider getProvider() {
        return provider;
     }

    // Méthode pour définir le fournisseur
    public void setProvider(Provider provider) {
        this.provider = provider;
    }




   public Resource<Manifest> generateResource() {
       if (identifier == null) {
           throw new RuntimeException("Le manifeste nécessite un identifiant.");
       }
       Manifest manifest = new Manifest(id.toString(), label.getString());

       // Créer une liste de métadonnées à partir des valeurs fournies
       List<Metadata> metadataList = new ArrayList<>();
       for (Metadata metadataItem : metadata) {
           // Utiliser les valeurs de la liste metadata pour créer des MetadataEntry
           metadataList.add(new Metadata(metadataItem.getLabel().getString(), metadataItem.getValue().toString()));
       }
       // Définir les métadonnées sur le manifeste
       manifest.setMetadata(metadataList);
       // Retourner l'instance de Manifest avec les paramètres spécifiés
       return manifest;
   }


}
