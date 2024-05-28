package org.dspace.app.iiif.v3.model.generator;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import info.freelibrary.iiif.presentation.v3.Annotation;
import info.freelibrary.iiif.presentation.v3.PaintingAnnotation;
import info.freelibrary.iiif.presentation.v3.AnnotationBody;
import info.freelibrary.iiif.presentation.v3.Canvas;
import info.freelibrary.iiif.presentation.v3.Manifest;
import info.freelibrary.iiif.presentation.v3.Resource;
import jakarta.validation.constraints.NotNull;

/**
 * Generator for an {@code annotation} model. Annotations associate content resources and commentary with a canvas.
 * This is used for the {@code seeAlso} annotation and Search response.
 */
public class AnnotationGenerator implements IIIFV3Resource {

    private String motivation;
    private String identifier;
    private CanvasGenerator canvasGenerator;
    private ContentAsTextGenerator contentAsTextGenerator;
    private ExternalLinksGenerator externalLinksGenerator;
    private List<Manifest> manifests = new ArrayList<>();

    public static final String TYPE = "sc:AnnotationList";
    public static final String PAINTING = "sc:painting";
    public static final String COMMENTING = "oa:commenting";
    public static final String LINKING = "oa:linking";


    public AnnotationGenerator(@NotNull String identifier) {
        if (identifier.isEmpty()) {
            throw new IllegalArgumentException("Invalid annotation identifier. Cannot be an empty string.");
        }
        this.identifier = identifier;
    }

    public AnnotationGenerator(@NotNull String identifier, @NotNull String motivation) {
        this(identifier);
        this.motivation = motivation;
    }

    /**
     * Sets the motivation field. Required.
     *
     * @param motivation the motivation
     * @return this {@code AnnotationGenerator}
     */
    public AnnotationGenerator setMotivation(@NotNull String motivation) {
        this.motivation = motivation;
        return this;
    }

    /**
     * Sets the canvas that is associated with this annotation.
     *
     * @param canvas the canvas generator
     * @return this {@code AnnotationGenerator}
     */
    public AnnotationGenerator setOnCanvas(CanvasGenerator canvas) {
        this.canvasGenerator = canvas;
        return this;
    }

    /**
     * Sets a text resource for this annotation.
     *
     * @param contentAsText the text content generator
     * @return this {@code AnnotationGenerator}
     */
    public AnnotationGenerator setResource(ContentAsTextGenerator contentAsText) {
        this.contentAsTextGenerator = contentAsText;
        return this;
    }

    /**
     * Sets an external link for this annotation.
     *
     * @param otherContent the external link generator
     * @return this {@code AnnotationGenerator}
     */
    public AnnotationGenerator setResource(ExternalLinksGenerator otherContent) {
        this.externalLinksGenerator = otherContent;
        return this;
    }

    /**
     * Set the within property for this annotation. This property is a list of manifests. The property is renamed to partOf in v3.
     * Used by search result annotations.
     *
     * @param within the list of manifests
     * @return this {@code AnnotationGenerator}
     */
    public AnnotationGenerator setWithin(List<ManifestV3Generator> within) {
        for (ManifestV3Generator manifest : within) {
            this.manifests.add((Manifest) manifest.generateResource());
        }
        return this;
    }

    @Override
    public Resource<PaintingAnnotation> generateResource() {
        if (identifier == null) {
            throw new RuntimeException("Missing the required identifier for the annotation page.");
        }
        PaintingAnnotation annotationPage = new PaintingAnnotation(URI.create(identifier), null);

        // Remplacer les annotations manquantes par une liste vide pour éviter les erreurs
        List<AnnotationBody<?>> annotationBodies = new ArrayList<>();
        if (canvasGenerator != null) {
            annotationBodies.add((AnnotationBody<?>) canvasGenerator.generateResource());
        }
        if (contentAsTextGenerator != null) {
            annotationBodies.add((AnnotationBody<?>) contentAsTextGenerator.generateResource());
        }
        if (externalLinksGenerator != null) {
            annotationBodies.add((AnnotationBody<?>) externalLinksGenerator.generateSeeAlsoList());
        }
        annotationPage.setBodies(annotationBodies);
        return annotationPage;
    }

}
