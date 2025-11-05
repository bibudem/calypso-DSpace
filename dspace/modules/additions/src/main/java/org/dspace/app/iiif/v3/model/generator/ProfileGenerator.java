/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.model.generator;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import info.freelibrary.iiif.presentation.v3.Service;
import info.freelibrary.iiif.presentation.v3.services.ImageService3;

/**
 * This class wraps the domain model service profile.
 */
@Scope("prototype")
@Component("ProfileGeneratorV3")
public class ProfileGenerator implements IIIFValue<ImageService3.Profile> {

    private String identifier;

    /**
     * Set the identifier for the profile.
     *
     * @param identifier The identifier for the profile
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public ImageService3.Profile generateValue() {
        try {
            return ImageService3.Profile.fromString(identifier);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid identifier provided", e);
        }
    }
}
