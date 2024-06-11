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
import info.freelibrary.iiif.presentation.v3.Annotation;
import info.freelibrary.iiif.presentation.v3.AnnotationPage;
import info.freelibrary.iiif.presentation.v3.SupplementingAnnotation;
import info.freelibrary.iiif.presentation.v3.AnnotationBody;

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
    private List<Metadata> metadata = new ArrayList<>();
    private AnnotationPage<SupplementingAnnotation> annotation;
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
     * Adds an AnnotationPage to the canvas.
     *
     * @return this {@code CanvasGenerator}
     */
    public CanvasGenerator addAnnotationPage(AnnotationPage<SupplementingAnnotation> annotation) {
        this.annotation = annotation;
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
        if (height != null && width != null) {
            canvas.setWidthHeight(width, height);
        }

        if (thumbnail != null) {
            canvas.setThumbnails(thumbnail);
        }
        if (!metadata.isEmpty()) {
            canvas.setMetadata(metadata);
        }
       if (annotation != null) {
           canvas.setSupplementingPages(annotation);
       }


        return canvas;
    }
}
