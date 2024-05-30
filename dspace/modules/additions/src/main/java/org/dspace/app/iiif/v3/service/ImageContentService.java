/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.service;

import java.util.UUID;

import org.dspace.app.iiif.v3.model.generator.ImageContentGenerator;
import org.dspace.app.iiif.v3.model.generator.ImageServiceGenerator;
import org.dspace.app.iiif.v3.model.generator.ProfileGenerator;
import org.dspace.services.ConfigurationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;


@RequestScope
@Component("ImageContentServiceV3")
public class ImageContentService extends AbstractResourceService {

    /**
    * Constructor that initializes the ImageContentService with the given ConfigurationService.
    *
    * @param configurationService the configuration service to use
    */
    public ImageContentService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    /**
    * Generates an ImageContentGenerator with specified UUID, MIME type, profile, and path.
    *
    * @param uuid the unique identifier for the image content
    * @param mimetype the MIME type of the image content
    * @param profile the profile generator to use
    * @param path the path to the image content
    * @return an ImageContentGenerator configured with the specified parameters
    */
    protected ImageContentGenerator getImageContent(UUID uuid, String mimetype, ProfileGenerator profile, String path) {
        return new ImageContentGenerator(IMAGE_SERVICE + uuid + path)
                .setFormat(mimetype)
                .addService(getImageService(profile, uuid.toString()));
    }

    /**
    * Generates an ImageContentGenerator with a specified identifier.
    *
    * @param identifier the identifier for the image content
    * @return an ImageContentGenerator configured with the specified identifier
    */
    protected ImageContentGenerator getImageContent(String identifier) {
        return new ImageContentGenerator(identifier);
    }

    /**
    * Generates an ImageServiceGenerator with the specified profile and UUID.
    *
    * @param profile the profile generator to use
    * @param uuid the unique identifier for the image service
    * @return an ImageServiceGenerator configured with the specified parameters
    */
    private ImageServiceGenerator getImageService(ProfileGenerator profile, String uuid) {
        return new ImageServiceGenerator(IMAGE_SERVICE + uuid).setProfile(profile);
    }

}
