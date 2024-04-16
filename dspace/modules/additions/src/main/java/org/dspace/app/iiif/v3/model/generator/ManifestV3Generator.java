package org.dspace.app.iiif.v3.model.generator;

// Importations des classes nécessaires
import info.freelibrary.iiif.presentation.v3.Manifest;
import info.freelibrary.iiif.presentation.v3.Resource;
import info.freelibrary.iiif.presentation.v3.properties.Summary;
import info.freelibrary.iiif.presentation.v3.properties.Label;
import info.freelibrary.iiif.presentation.v3.properties.Metadata;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import org.dspace.content.Item;
import org.dspace.core.Context;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// Annotation indiquant que cette classe est un composant Spring géré par le conteneur
@RequestScope
@Component
public class ManifestV3Generator implements IIIFV3Resource {

    // Initialisation du logger
    private static final Log log = LogFactory.getLog(ManifestV3Generator.class);

    // Déclaration des variables membres
    private String identifier;
    private Label label;
    private Summary summary;
    private List<Metadata> metadataList = new ArrayList<>();

    // Méthode pour définir l'identifiant du manifeste
    public void setID(String identifier) {
        this.identifier = identifier;
    }

    // Méthode pour définir l'étiquette du manifeste
    public void setLabel(String langTag, String value) {
        this.label = new Label(langTag, value);
    }

    // Méthode pour définir le résumé du manifeste
    public void setSummary(String summary) {
        this.summary = new Summary(summary);
    }

    // Méthode pour ajouter des métadonnées au manifeste
    public void addMetadata(String field, String value) {
        Metadata metadata = new Metadata(field, value);
        this.metadataList.add(metadata);
    }

    // Méthode pour générer la ressource manifeste
    @Override
    public Resource<Manifest> generateResource() {
        // Vérification si l'identifiant est défini
        if (identifier == null) {
            throw new RuntimeException("The Manifest resource requires an identifier.");
        }

        // Création du manifeste avec l'identifiant et l'étiquette
        Manifest manifest = new Manifest(identifier, label);

        // Vérification si un résumé est défini et l'ajouter au manifeste
        if (summary != null) {
            manifest.setSummary(summary);
        }

        // Retourner le manifeste généré
        return manifest;
    }
}
