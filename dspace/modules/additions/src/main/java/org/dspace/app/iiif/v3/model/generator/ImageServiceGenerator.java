package org.dspace.app.iiif.v3.model.generator;

import info.freelibrary.iiif.presentation.v3.Service;
import info.freelibrary.iiif.presentation.v3.services.ImageService;
import info.freelibrary.iiif.presentation.v3.services.ImageService3;

public class ImageServiceGenerator implements IIIFService {

    private final ImageService3 imageService;

    public ImageServiceGenerator(String identifier) {
        this.imageService = new ImageService3(identifier);
    }

    /**
     * Sets the IIIF image profile.
     * @param profile a profile generator
     */
    public ImageServiceGenerator setProfile(ProfileGenerator profile) {
        ImageService.Profile serviceProfile = profile.generateValue();
        imageService.setProfile(serviceProfile);
        return this;
    }

    @Override
    public Service generateService() {
        return imageService;
    }
}
