/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.model.generator;

import info.freelibrary.iiif.presentation.v3.TextContent;
import info.freelibrary.iiif.presentation.v3.Resource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Generator for a text annotation.
 */
@Scope("prototype")
@Component("ContentAsTextGeneratorV3")
public class ContentAsTextGenerator implements IIIFV3Resource {

    private String text;

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public Resource<TextContent> generateResource() {
        if (text == null) {
            throw new RuntimeException("Missing required text for the text annotation.");
        }
        return new TextContent(text);
    }
}
