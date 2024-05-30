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

import info.freelibrary.iiif.presentation.v3.Range;
import info.freelibrary.iiif.presentation.v3.Range.Item;
import info.freelibrary.iiif.presentation.v3.Canvas;
import info.freelibrary.iiif.presentation.v3.Resource;
import info.freelibrary.iiif.presentation.v3.properties.ViewingDirection;
import info.freelibrary.iiif.presentation.v3.AccompanyingCanvas;
import org.dspace.app.iiif.v3.service.RangeService;
import jakarta.validation.constraints.NotNull;

public class RangeGenerator implements IIIFV3Resource {

    private String identifier;
    private String label;
    private final List<ViewingDirection> viewingDirection = new ArrayList<>();
    private final List<Range.Item> canvasList = new ArrayList<>();
    private final List<Range.Item> rangesList = new ArrayList<>();

    private final RangeService rangeService;

    public RangeGenerator(RangeService rangeService) {
        this.rangeService = rangeService;
    }

    public RangeGenerator setID(@NotNull String identifier) {
        if (identifier.isEmpty()) {
            throw new RuntimeException("Invalid range identifier. Cannot be an empty string.");
        }
        this.identifier = identifier;
        return this;
    }

    public String getID() {
        return identifier;
    }

    public RangeGenerator setLabel(String label) {
        this.label = label;
        return this;
    }

    public RangeGenerator addViewingDirection(ViewingDirection viewing) {
        viewingDirection.add(viewing);
        return this;
    }

    public RangeGenerator addCanvas(CanvasGenerator canvas) {
       canvasList.add(new Item((Canvas) canvas.generateResource()));
       return this;
   }

   public void addSubRange(RangeGenerator range) {
       range.setID(identifier + "-" + rangesList.size());
       RangeGenerator rangeReference = rangeService.getRangeReference(range);
       rangesList.add(new Item((Range) rangeReference.generateResource()));
   }

    @Override
    public Resource<Range> generateResource() {
        if (identifier == null) {
            throw new RuntimeException("The Range resource requires an identifier.");
        }
        Range range;
        if (label != null) {
            range = new Range(identifier, label);
        } else {
            range = new Range(identifier);
        }
        for (ViewingDirection direction : viewingDirection) {
            range.setViewingDirection(direction);
        }
        for (Item canvas : canvasList) {
            range.getItems().add(canvas);
        }
        for (Item rangeResource : rangesList) {
           range.getItems().add(rangeResource);
        }

        return range;
    }
}

