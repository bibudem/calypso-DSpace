/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.service;

import info.freelibrary.iiif.presentation.v3.properties.SeeAlso;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import org.dspace.app.iiif.v3.model.generator.ExternalLinksGenerator;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A service for generating SeeAlso resources.
 */

@RequestScope
@Component("SeeAlsoServiceV3")
public class SeeAlsoService extends AbstractResourceService {

    private static final Log log = LogFactory.getLog(SeeAlsoService.class);
    private static final String SEE_ALSO_LABEL = "More descriptions of this resource";

    protected String SEEALSO_BUNDLE_NAME;
    protected String DEFAULT_LANGUAGE;

    /**
     * Constructs a new SeeAlsoService instance with the provided configuration service.
     *
     * @param configurationService The configuration service
     */
    public SeeAlsoService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
        SEEALSO_BUNDLE_NAME = configurationService.getProperty("iiif.seealso.bundle");
    }

    /**
     * Retrieves the SeeAlso resources for the specified item.
     *
     * @param item    The item for which to retrieve SeeAlso resources
     * @param context The DSpace context
     * @return A list of SeeAlso resources
     */
    public List<ExternalLinksGenerator> getSeeAlsos(Item item, Context context) {
        List<ExternalLinksGenerator> elgs = new ArrayList<>();

        if (SEEALSO_BUNDLE_NAME != null) {
            List<Bundle> bundles = item.getBundles();
            if (bundles != null) {
                for (Bundle bundle : bundles) {
                    if (bundle.getName().equals(SEEALSO_BUNDLE_NAME)) {
                        List<Bitstream> bitstreams = bundle.getBitstreams();
                        for (Bitstream bitstream : bitstreams) {
                            String mimeType = null;
                            try {
                                mimeType = bitstream.getFormat(context).getMIMEType();
                            } catch (SQLException e) {
                                log.error("Error retrieving MIME type", e);
                            }
                            String id = BITSTREAM_PATH_PREFIX + "/" + bitstream.getID() + "/content";
                            ExternalLinksGenerator elg = new ExternalLinksGenerator().setIdentifier(id)
                                    .setLabel(utils.getIIIFLabel(bitstream, bitstream.getName()))
                                    .setFormat(mimeType);
                            elgs.add(elg);
                        }
                    }
                }
            }
        } else {
            String id = IIIF_ENDPOINT + item.getID() + "/manifest/v3/seeAlso";
            ExternalLinksGenerator elg = new ExternalLinksGenerator().setIdentifier(id)
                    .setLabel(SEE_ALSO_LABEL);
            elgs.add(elg);
        }
        return elgs;
    }
}
