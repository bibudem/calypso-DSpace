package org.dspace.app.iiif.v3.model.generator;

import info.freelibrary.iiif.presentation.v3.Manifest;
import info.freelibrary.iiif.presentation.v3.Resource;
import info.freelibrary.iiif.presentation.v3.properties.Summary;
import info.freelibrary.iiif.presentation.v3.properties.I18n;
import info.freelibrary.iiif.presentation.v3.properties.Label;
import info.freelibrary.iiif.presentation.v3.properties.Metadata;
import info.freelibrary.iiif.presentation.v3.properties.Rendering;
import info.freelibrary.iiif.presentation.v3.ImageContent;
import info.freelibrary.iiif.presentation.v3.properties.SeeAlso;
import info.freelibrary.iiif.presentation.v3.Range;
import info.freelibrary.iiif.presentation.v3.Canvas;
import info.freelibrary.iiif.presentation.v3.properties.ViewingDirection;
import info.freelibrary.iiif.presentation.v3.properties.Value;

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

    private Manifest manifest;

    private String identifier;
    private Label label;
    private Summary summary;
    private String rights;
    private List<Metadata> metadataList;
    private ImageContent thumbnail;
    private List<ExternalLinksGenerator> seeAlsos = new ArrayList<>();
    private List<Rendering> renderings = new ArrayList<>();
    private final List<Range> ranges = new ArrayList<>();
    private final List<Canvas> canvas = new ArrayList<>();
    private ViewingDirection viewingDirection;

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
    public void setSummary(String langTag, String value) {
        this.summary = new Summary(new I18n(langTag, value));
    }


    /**
     * Adds metadata to the manifest.
     *
     * @param label The label of the metadata
     * @param value The value of the metadata
     */
    public void addMetadata(String langTag, String labelValue, String value) {
        Label metadataLabel = new Label(langTag, labelValue);
        I18n i18nValue = new I18n(langTag, value);
        Value metadataValue = new Value(i18nValue);
        metadataList.add(new Metadata(metadataLabel, metadataValue));
    }




    /**
     * Sets the rights URI for the manifest.
     *
     * @param rights The URI of the rights statement
     */
    public void setRights(String rights) {
        this.rights = rights;
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

    /**
     * Adds optional Range to the manifest's structures element.
     * @param rangeGenerator to add
     */
    public void addRange(RangeGenerator rangeGenerator) {
        ranges.add((Range) rangeGenerator.generateResource());
    }

   /**
    * Adds add single (mandatory) {@code sequence} to the manifest. In IIIF Presentation API 3.0 "items"
    *
    * @param items Liste des modèles de canevas
    */
   // Remplacez la méthode addCanvasItems par la suivante :
   public void addCanvasItems(List<CanvasGenerator> items) {
       for (CanvasGenerator canvas : items) {
           Canvas canvasResource = (Canvas) canvas.generateResource();
           this.canvas.add(canvasResource);
       }
   }


    /**
     * Sets the viewing direction. In the context of IIIF Presentation API version 3.0, this becomes the "behavior" of the manifest.
     *
     * @param viewingDirection The viewing direction to set
     */
    public void addViewingDirection(String viewingDirection) {
        this.viewingDirection = ViewingDirection.fromString(viewingDirection);
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

        if (rights != null) {
            manifest.setRights(rights);
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

        if (ranges.size() > 0) {
            for (Range range : ranges) {
                manifest.addRanges(range);
            }
        }

        if (canvas != null) {
            manifest.setCanvases(canvas);
        }

        if (viewingDirection != null) {
            manifest.setViewingDirection(viewingDirection);
        }


        return manifest;
    }
}
