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

    public ImageContentGenerator(@NotNull String identifier) {
        imageContent = new ImageContent(identifier);
    }

    public ImageContentGenerator setFormat(String mimetype) {
        imageContent.setFormat(mimetype);
        return this;
    }

    public ImageContentGenerator addService(ImageServiceGenerator imageService) {
        this.imageContent.setServices(imageService.generateService());
        return this;
    }

    @Override
    public Resource<ImageContent> generateResource() {
        return imageContent;
    }
}
