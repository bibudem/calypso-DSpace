package org.dspace.app.iiif.v3.service;

import org.dspace.app.iiif.v3.model.generator.ManifestV3Generator;
import org.dspace.content.Item;
import org.dspace.core.Context;

import info.freelibrary.iiif.presentation.v3.Manifest;
import info.freelibrary.iiif.presentation.v3.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Component
@RequestScope
public class ManifestV3Service {

    private static final Log log = LogFactory.getLog(ManifestV3Service.class);

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Returns JSON manifest response for a DSpace item.
     *
     * @param item    the DSpace Item
     * @param context the DSpace context
     * @return manifest as JSON
     */
    public String getManifest(Item item, Context context) {

        // Créer une instance du générateur de manifeste
         ManifestV3Generator generator = new ManifestV3Generator();

         // Configurer le générateur en fonction de l'élément et du contexte
         generator.setID(item.getID().toString());
         generator.setLabel("fr", item.getName());


         // Ajouter un résumé ou une autre configuration au générateur si nécessaire
         generator.setSummary("Summary of the item");

        // Générer la ressource manifeste
        Resource<Manifest> manifestResource = generator.generateResource();

        try {
            // Convertir la ressource manifeste en JSON
            return objectMapper.writeValueAsString(manifestResource);
        } catch (Exception e) {
            // Gérer l'exception en fonction de vos besoins
            log.error("Error generating JSON for manifest", e);
            return null;
        }
    }
}
