package org.dspace.app.iiif.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

import org.dspace.app.iiif.model.generator.AnnotationGenerator;
import org.dspace.app.iiif.model.generator.ExternalLinksGenerator;
import org.dspace.app.iiif.service.utils.IIIFUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.sharedcanvas.AnnotationList;

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
public class CanvasTranscriptionsService extends AbstractResourceService {

    private boolean addTranscriptions = false; 
    private String TRANSCRIPTIONS_BUNDLE_NAME = null;
    private String TRANSCRIPTIONS_LABEL = null;

    /**
     * Creates a service with the appropriate configuration.
     * 
     * @param configurationService  DSpace configuration service
     */
    public CanvasTranscriptionsService(ConfigurationService configurationService) {
        setConfiguration(configurationService);

        // Check if we generate a link to transcriptions for canvas
        TRANSCRIPTIONS_BUNDLE_NAME = configurationService.getProperty("iiif.transcriptions.bundle");
        TRANSCRIPTIONS_LABEL = configurationService.getProperty("iiif.transcriptions.label");

        // We generate the transcriptions links iif the 2 configuration properties are set
        if (TRANSCRIPTIONS_BUNDLE_NAME != null && TRANSCRIPTIONS_LABEL != null) addTranscriptions = true;
    }

    /**
     * Method to generate a list of links to be included in the otherContent for the
     * canvas. Will return an empty list if no links are found.
     * 
     * @param context       DSpace context
     * @param item          The item generating the IIIF manifest
     * @param bitstream     The bitstream generating the canvas
     * @return              A list of links (can be emtpy, but wont be null)
     */
    public AnnotationList getCanvasTranscriptions(Context context, Item item, Bitstream bitstream, String linkUrl) {

        // Only proceed if configuration is set
        if ( !addTranscriptions || context == null || item == null || bitstream == null || linkUrl == null) return null;


        // Check if we have the specified bundle, otherwise return null
        Bundle transcriptionsBundle = null;
        List<Bundle> bdls = item.getBundles();
        for ( Bundle bdl: bdls ) {
            if ( bdl.getName().equals(TRANSCRIPTIONS_BUNDLE_NAME) ) {
                transcriptionsBundle = bdl;
                break;
            }
        }
        if ( transcriptionsBundle == null ) return null;

        // Check if we have a bitstream with a correct name
        String name = IIIFUtils.getRootName(bitstream.getName());
        List<Bitstream> bts = transcriptionsBundle.getBitstreams();
        for ( Bitstream bt: bts ) {
            if ( IIIFUtils.getRootName(bt.getName()).startsWith(name)) {
                // We have at least one bitstream, generate a link and stop searching
                AnnotationList al = new AnnotationList(linkUrl);
                al.setLabel(new PropertyValue(TRANSCRIPTIONS_LABEL));
                return al;
                //return new ExternalLinksGenerator(linkUrl)
                //            .setType(AnnotationGenerator.TYPE)
                //            .setLabel(TRANSCRIPTIONS_LABEL);
            }            
        }

        // No bitstream found, return null
        return null;
    }
}