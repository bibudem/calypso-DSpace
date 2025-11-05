/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.service.utils;

import org.dspace.app.iiif.v3.model.generator.ProfileGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import info.freelibrary.iiif.presentation.v3.services.ImageService3;

@Component("ImageProfileUtilV3")
public class ImageProfileUtil {

    @Autowired
    ProfileGenerator profile;

    /**
     * Utility method for obtaining the image service profile.
     *
     * @return  image service profile
     */
    public ProfileGenerator getImageProfile() throws
            RuntimeException {
        profile.setIdentifier(ImageService3.Profile.LEVEL_ONE.uri().toString());
        return profile;
    }
}
