/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.model.generator;

import info.freelibrary.iiif.presentation.v3.properties.Metadata;
import info.freelibrary.iiif.presentation.v3.properties.Value;
import info.freelibrary.iiif.presentation.v3.properties.Label;
import org.dspace.core.I18nUtil;

/**
 * Wraps the domain model metadata property.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class MetadataEntryGenerator implements IIIFValue {

    private String field;
    private String value;
    private String[] rest;

    /**
     * Set metadata field name.
     * @param field field name
     */
    public MetadataEntryGenerator setField(String field) {
        this.field = field;
        return this;
    }

    /**
     * Set metadata value.
     * @param value metadata value
     */
    public MetadataEntryGenerator setValue(String value, String... rest) {
        this.value = value;
        this.rest = rest;
        return this;
    }

    @Override
    public Metadata generateValue() {
        Value metadataValues;
        if (rest != null && rest.length > 0) {
            metadataValues = new Value(rest);
        } else {
            metadataValues = new Value(value);
        }
        Label label = new Label(I18nUtil.getMessage("metadata." + field));
        return new Metadata(label, metadataValues);
    }

}
