/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.app.iiif.v3.service.ManifestV3Service;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import org.dspace.app.iiif.service.utils.IIIFUtils;



/**
 * IIIF Service facade to support IIIF Presentation and Search API requests.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Service
public class IIIFV3ServiceFacade {

    @Autowired
    ItemService itemService;

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    ManifestV3Service manifestService;

    @Autowired
    IIIFUtils utils;

    /**
     * The manifest response contains sufficient information for the client to initialize itself
     * and begin to display something quickly to the user. The manifest resource represents a single
     * object and any intellectual work or works embodied within that object. In particular it
     * includes the descriptive, rights and linking information for the object. It then embeds
     * the sequence(s) of canvases that should be rendered to the user.
     *
     * Returns manifest for single DSpace item.
     *
     * @param id DSpace Item uuid
     * @return manifest as JSON
     */
    @Cacheable(key = "'V33'+ #id.toString()", cacheNames = "manifests")
    @PreAuthorize("hasPermission(#id, 'ITEM', 'READ')")
    public String getManifest(Context context, UUID id)
        throws ResourceNotFoundException {
        Item item;
        try {
            item = itemService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (item == null || !utils.isIIIFEnabled(item)) {
            throw new ResourceNotFoundException("IIIF manifest for id " + id + " not found");
        }

        return manifestService.getManifest(item, context);
    }

}
