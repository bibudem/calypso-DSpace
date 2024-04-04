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

@Component("thumbProfileUtilV3")
public class ThumbProfileUtil {

    @Autowired
    ProfileGenerator profile;

    /**
     * Utility method for obtaining the thumbnail image service profile.
     * Calling from this utility provides a unique instance of the
     * autowired property. Necessary because a single canvas resource contains
     * both thumbnail and images.
     *
     * @return the thumbnail service profile
     */
    public ProfileGenerator getThumbnailProfile() throws
            RuntimeException {
        profile.setIdentifier("http://iiif.io/api/image/3/level0.json");
        return profile;
    }

}
