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
import info.freelibrary.iiif.presentation.v3.properties.I18n;
import info.freelibrary.iiif.presentation.v3.properties.Label;
import org.dspace.core.I18nUtil;


public class MetadataEntryGenerator  {

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

    /**
     * Generate a metadata entry with language tag.
     * @param langTag The language tag for the metadata entry
     * @return The generated metadata
     */
    public Metadata generateValue(String langTag) {
        Value metadataValues;
        if (rest != null && rest.length > 0) {
            I18n i18nValue = new I18n(langTag, rest[0]);
            metadataValues = new Value(i18nValue);
        } else {
            I18n i18nValue = new I18n(langTag, value);
            metadataValues = new Value(i18nValue);
        }
        Label label = new Label(langTag, I18nUtil.getMessage("metadata." + field));
        return new Metadata(label, metadataValues);
    }
}
