/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.iiif.model.generator.AnnotationGenerator;
import org.dspace.app.iiif.model.generator.AnnotationListGenerator;
import org.dspace.app.iiif.model.generator.CanvasGenerator;
import org.dspace.app.iiif.model.generator.ContentAsTextGenerator;
import org.dspace.app.iiif.model.generator.ExternalLinksGenerator;
import org.dspace.app.iiif.service.utils.IIIFUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import de.digitalcollections.iiif.model.MimeType;
import de.digitalcollections.iiif.model.openannotation.ContentAsText;

/**
 * This service provides methods for creating an {@code Annotation List}. There should be a single instance of
 * this service per request. The {@code @RequestScope} provides a single instance created and available during
 * complete lifecycle of the HTTP request.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RequestScope
@Component
public class AnnotationListService extends AbstractResourceService {


    @Autowired
    IIIFUtils utils;

    @Autowired
    ItemService itemService;

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    BitstreamFormatService bitstreamFormatService;

    @Autowired
    AnnotationListGenerator annotationList;

    private String TRANSCRIPTIONS_BUNDLE_NAME = null;

    public AnnotationListService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
        TRANSCRIPTIONS_BUNDLE_NAME = configurationService.getProperty("iiif.transcriptions.bundle");
    }

    /**
     * Returns an AnnotationList for bitstreams in the OtherContent bundle.
     * These resources are not appended directly to the manifest but can be accessed
     * via the seeAlso link.
     *
     * The semantics of this linking property may be extended to full text files, but
     * machine readable formats like ALTO, METS, and schema.org descriptions are preferred.
     *
     * @param context DSpace context
     * @param id bitstream uuid
     * @return AnnotationList as JSON
     */
    public String getSeeAlsoAnnotations(Context context, UUID id)
            throws RuntimeException {

        // We need the DSpace item to proceed
        Item item;
        try {
            item = itemService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        // AnnotationList requires an identifier.
        annotationList.setIdentifier(IIIF_ENDPOINT + id + "/manifest/seeAlso");

        // Get the "seeAlso" bitstreams for the item. Add
        // Annotations for each bitstream found.
        List<Bitstream> bitstreams = utils.getSeeAlsoBitstreams(item);
        for (Bitstream bitstream : bitstreams) {
            BitstreamFormat format;
            String mimetype;
            try {
                format = bitstream.getFormat(context);
                mimetype = format.getMIMEType();
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            AnnotationGenerator annotation = new AnnotationGenerator(IIIF_ENDPOINT + bitstream.getID())
                .setMotivation(AnnotationGenerator.LINKING)
                .setResource(getLinksGenerator(mimetype, bitstream));
            annotationList.addResource(annotation);
        }
        return utils.asJson(annotationList.generateResource());
    }

    /**
     * Find transcriptions for the bitstream and return an annotationList with the content
     * of these transcriptions.
     * 
     * @param context           The DSpace context
     * @param iId               The Item UUID
     * @param bId               The Bitstream UUID
     * @param cId               The canvas ID
     * @param annotationListId  The ID of the annotation list itself (the part after the base IIIF service URL)
     * @return
     */
    public String getTranscriptionsAnnotations(Context context, UUID iId, UUID bId, String cId, String annotationListId) {

        // Set the ID for the annotationList
        annotationList.setIdentifier(IIIF_ENDPOINT + annotationListId);

        // First get the DSpace object
        try {
            Bitstream bts = bitstreamService.find(context, bId);
            if (bts != null) {
                // Get the item (assume first bundle and first item)
                List<Bundle> bundles = bts.getBundles();
                if (bundles.size() > 0) {
                    Bundle bdl = bundles.get(0);
                    List<Item> items = bdl.getItems();
                    if (items.size() >0) {
                        // We have the item, try to find the required bundle
                        Item item = items.get(0);
                        List<Bundle> itemBundles = item.getBundles();
                        for (Bundle itemBundle: itemBundles) {
                            if (itemBundle.getName().equals(TRANSCRIPTIONS_BUNDLE_NAME)) {
                                // We have the required bundle, let's look at the bitstreams
                                String rootName = IIIFUtils.getRootName(bts.getName());
                                String canvasId = IIIF_ENDPOINT + iId + "/canvas/" + cId;
                                List<Bitstream> tBitstreams = itemBundle.getBitstreams();
                                for ( Bitstream tBitstream: tBitstreams ) {
                                    // The name must begin with the name of the bitstream
                                    if (IIIFUtils.getRootName(tBitstream.getName()).startsWith(rootName)) {
                                        // The format must be HTML or plain text or JSON
                                        BitstreamFormat format = tBitstream.getFormat(context);
                                        if ( format.getMIMEType().equals("text/plain") || format.getMIMEType().equals("text/html") ) {
                                            AnnotationGenerator annotation = new AnnotationGenerator(IIIF_ENDPOINT + tBitstream.getID())
                                            .setMotivation(AnnotationGenerator.PAINTING)
                                            .setOnCanvas(new CanvasGenerator(canvasId))
                                            .setResource(getContentGenerator(context, tBitstream, format.getMIMEType()));
                                            annotationList.addResource(annotation);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // If there is an exception, simply return en empty annotationList.
        }

        // We will return the annotationList in JSON
        return utils.asJson(annotationList.generateResource());
    }

    private ExternalLinksGenerator getLinksGenerator(String mimetype, Bitstream bitstream) {
        String identifier = BITSTREAM_PATH_PREFIX
                + "/"
                + bitstream.getID()
                + "/content";

        return new ExternalLinksGenerator(identifier)
                .setFormat(mimetype)
                .setLabel(bitstream.getName());
    }

    private ContentAsTextGenerator getContentGenerator(Context context, Bitstream bts, String mimetype) {
        ContentAsTextGenerator generator = new ContentAsTextGenerator();
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Utils.copy(bitstreamService.retrieve(context, bts), bos);
            generator.setText(bos.toString());
            generator.setFormat(mimetype);
        }
        catch (IOException | SQLException | AuthorizeException e) {
            // Logging?
        }
        return generator;
    }
}
