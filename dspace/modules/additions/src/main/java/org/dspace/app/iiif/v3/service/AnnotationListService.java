package org.dspace.app.iiif.v3.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.iiif.v3.model.generator.AnnotationGenerator;
import org.dspace.app.iiif.v3.model.generator.AnnotationListGenerator;
import org.dspace.app.iiif.v3.model.generator.CanvasGenerator;
import org.dspace.app.iiif.v3.model.generator.ContentAsTextGenerator;
import org.dspace.app.iiif.v3.model.generator.ExternalLinksGenerator;
import org.dspace.app.iiif.v3.service.utils.IIIFUtils;
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

import info.freelibrary.iiif.presentation.v3.MediaType;
import info.freelibrary.iiif.presentation.v3.TextContent;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RequestScope
@Component("AnnotationListServiceV3")
public class AnnotationListService  extends AbstractResourceService {

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

    private AnnotationGenerator annotationGenerator;

    private String TRANSCRIPTIONS_BUNDLE_NAME = null;

    public AnnotationListService(ConfigurationService configurationService) {
        TRANSCRIPTIONS_BUNDLE_NAME = configurationService.getProperty("iiif.transcriptions.bundle");
    }

    public String getSeeAlsoAnnotations(Context context, UUID id)
            throws RuntimeException {

        Item item;
        try {
            item = itemService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        annotationList.setIdentifier(IIIF_ENDPOINT + id + "/manifest/seeAlso/v3");

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
        return annotationList.generateResource().toString();
    }

    public String getTranscriptionsAnnotations(Context context, UUID iId, UUID bId, String cId, String annotationListId) {

        annotationList.setIdentifier(IIIF_ENDPOINT + annotationListId);

        try {
            Bitstream bts = bitstreamService.find(context, bId);
            if (bts != null) {
                List<Bundle> bundles = bts.getBundles();
                if (bundles.size() > 0) {
                    Bundle bdl = bundles.get(0);
                    List<Item> items = bdl.getItems();
                    if (items.size() >0) {
                        Item item = items.get(0);
                        List<Bundle> itemBundles = item.getBundles();
                        for (Bundle itemBundle: itemBundles) {
                            if (itemBundle.getName().equals(TRANSCRIPTIONS_BUNDLE_NAME)) {
                                String rootName = utils.getRootName(bts.getName());
                                String canvasId = IIIF_ENDPOINT + iId + "/canvas/" + cId + "/v3";
                                List<Bitstream> tBitstreams = itemBundle.getBitstreams();
                                for ( Bitstream tBitstream: tBitstreams ) {
                                    if (utils.getRootName(tBitstream.getName()).startsWith(rootName)) {
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
            // If there is an exception, simply return an empty annotationList.
        }

        //return utils.asJson(annotationList.generateResource());
        return annotationList.generateResource().toString();
    }

    private ExternalLinksGenerator getLinksGenerator(String mimetype, Bitstream bitstream) {
        String identifier = BITSTREAM_PATH_PREFIX + "/" + bitstream.getID() + "/content";

        return new ExternalLinksGenerator()
                .setFormat(mimetype)
                .setLabel(bitstream.getName())
                .setIdentifier(identifier);
    }

    private ContentAsTextGenerator getContentGenerator(Context context, Bitstream bts, String mimetype) {
        ContentAsTextGenerator generator = new ContentAsTextGenerator();
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Utils.copy(bitstreamService.retrieve(context, bts), bos);
            generator.setText(bos.toString());
        } catch (IOException | SQLException | AuthorizeException e) {
            // Logging?
        }
        return generator;
    }
}
