/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.service;

import org.apache.logging.log4j.Logger;
import org.dspace.app.iiif.v3.model.generator.CanvasGenerator;
import org.dspace.app.iiif.v3.model.generator.CanvasItemsGenerator;
import info.freelibrary.iiif.presentation.v3.Canvas;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;
import java.util.ArrayList;
import org.dspace.content.Bundle;

@RequestScope
@Component("CanvasItemsServiceV3")
public class CanvasItemsService extends AbstractResourceService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CanvasItemsService.class);
    protected String DEFAULT_LANGUAGE;

    int counter = 0;

    @Autowired
    CanvasItemsGenerator itemGenerator;

    @Autowired
    CanvasService canvasService;

    /**
     * Constructor for CanvasItemsService.
     *
     * @param configurationService The DSpace configuration service.
     */
    public CanvasItemsService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    /**
    * Get the sequence for the specified item.
    *
    * @param item The DSpace item.
    * @return The canvas items generator.
    */
    public CanvasItemsGenerator getCanvasItem(Item item) {
        itemGenerator.setIdentifier(IIIF_ENDPOINT + item.getID() + "/item/i0");
        return itemGenerator;
    }


    /**
    * Add a canvas to the sequence for the specified item.
    *
    * @param context    The DSpace context.
    * @param item       The DSpace item.
    * @param bnd        The DSpace bundle.
    * @param bitstream  The DSpace bitstream.
    * @return The canvas generator.
    */
    public CanvasGenerator addCanvas(Context context, Item item, Bundle bnd, Bitstream bitstream, String DEFAULT_LANGUAGE) {
        String mimeType = utils.getBitstreamMimeType(bitstream, context);
        String manifestId = item.getID().toString();
        CanvasGenerator canvasGenerator =
                canvasService.getCanvas(context, manifestId, bitstream, bnd, item, counter, mimeType);
        itemGenerator.addCanvas(canvasGenerator);
        counter++;
        return canvasGenerator;
    }

}
