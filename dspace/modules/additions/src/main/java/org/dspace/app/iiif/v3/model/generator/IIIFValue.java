/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.model.generator;

/**
 * Interface générique pour les générateurs de valeurs IIIF.
 * @param <T> Le type de valeur généré
 */
public interface IIIFValue<T> {

    /**
     * creates and returns a value model.
     * @return a value model.
     */
    T generateValue();
}
