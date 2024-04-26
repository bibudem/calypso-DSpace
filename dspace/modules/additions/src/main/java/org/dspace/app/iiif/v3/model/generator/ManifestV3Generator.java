package org.dspace.app.iiif.v3.model.generator;

import info.freelibrary.iiif.presentation.v3.Manifest;
import info.freelibrary.iiif.presentation.v3.Resource;
import info.freelibrary.iiif.presentation.v3.properties.Summary;
import info.freelibrary.iiif.presentation.v3.properties.Label;
import info.freelibrary.iiif.presentation.v3.properties.Metadata;
import info.freelibrary.iiif.presentation.v3.ImageContent;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;
import java.util.ArrayList;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@RequestScope
@Component
public class ManifestV3Generator implements IIIFV3Resource {

    private static final Log log = LogFactory.getLog(ManifestV3Generator.class);

    private String identifier;
        private Label label;
        private Summary summary;
        private URI rightsURI;
        private List<Metadata> metadataList;
        private ImageContent thumbnail;

        public ManifestV3Generator() {
            metadataList = new ArrayList<>();
        }

        public void setID(String identifier) {
            this.identifier = identifier;
        }

        public void setLabel(String langTag, String value) {
            this.label = new Label(langTag, value);
        }

        public void setSummary(String summary) {
            this.summary = new Summary(summary);
        }

        public void addMetadata(String label, String value) {
            metadataList.add(new Metadata(label, value));
        }

        public void setRights(String rights) {
            this.rightsURI = URI.create(rights);
        }

        /**
        * Adds optional thumbnail image resource to manifest.
        * @param thumbnail an image content generator
        */
        public void addThumbnail(ImageContentGenerator thumbnail) {
             this.thumbnail = (ImageContent) thumbnail.generateResource();
         }

        @Override
        public Resource<Manifest> generateResource() {
            if (identifier == null) {
                throw new RuntimeException("The Manifest resource requires an identifier.");
            }

            Manifest manifest = new Manifest(identifier, label);

            if (thumbnail != null) {
                manifest.setThumbnails(thumbnail);
            }

            if (!metadataList.isEmpty()) {
                Metadata[] metadataArray = metadataList.toArray(new Metadata[metadataList.size()]);
                manifest.setMetadata(metadataArray);
            }

            if (summary != null) {
                manifest.setSummary(summary);
            }

            return manifest;
        }
}
