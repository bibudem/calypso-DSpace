/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.model.generator;

import info.freelibrary.iiif.presentation.v3.Service;

/**
 * Interface for iiif service generators.
 */
public interface IIIFService {

    /**
     * Creates and returns a service model
     * @return a service model
     */
    Service generateService();
}
