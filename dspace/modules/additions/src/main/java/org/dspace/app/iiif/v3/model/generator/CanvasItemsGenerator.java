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

import info.freelibrary.iiif.presentation.v3.Canvas;
import info.freelibrary.iiif.presentation.v3.Resource;
import info.freelibrary.iiif.presentation.v3.Manifest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This generator wraps the domain model for a Presentation API 2.1.1 {@code Sequence}. The IIIF sequence
 * conveys the ordering of the views of the object.
 *
 * <p>Please note that this is a request scoped bean. This means that for each http request a
 * different instance will be initialized by Spring and used to serve this specific request.</p>
 *
 */
@RequestScope
@Component("CanvasItemsGeneratorV3")
public class CanvasItemsGenerator implements IIIFV3Resource {

    private String identifier;
    private final List<Canvas> canvas = new ArrayList<>();
    private String language;

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void defaultLanguage(String defaultLanguage) {
        this.language = defaultLanguage;
    }

    /**
     * Adds a single {@code Canvas} to the sequence.
     * @param canvas generator for canvas
     */
    public String addCanvas(CanvasGenerator canvas) {
        Canvas resource = (Canvas) canvas.generateResource();
        this.canvas.add(resource);
        return resource.getID().toString();
    }

    @Override
    public Resource<Manifest> generateResource() {
        Manifest items = new Manifest(identifier, this.language);
        items.setCanvases(canvas);
        return items;
    }
}
