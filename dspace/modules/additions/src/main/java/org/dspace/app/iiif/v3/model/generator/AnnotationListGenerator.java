/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.model.generator;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import info.freelibrary.iiif.presentation.v3.Annotation;
import info.freelibrary.iiif.presentation.v3.PaintingAnnotation;
import info.freelibrary.iiif.presentation.v3.AnnotationBody;
import info.freelibrary.iiif.presentation.v3.Resource;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This generator wraps the domain model for the {@code AnnotationList}.
 *
 * <p>Please note that this is a request scoped bean. This means that for each http request a
 * different instance will be initialized by Spring and used to serve this specific request.</p>
 *
 * <p>The model represents an ordered list of annotations.</p>
 */
@RequestScope
@Component("AnnotationListGeneratorV3")
public class AnnotationListGenerator implements IIIFV3Resource {

    private String identifier;
    private List<Annotation> annotations = new ArrayList<>();

    /**
     * Sets the required annotation identifier.
     * @param identifier the annotation identifier
     */
    public void setIdentifier(@NotNull String identifier) {
        this.identifier = identifier;
    }

    /**
     * Adds Annotation resource to the annotation list.
     * @param annotation an annotation generator
     */
    public void addResource(AnnotationGenerator annotation) {
        this.annotations.add((Annotation) annotation.generateResource());
    }

    @Override
    public Resource<PaintingAnnotation> generateResource() {
        if (identifier == null) {
            throw new RuntimeException("Missing the required identifier for the annotation page.");
        }
        PaintingAnnotation annotationPage = new PaintingAnnotation(URI.create(identifier), null);
        List<AnnotationBody<?>> annotationBodies = annotations.stream()
                .map(annotation -> (AnnotationBody<?>) annotation)
                .collect(Collectors.toList());
        annotationPage.setBodies(annotationBodies);
        return annotationPage;
    }
}
