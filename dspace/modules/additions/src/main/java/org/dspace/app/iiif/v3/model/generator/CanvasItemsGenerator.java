package org.dspace.app.iiif.v3.model.generator;

import java.util.ArrayList;
import java.util.List;

import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import de.digitalcollections.iiif.model.sharedcanvas.Sequence;
import org.dspace.app.iiif.model.generator.CanvasGenerator;
import org.dspace.app.iiif.model.generator.IIIFResource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Presentation API 3.0 CanvasItemsGenerator.
 * Sequence is removed in 3.0, canvases go directly into Manifest.items.
 */
@RequestScope
@Component
public class CanvasItemsGenerator implements IIIFResource {

    private String identifier;
    private final List<Canvas> canvas = new ArrayList<>();

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String addCanvas(CanvasGenerator canvas) {
        Canvas resource = (Canvas) canvas.generateResource();
        this.canvas.add(resource);
        return resource.getIdentifier().toString();
    }

    @Override
    public Resource<Sequence> generateResource() {
        Sequence items = new Sequence(identifier);
        items.setCanvases(canvas);
        return items;
    }
}
