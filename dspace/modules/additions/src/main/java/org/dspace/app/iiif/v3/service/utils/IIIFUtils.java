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


}
