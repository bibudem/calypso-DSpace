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
    private final List<ImageContent> images = new ArrayList<>();
    private Label label;
    private String langTag;
    private Integer height;
    private Integer width;
    private ImageContent thumbnail;
    private ImageContent image;

    /**
     * Constructs a {@code CanvasGenerator} with the provided identifier.
     *
     * @param identifier the identifier for the canvas
     * @throws RuntimeException if the provided identifier is empty
     */
    public CanvasGenerator(@NotNull String identifier) {
        if (identifier.isEmpty()) {
            throw new RuntimeException("Invalid canvas identifier. Cannot be an empty string.");
        }
        this.identifier = identifier;
    }

    /**
     * Gets the identifier of the canvas.
     *
     * @return the canvas identifier
     */
    public String getID() {
        return identifier;
    }

    /**
     * Sets the label for the canvas.
     *
     * @param label the label to set
     * @return this {@code CanvasGenerator}
     */
    public CanvasGenerator setLabel(Label label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the width and height dimensions for the canvas.
     *
     * @param width  the width of the canvas
     * @param height the height of the canvas
     * @return this {@code CanvasGenerator}
     */
    public CanvasGenerator setWidthHeight(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Adds an image content to the canvas.
     *
     * @param image the image content to add
     * @return this {@code CanvasGenerator}
     */
    public CanvasGenerator addImage(ImageContent image) {
        images.add(image);
        return this;
    }

    /**
     * Adds a thumbnail to the canvas.
     *
     * @param thumbnail the thumbnail image content to add
     * @return this {@code CanvasGenerator}
     */
    public CanvasGenerator addThumbnail(ImageContent thumbnail) {
        this.thumbnail = thumbnail;
        return this;
    }

    /**
     * Adds metadata to the canvas.
     *
     * @param langTag the language tag for the metadata
     * @param field   the field name for the metadata
     * @param value   the value of the metadata
     * @param rest    additional values for the metadata
     */
    public void addMetadata(String langTag, String field, String value, String... rest) {
        MetadataEntryGenerator metadataEntryGenerator = new MetadataEntryGenerator();
        metadataEntryGenerator.setField(field);
        metadataEntryGenerator.setValue(value, rest);
        metadata.add(metadataEntryGenerator.generateValue(langTag));
    }

    @Override
    public Resource<Canvas> generateResource() {
        Canvas canvas;
        if (identifier == null) {
            throw new RuntimeException("The Canvas resource requires an identifier.");
        }
        if (label != null) {
            canvas = new Canvas(identifier, label);
        } else {
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
        if (!metadata.isEmpty()) {
            for (Metadata meta : metadata) {
                canvas.setMetadata(meta);
            }
        }
        return canvas;
    }
}
