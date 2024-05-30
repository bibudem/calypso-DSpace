/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import info.freelibrary.iiif.presentation.v3.utils.JSON;
import org.springframework.stereotype.Component;

@Component("ObjectMapperFactoryV3")
public class ObjectMapperFactory {

    private ObjectMapperFactory() {}

    /**
    * Gets the Jackson ObjectMapper configured for IIIF Presentation API v3.
    * @return Jackson mapper
    */
    public static ObjectMapper getIiifObjectMapper() {
        // Return the Jackson ObjectMapper configured for IIIF Presentation API v3
        return new ObjectMapper();
    }

    /**
    * Gets the Jackson SimpleModule configured for IIIF Presentation API v3.
    * @return SimpleModule
    */
    public static SimpleModule getIiifModule() {
        // Return the Jackson SimpleModule configured for IIIF Presentation API v3
        return new SimpleModule();
    }
}
