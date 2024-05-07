package org.dspace.app.iiif.v3.model.generator;

import info.freelibrary.iiif.presentation.v3.Manifest;
import info.freelibrary.iiif.presentation.v3.Resource;
import info.freelibrary.iiif.presentation.v3.properties.Summary;
import info.freelibrary.iiif.presentation.v3.properties.Label;
import info.freelibrary.iiif.presentation.v3.properties.Metadata;
import info.freelibrary.iiif.presentation.v3.properties.Rendering;
import info.freelibrary.iiif.presentation.v3.ImageContent;
import info.freelibrary.iiif.presentation.v3.properties.SeeAlso;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;
import java.util.ArrayList;
import java.net.URI;
import java.util.Optional;
import org.dspace.app.iiif.v3.model.generator.ExternalLinksGenerator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A generator for creating IIIF version 3 manifests.
 */
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
    private List<ExternalLinksGenerator> seeAlsos = new ArrayList<>();
    private List<Rendering> renderings = new ArrayList<>();

    /**
     * Creates a new instance of ManifestV3Generator.
     */
    public ManifestV3Generator() {
        metadataList = new ArrayList<>();
    }

    /**
     * Sets the identifier for the manifest.
     *
     * @param identifier The manifest identifier
     */
    public void setID(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Sets the label for the manifest.
     *
     * @param langTag The language tag for the label
     * @param value   The value of the label
     */
    public void setLabel(String langTag, String value) {
        this.label = new Label(langTag, value);
    }

    /**
     * Sets the summary for the manifest.
     *
     * @param summary The summary of the manifest
     */
    public void setSummary(String summary) {
        this.summary = new Summary(summary);
    }

    /**
     * Adds metadata to the manifest.
     *
     * @param label The label of the metadata
     * @param value The value of the metadata
     */
    public void addMetadata(String label, String value) {
        metadataList.add(new Metadata(label, value));
    }

    /**
     * Sets the rights URI for the manifest.
     *
     * @param rights The URI of the rights statement
     */
    public void setRights(String rights) {
        this.rightsURI = URI.create(rights);
    }

    /**
     * Adds a thumbnail to the manifest.
     *
     * @param thumbnail The thumbnail of the manifest
     */
    public void addThumbnail(ImageContentGenerator thumbnail) {
        this.thumbnail = (ImageContent) thumbnail.generateResource();
    }

    /**
     * Adds a SeeAlso reference to the manifest.
     *
     * @param seeAlso The SeeAlso reference to add
     */
    public void addSeeAlso(ExternalLinksGenerator seeAlso) {
        this.seeAlsos.add(seeAlso);
    }
    /**
     * Adds a rendering annotation to the Sequence. The rendering is a link to an external resource intended
     * for display or download by a human user. This is typically going to be a PDF file.
     *
     * @param rendering The rendering to add
     */
    public void addRendering(Rendering rendering) {
        this.renderings.add(rendering);
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

        if (!seeAlsos.isEmpty()) {
            for (ExternalLinksGenerator sa : seeAlsos) {
                manifest.setSeeAlsoRefs(sa.generateSeeAlsoList());
            }
        }

        if (!renderings.isEmpty()) {
            for (Rendering rendering : renderings) {
                manifest.setRenderings(rendering);
            }
        }

        return manifest;
    }
}
