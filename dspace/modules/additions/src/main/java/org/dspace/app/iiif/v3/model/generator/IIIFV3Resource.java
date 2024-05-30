/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.model.generator;

import info.freelibrary.iiif.presentation.v3.Resource;

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
