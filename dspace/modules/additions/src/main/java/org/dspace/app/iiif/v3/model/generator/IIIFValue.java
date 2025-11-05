/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.model.generator;

/**
 * A generic interface for IIIF value generators in v3.
 *
 * @param <T> the type of the value produced
 */
public interface IIIFValue<T> {
    T generateValue();
}
