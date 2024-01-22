package org.dspace.app.iiif.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

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
 * This service creates a list of seeAlso links for a canvas (image = bitstream).
 * 
 * A link will be created for every bitstream in a specified bundle
 * (iiif.ocr.bundle) with a name that begins with the same name as the bundle
 * (minus the file extension). Configuration properties iiif.ocr.seealso.label
 * and iiif.ocr.seealso.profiles must also be set.
 */
@RequestScope
@Component
public class CanvasSeeAlsoService extends AbstractResourceService {

    private boolean addSeeAlso = false; 
    private String SEEALSO_BUNDLE_NAME = null;
    private String SEEALSO_LABEL = null;
    private URI SEEALSO_PROFILE = null;

    /**
     * Creates a service with the appropriate configuration.
     * 
     * @param configurationService  DSpace configuration service
     */
    public CanvasSeeAlsoService(ConfigurationService configurationService) {
        setConfiguration(configurationService);

        // Check if we generate a seeAlso for canvas
        SEEALSO_BUNDLE_NAME = configurationService.getProperty("iiif.ocr.bundle");
        SEEALSO_LABEL = configurationService.getProperty("iiif.ocr.seealso.label");
        String profile = configurationService.getProperty("iiif.ocr.seealso.profile");
        if (profile != null) {
            try {
                SEEALSO_PROFILE = new URI(profile);
            }
            catch (URISyntaxException e) {}
        }
        // We generate seeAlso links iif the 3 configuration properties are set
        if (SEEALSO_BUNDLE_NAME != null && SEEALSO_LABEL != null && SEEALSO_PROFILE != null) addSeeAlso = true;
    }

    /**
     * Method to generate a list of links to be included in the seeAlso for the
     * canvas. Will return an empty list if no links are found.
     * 
     * @param context       DSpace context
     * @param item          The item generating the IIIF manifest
     * @param bitstream     The bitstream generating the canvas
     * @return              A list of links (can be emtpy, but wont be null)
     */
    public List<ExternalLinksGenerator> getCanvasSeeAlso(Context context, Item item, Bitstream bitstream) {

        List<ExternalLinksGenerator> links = new ArrayList<ExternalLinksGenerator>();

        // Only proceed if configuration is set
        if ( !addSeeAlso || context == null || item == null || bitstream == null ) return links;


        // Check if we have the specified bundle, otherwise return an empty list
        Bundle seeAlsoBundle = null;
        List<Bundle> bdls = item.getBundles();
        for ( Bundle bdl: bdls ) {
            if ( bdl.getName().equals(SEEALSO_BUNDLE_NAME) ) {
                seeAlsoBundle = bdl;
                break;
            }
        }
        if ( seeAlsoBundle == null ) return links;

        // Check if we have a bitstream with a correct name
        String name = getRootName(bitstream.getName());
        List<Bitstream> bts = seeAlsoBundle.getBitstreams();
        for ( Bitstream bt: bts ) {
            if ( name.equals(getRootName(bt.getName()))) {
                try {
                    links.add(new ExternalLinksGenerator(BITSTREAM_PATH_PREFIX + "/" + bt.getID() + "/content")
                            .setFormat(bt.getFormat(context).getMIMEType())
//                            .setType(AnnotationGenerator.TYPE)
                            .setLabel(SEEALSO_LABEL)
                            .setProfile(SEEALSO_PROFILE));
                }
                catch (SQLException e) {}
            }            
        }
        return links;
    }

    /**
     * Check a String (filename) and returns the portion before the last occurrence of "."
     * @param name  The String to check
     * @return  The portion before ".", or the string itself if not found
     */
    private String getRootName(String name) {
        String root = name;
        int dotPos = root.lastIndexOf(".");
        if (dotPos > 0) {
            root = name.substring(0, dotPos);
        }
        return root;
    }

}