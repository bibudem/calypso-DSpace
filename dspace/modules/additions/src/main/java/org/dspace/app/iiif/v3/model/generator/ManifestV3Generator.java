/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.model.generator;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.dspace.app.iiif.model.generator.IIIFResource;

import de.digitalcollections.iiif.model.ImageContent;
import de.digitalcollections.iiif.model.MetadataEntry;
import de.digitalcollections.iiif.model.OtherContent;
import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.enums.ViewingHint;
import de.digitalcollections.iiif.model.search.ContentSearchService;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import de.digitalcollections.iiif.model.sharedcanvas.Range;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import de.digitalcollections.iiif.model.sharedcanvas.Sequence;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import jakarta.validation.constraints.NotNull;

/**
 * This generator wraps a domain model for the {@code Manifest}.
 * <p>
 * Please note that this is a request scoped bean. This mean that for each http request a
 * different instance will be initialized by Spring and used to serve this specific request.</p>
 * <p>
 *  The Manifest is an overall description of the structure and properties of the digital representation
 *  of an object. It carries information needed for the viewer to present the digitized content to the user,
 *  such as a title and other descriptive information about the object or the intellectual work that
 *  it conveys. Each manifest describes how to present a single object such as a book, a photograph,
 *  or a statue.</p>
 *
 * Please note that this is a request scoped bean. This means that for each http request a
 * different instance will be initialized by Spring and used to serve this specific request.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RequestScope
@Component
public class ManifestV3Generator implements IIIFResource {

    private String identifier;
    private String label;
    private PropertyValue description;
    private ImageContent logo;
    private ViewingHint viewingHint;
    private Sequence sequence;
    private List<OtherContent> seeAlsos = new ArrayList<>();
    private OtherContent related;
    private ImageContent thumbnail;
    private ContentSearchService searchService;
    private List<OtherContent> renderings = new ArrayList<>();
    private final List<URI> license = new ArrayList<>();
    private final List<MetadataEntry> metadata = new ArrayList<>();
    private final List<Range> ranges = new ArrayList<>();

    /**
     * Sets the mandatory manifest identifier.
     * @param identifier manifest identifier
     */
    public void setIdentifier(@NotNull String identifier) {

        if (identifier.isEmpty()) {
            throw new RuntimeException("Invalid manifest identifier. Cannot be an empty string.");
        }
        this.identifier = identifier;
    }

    /**
     * Sets the manifest label.
     * @param label manifest label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Adds an optional license to manifest.
     * @param license license terms
     */
    public void addLicense(String license) {
        this.license.add(URI.create(license));
    }

    @Override
    public Resource<Manifest> generateResource() {

        if (identifier == null) {
            throw new RuntimeException("The Manifest resource requires an identifier.");
        }
        Manifest manifest;
        if (label != null) {
            manifest = new Manifest(identifier, label);
        } else {
            manifest = new Manifest(identifier);
        }
        if (sequence != null) {
            manifest.addSequence(sequence);
        }
        if (metadata.size() > 0) {
            for (MetadataEntry meta : metadata) {
                manifest.addMetadata(meta);
            }
        }
        if (related != null) {
            manifest.addRelated(related);
        }
        if (searchService != null) {
            manifest.addService(searchService);
        }
        if (license.size() > 0) {
            manifest.setLicenses(license);
        }
        if (description != null) {
            manifest.setDescription(description);
        }
        if (viewingHint != null) {
            manifest.addViewingHint(viewingHint);
        }
        return manifest;
    }

}
