/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.model.generator;

import info.freelibrary.iiif.presentation.v3.properties.SeeAlso;
import info.freelibrary.iiif.presentation.v3.properties.Label;
import info.freelibrary.iiif.presentation.v3.properties.Rendering;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * A generator class for creating external links.
 */
@RequestScope
@Component("ExternalLinksGeneratorV3")
public class ExternalLinksGenerator {

    private static final Log log = LogFactory.getLog(ExternalLinksGenerator.class);

    private String type;
    private String label;
    private String identifier;
    private String format;
    private URI profile;
    private String langue;

    /**
     * Sets the identifier of the external link.
     *
     * @param identifier The identifier of the external link
     * @return The ExternalLinksGenerator object
     */
    public ExternalLinksGenerator setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * Sets the format of the external link.
     *
     * @param format The format of the external link
     * @return The ExternalLinksGenerator object
     */
    public ExternalLinksGenerator setFormat(String format) {
        this.format = format;
        return this;
    }

    /**
     * Sets the label of the external link.
     *
     * @param label The label of the external link
     * @return The ExternalLinksGenerator object
     */
    public ExternalLinksGenerator setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the type of the external link.
     *
     * @param type The type of the external link
     * @return The ExternalLinksGenerator object
     */
    public ExternalLinksGenerator setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the profile URI of the external link.
     *
     * @param profile The profile URI of the external link
     * @return The ExternalLinksGenerator object
     */
    public ExternalLinksGenerator setProfile(URI profile) {
        this.profile = profile;
        return this;
    }

    /**
     * Sets the profile URI of the external link.
     *
     * @param profile The profile URI of the external link
     * @return The ExternalLinksGenerator object
     */
    public ExternalLinksGenerator setLangue(String langue) {
        this.langue = langue;
        return this;
    }


    /**
     * Generates a list of SeeAlso resources based on the configured properties.
     *
     * @return A list of SeeAlso resources
     */
    public List<SeeAlso> generateSeeAlsoList() {
        List<SeeAlso> seeAlsoList = new ArrayList<>();
        SeeAlso seeAlso = new SeeAlso(URI.create(identifier), format);
        if (label != null) {
            if(langue != null){
            Label lb = new Label(langue, label);
            seeAlso.setLabel(lb);
            }
            else {
            seeAlso.setLabel(label);
            }
        }
        if (type != null) {
            seeAlso.setType(type);
        }
        if (profile != null) {
            seeAlso.setProfile(profile);
        }

        seeAlsoList.add(seeAlso);
        return seeAlsoList;
    }

}
