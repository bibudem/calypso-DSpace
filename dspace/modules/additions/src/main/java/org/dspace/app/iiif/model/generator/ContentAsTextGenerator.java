/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.model.generator;

import de.digitalcollections.iiif.model.MimeType;
import de.digitalcollections.iiif.model.openannotation.ContentAsText;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;

import java.util.Locale;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Generator for a text annotation.
 */
@Scope("prototype")
@Component
public class ContentAsTextGenerator implements IIIFResource {

    private String text;
    private String format;
    private String type;
    private String language;

    public void setText(String text) {
        this.text = text;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public Resource<ContentAsText> generateResource() {
        if (text == null) {
            throw new RuntimeException("Missing required text for the text annotation.");
        }
        ContentAsText content = new ContentAsText(text);
        if ( format != null ) content.setFormat(MimeType.fromTypename(format));
        if ( type != null ) content.setType(type);
        if ( language != null ) content.setLanguage(new Locale(language));
        return content;
    }
}
