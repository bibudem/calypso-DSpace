/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.model.generator;

import java.util.ArrayList;
import java.util.List;

import org.dspace.services.ConfigurationService;

import info.freelibrary.iiif.presentation.v3.Canvas;
import info.freelibrary.iiif.presentation.v3.ImageContent;
import info.freelibrary.iiif.presentation.v3.properties.Metadata;
import info.freelibrary.iiif.presentation.v3.Resource;
import info.freelibrary.iiif.presentation.v3.Manifest;
import info.freelibrary.iiif.presentation.v3.properties.Label;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;


/**
 * This generator wraps the domain model for a single {@code Canvas}.
 */

@RequestScope
@Component("CanvasGeneratorV3")
public class CanvasGenerator implements IIIFV3Resource {

    private final String identifier;
    private final List<Metadata> metadata = new ArrayList<>();
    private final List<ImageContent> images = new ArrayList();
    private Label label;
    private String language;
    private Integer height;
    private Integer width;
    private ImageContent thumbnail;
    private ImageContent image;


    public CanvasGenerator(@NotNull String identifier) {
        if (identifier.isEmpty()) {
            throw new RuntimeException("Invalid canvas identifier. Cannot be an empty string.");
        }
        this.identifier = identifier;
    }

    public String getID() {
        return identifier;
    }


    public CanvasGenerator setLabel(Label label) {
        this.label = label;
        return this;
    }

    public CanvasGenerator setWidthHeight(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public CanvasGenerator addImage(ImageContent image) {
        images.add((ImageContent) image);
        return this;
    }

    public CanvasGenerator addThumbnail(ImageContent thumbnail) {
        this.thumbnail = (ImageContent) thumbnail;
        return this;
    }

    public void addMetadata(String field, String value, String... rest) {
        MetadataEntryGenerator metadataEntryGenerator = new MetadataEntryGenerator();
        metadataEntryGenerator.setField(field);
        metadataEntryGenerator.setValue(value, rest);
        metadata.add(metadataEntryGenerator.generateValue());
    }

    @Override
    public Resource<Canvas> generateResource() {
        Canvas canvas;
        if (identifier == null) {
            throw new RuntimeException("The Canvas resource requires an identifier.");
        }
        if (label != null) {
            canvas = new Canvas(identifier, label);
            }
        else {
            canvas = new Canvas(identifier);
            }
        if (thumbnail != null) {
            if (height == null || width == null) {
                throw new RuntimeException("The Canvas resource requires both height and width dimensions.");
            }
            canvas.setWidthHeight(width, height);

            for (ImageContent res : images) {
                canvas.setThumbnails(res);
            }
            if (thumbnail != null) {
                canvas.setThumbnails(thumbnail);
            }
        }
        if (metadata.size() > 0) {
            for (Metadata meta : metadata) {
                canvas.setMetadata(meta);
            }
        }

        return canvas;
    }
}
