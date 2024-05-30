/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.model.generator;

import info.freelibrary.iiif.presentation.v3.ImageContent;
import info.freelibrary.iiif.presentation.v3.Resource;
import jakarta.validation.constraints.NotNull;

public class ImageContentGenerator implements IIIFV3Resource {

    private final ImageContent imageContent;

    /**
     * Constructs an ImageContentGenerator with the given identifier.
     *
     * @param identifier the identifier for the image content
     */
    public ImageContentGenerator(@NotNull String identifier) {
        imageContent = new ImageContent(identifier);
    }

    /**
     * Sets the format (mimetype) of the image content.
     *
     * @param mimetype the mimetype to set
     * @return this ImageContentGenerator instance
     */
    public ImageContentGenerator setFormat(String mimetype) {
        imageContent.setFormat(mimetype);
        return this;
    }

    /**
     * Adds an ImageServiceGenerator to the image content.
     *
     * @param imageService the ImageServiceGenerator to add
     * @return this ImageContentGenerator instance
     */
    public ImageContentGenerator addService(ImageServiceGenerator imageService) {
        this.imageContent.setServices(imageService.generateService());
        return this;
    }

    @Override
    public ImageContent generateResource() {
        return imageContent;
    }
}
