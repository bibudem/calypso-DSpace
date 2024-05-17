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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
/**
 * This generator wraps the domain model for a Presentation API 3 {@code Sequence}. The IIIF sequence
 *
 */
@RequestScope
@Component("CanvasItemsGeneratorV3")
public class CanvasItemsGenerator implements IIIFV3Resource {

private static final Log log = LogFactory.getLog(ManifestV3Generator.class);

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
     public Resource<Canvas> generateResource() {
        Canvas items = new Canvas(identifier);
        return items;
     }

}
