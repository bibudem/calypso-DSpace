package org.dspace.app.iiif.v3.model.generator;

import info.freelibrary.iiif.presentation.v3.Resource;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface pour les générateurs de ressources IIIF v3.
 */
public interface IIIFV3Resource {

    /**
     * Crée et retourne un modèle de ressource.
     * @return modèle de ressource
     */
    Resource<?> generateResource();
}
