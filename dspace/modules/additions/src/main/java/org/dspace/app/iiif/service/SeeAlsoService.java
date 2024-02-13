/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.app.iiif.model.generator.AnnotationGenerator;
import org.dspace.app.iiif.model.generator.ExternalLinksGenerator;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This service provides methods for creating {@code seAlso} external link. There should be a single instance of
 * this service per request. The {@code @RequestScope} provides a single instance created and available during
 * complete lifecycle of the HTTP request.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RequestScope
@Component
public class SeeAlsoService extends AbstractResourceService {

    private static final String SEE_ALSO_LABEL = "More descriptions of this resource";
    protected String SEEALSO_BUNDLE_NAME;

    public SeeAlsoService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
        SEEALSO_BUNDLE_NAME = configurationService.getProperty("iiif.seealso.bundle");
    }

    /**
     * Generates all the seeAlso links for the item. The seeAlso may be
     * a reference to an annotation list (legacy method) or one per bitstream
     * in a specific bundle.
     * 
     * @param item      The DSpace item
     * @param context   The DSpace context
     * @return          A list of seeAlsos, may be empty but not null
     */
    public List<ExternalLinksGenerator> getSeeAlsos(Item item, Context context) {

        // The list to return
        List<ExternalLinksGenerator> elgs = new ArrayList<ExternalLinksGenerator>();
    
        if (SEEALSO_BUNDLE_NAME != null) {
            // New method: we add bitstreams from the specified bundle
            List<Bundle> bundles = item.getBundles();
            if ( bundles != null ) {
                for (Bundle bundle : bundles) {
                    if ( bundle.getName().equals(SEEALSO_BUNDLE_NAME) ) {
                        List<Bitstream> bitstreams = bundle.getBitstreams();
                        for (Bitstream bitstream : bitstreams) {
                            String mimeType = null;
                            try {
                                mimeType = bitstream.getFormat(context).getMIMEType();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            // We add the bitstream
                            String id = BITSTREAM_PATH_PREFIX + "/" + bitstream.getID() + "/content";
                            elgs.add(
                                new ExternalLinksGenerator(id)
                                    .setLabel(utils.getIIIFLabel(bitstream, bitstream.getName()))
                                    .setFormat(mimeType)
                            );
                        }
                    }
                }
            }
        }
        else {
            // Original method: return a link to an annotation list
            elgs.add(new ExternalLinksGenerator(IIIF_ENDPOINT + item.getID() + "/manifest/seeAlso")
            .setType(AnnotationGenerator.TYPE)
            .setLabel(SEE_ALSO_LABEL));
        }
        return elgs;
    }

}
