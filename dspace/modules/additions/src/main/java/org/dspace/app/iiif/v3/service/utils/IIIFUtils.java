/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.service.utils;

import static org.dspace.iiif.util.IIIFSharedUtils.METADATA_IIIF_HEIGHT_QUALIFIER;
import static org.dspace.iiif.util.IIIFSharedUtils.METADATA_IIIF_IMAGE_ELEMENT;
import static org.dspace.iiif.util.IIIFSharedUtils.METADATA_IIIF_SCHEMA;
import static org.dspace.iiif.util.IIIFSharedUtils.METADATA_IIIF_WIDTH_QUALIFIER;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import info.freelibrary.iiif.presentation.v3.Resource;
import info.freelibrary.iiif.presentation.v3.Manifest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.iiif.model.ObjectMapperFactory;// il faut adapter a la version 3
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.iiif.IIIFApiQueryService;
import org.dspace.iiif.util.IIIFSharedUtils;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component("IIIFUtils iiifv3")
public class IIIFUtils {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(IIIFUtils.class);
    // The DSpace bundle for other content related to item.
    protected static final String OTHER_CONTENT_BUNDLE = "OtherContent";

    // The canvas position will be appended to this string.
    private static final String CANVAS_PATH_BASE = "/canvas/c";

    // metadata used to enable the iiif features on the item
    public static final String METADATA_IIIF_ENABLED = "dspace.iiif.enabled";
    // metadata used to enable the iiif search service on the item
    public static final String METADATA_IIIF_SEARCH_ENABLED = "iiif.search.enabled";
    // metadata used to override the title/name exposed as label to iiif client
    public static final String METADATA_IIIF_LABEL = "iiif.label";
    // metadata used to override the description/abstract exposed as label to iiif client
    public static final String METADATA_IIIF_DESCRIPTION = "iiif.description";
    // metadata used to set the position of the resource in the iiif manifest structure
    public static final String METADATA_IIIF_TOC = "iiif.toc";
    // metadata used to set the naming convention (prefix) used for all canvas that has not an explicit name
    public static final String METADATA_IIIF_CANVAS_NAMING = "iiif.canvas.naming";
    // metadata used to set the iiif viewing hint
    public static final String METADATA_IIIF_VIEWING_HINT  = "iiif.viewing.hint";
    // metadata used to set the width of the canvas that has not an explicit name
    public static final String METADATA_IMAGE_WIDTH = METADATA_IIIF_SCHEMA + "." + METADATA_IIIF_IMAGE_ELEMENT
        + "." + METADATA_IIIF_WIDTH_QUALIFIER;
    // metadata used to set the height of the canvas that has not an explicit name
    public static final String METADATA_IMAGE_HEIGHT = METADATA_IIIF_SCHEMA + "." + METADATA_IIIF_IMAGE_ELEMENT
        + "." + METADATA_IIIF_HEIGHT_QUALIFIER;

    // string used in the metadata toc as separator among the different levels
    public static final String TOC_SEPARATOR = "|||";
    // convenient constant to split a toc in its components
    public static final String TOC_SEPARATOR_REGEX = "\\|\\|\\|";

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    private ObjectMapper mapper;

    // get module subclass.
    protected SimpleModule iiifModule = ObjectMapperFactory.getIiifModule();

    /**
     * Serializes the json response.
     * @param resource to be serialized
     * @return
     */
    public String asJson(Resource<Manifest> resource) {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //mapper.registerModule(iiifModule);  il faut adapter a la version 3
        try {
           return mapper.writeValueAsString(resource);
        } catch (JsonProcessingException e) {
           throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Return all the bitstreams in the item to be used as IIIF resources
     *
     * @param context the DSpace Context
     * @param item    the DSpace item
     * @return a not null list of bitstreams to use as IIIF resources in the
     *         manifest
     */
    public List<Bitstream> getIIIFBitstreams(Context context, Item item) {
        List<Bitstream> bitstreams = new ArrayList<Bitstream>();
        for (Bundle bnd : IIIFSharedUtils.getIIIFBundles(item)) {
            bitstreams
                    .addAll(getIIIFBitstreams(context, bnd));
        }
        return bitstreams;
    }

    /**
     * Return all the bitstreams in the bundle to be used as IIIF resources
     *
     * @param context the DSpace Context
     * @param bundle    the DSpace Bundle
     * @return a not null list of bitstreams to use as IIIF resources in the
     *         manifest
     */
    public List<Bitstream> getIIIFBitstreams(Context context, Bundle bundle) {
        return bundle.getBitstreams().stream().filter(b -> isIIIFBitstream(context, b))
                .collect(Collectors.toList());
    }

     /**
     * Utility method to check is a bitstream can be used as IIIF resources
     *
     * @param b the DSpace bitstream to check
     * @return true if the bitstream can be used as IIIF resource
     */
    public boolean isIIIFBitstream(Context context, Bitstream b) {
        return checkImageMimeType(getBitstreamMimeType(b, context)) && b.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_ENABLED))
                .noneMatch(m -> m.getValue().equalsIgnoreCase("false") || m.getValue().equalsIgnoreCase("no"));
    }

    /**
     * Returns the bitstream mime type
     *
     * @param bitstream DSpace bitstream
     * @param context   DSpace context
     * @return mime type
     */
    public String getBitstreamMimeType(Bitstream bitstream, Context context) {
        try {
            BitstreamFormat bitstreamFormat = bitstream.getFormat(context);
            return bitstreamFormat.getMIMEType();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Tests for image mimetype. Presentation API 2.1.1 canvas supports images only.
     * Other media types introduced in version 3.
     * @param mimetype
     * @return true if an image
     */
    private boolean checkImageMimeType(String mimetype) {
        if (mimetype != null && mimetype.contains("image/")) {
            return true;
        }
        return false;
    }

     /**
     * Return the custom iiif label for the resource or the provided default if none
     *
     * @param dso          the dspace object to use as iiif resource
     * @param defaultLabel the default label to return if none is specified in the
     *                     metadata
     * @return the iiif label for the dspace object
     */
     public String getIIIFLabel(DSpaceObject dso, String defaultLabel) {
        return dso.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_LABEL))
                .findFirst().map(m -> m.getValue()).orElse(defaultLabel);
     }


}
