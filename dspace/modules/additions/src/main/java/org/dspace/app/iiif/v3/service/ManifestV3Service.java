package org.dspace.app.iiif.v3.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.dspace.services.ConfigurationService;
import java.util.Collections;

import org.dspace.app.iiif.v3.model.generator.ManifestV3Generator;
import org.dspace.app.iiif.v3.model.generator.ImageContentGenerator;
import org.dspace.app.iiif.v3.model.generator.ProfileGenerator;
import org.dspace.app.iiif.v3.model.generator.ExternalLinksGenerator;
import org.dspace.app.iiif.v3.model.generator.CanvasGenerator;
import org.dspace.app.iiif.v3.model.generator.RangeGenerator;

import org.dspace.content.Item;
import org.dspace.content.Bundle;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;

import org.dspace.app.iiif.v3.service.utils.IIIFUtils;
import org.dspace.app.util.service.MetadataExposureService;

import info.freelibrary.iiif.presentation.v3.Manifest;
import info.freelibrary.iiif.presentation.v3.Resource;
import info.freelibrary.iiif.presentation.v3.services.ImageService3;
import info.freelibrary.iiif.presentation.v3.OtherContent;
import info.freelibrary.iiif.presentation.v3.properties.SeeAlso;
import info.freelibrary.iiif.presentation.v3.properties.Rendering;
import info.freelibrary.iiif.presentation.v3.properties.Label;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.dspace.util.FrontendUrlService;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Component
@RequestScope
public class ManifestV3Service extends AbstractResourceService {

    private static final Log log = LogFactory.getLog(ManifestV3Service.class);

    private List<OtherContent> otherContents = new ArrayList<>();

    @Autowired
    protected ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    MetadataExposureService metadataExposureService;

    @Autowired
    IIIFUtils utils;

    @Autowired
    private ManifestV3Generator manifestGenerator;

    @Autowired
    ImageContentService imageContentService;

    @Autowired
    SeeAlsoService seeAlsoService;

    @Autowired
    FrontendUrlService frontendUrlService;

    @Autowired
    RangeService rangeService;

    @Autowired
    CanvasItemsService canvasItemsService;

    @Autowired
    CanvasService canvasService;

    /**
     * Estimate image dimension metadata.
     */
    boolean guessCanvasDimension;



    protected String[] METADATA_FIELDS;
    protected String RENDERING_BUNDLE_NAME;
    protected String RENDERING_ITEM_LABEL;
    protected String DEFAULT_LANGUAGE;


    /**
    * Constructor.
    * @param configurationService the DSpace configuration service.
    */
    public ManifestV3Service(ConfigurationService configurationService) {
        setConfiguration(configurationService);
        METADATA_FIELDS = configurationService.getArrayProperty("iiif.metadata.item");
        RENDERING_BUNDLE_NAME = configurationService.getProperty("iiif.rendering.bundle");
        RENDERING_ITEM_LABEL = configurationService.getProperty("iiif.rendering.item");
        DEFAULT_LANGUAGE = configurationService.getProperty("default.language");
    }

     /**
     * Returns JSON manifest response for a DSpace item.
     *
     * @param item    the DSpace Item
     * @param context the DSpace context
     * @return manifest as JSON
     */
    public String getManifest(Item item, Context context) {
        try {

            String manifestId = getManifestId(item.getID());
            manifestGenerator.setID(manifestId);
            manifestGenerator.setLabel(DEFAULT_LANGUAGE,item.getName());

            populateManifest(item, context);

            // Generate the manifest resource
            Resource<Manifest> manifestResource = manifestGenerator.generateResource();

            //return utils.asJson(manifestResource);
            return manifestResource.toString();
        } catch (Exception e) {
            // Handle the exception as needed
            log.error("Error generating JSON for manifest", e);
            return null;
        }
    }

     /**
     * Populates the manifest for a DSpace Item.
     *
     * @param item the DSpace Item
     * @param context the DSpace context
     * @return manifest domain object
     */
    private void populateManifest(Item item, Context context ) {
        String manifestId = getManifestId(item.getID());
        addMetadata(context, item);
        addThumbnail(item, context);
        addSeeAlso(item, context);
        addRendering(item, context);
        addCanvasAndRange(context, item, manifestId);
    }

     /**
     * Add the ranges to the manifest structure. Ranges are generated from the
     * iiif.toc metadata
     *
     * @param context the DSpace Context
     * @param item the DSpace Item to represent
     * @param manifestId the generated manifestId
     */
    private void addCanvasAndRange(Context context, Item item, String manifestId) {

        // Set the root Range for this manifest.
        rangeService.setRootRange(manifestId);
        // Get bundles that contain manifest data.
        List<Bundle> bundles = utils.getIIIFBundles(item);
        // Set the default canvas dimensions.
        if (guessCanvasDimension) {
            canvasService.guessCanvasDimensions(context, bundles);
        }
        for (Bundle bnd : bundles) {
            String bundleToCPrefix = null;
            if (bundles.size() > 1) {
                // Check for bundle Range metadata if multiple IIIF bundles exist.
                bundleToCPrefix = utils.getBundleIIIFToC(bnd);
            }
            for (Bitstream bitstream : utils.getIIIFBitstreams(context, bnd)) {
                // Add the Canvas to the CanvasItemsService.
                CanvasGenerator canvas = canvasItemsService.addCanvas(context, item, bnd, bitstream, DEFAULT_LANGUAGE);
                // Update the Ranges.
                rangeService.updateRanges(bitstream, bundleToCPrefix, canvas);
            }
        }
        // If Ranges were created, add them to manifest.
        Map<String, RangeGenerator> tocRanges = rangeService.getTocRanges();
        if (tocRanges != null && tocRanges.size() > 0) {
            RangeGenerator rootRange = rangeService.getRootRange();
            manifestGenerator.addRange(rootRange);
            for (RangeGenerator range : tocRanges.values()) {
                manifestGenerator.addRange(range);
            }
        }
    }

    /**
     * Adds DSpace Item metadata to the manifest.
     *
     * @param context the DSpace Context
     * @param item the DSpace item
     */
    private void addMetadata(Context context, Item item) {

        for (String field : METADATA_FIELDS) {
            String[] eq = field.split("\\.");
            String schema = eq[0];
            String element = eq[1];
            String qualifier = null;
            if (eq.length > 2) {
                qualifier = eq[2];
            }
            List<MetadataValue> metadata = item.getItemService().getMetadata(item, schema, element, qualifier,
                    Item.ANY);
            List<String> values = new ArrayList<String>();
            for (MetadataValue meta : metadata) {
                // we need to perform the check here as the configuration can include jolly
                // characters (i.e. dc.description.*) and we need to be sure to hide qualified
                // metadata (dc.description.provenance)
                try {
                    if (metadataExposureService.isHidden(context, meta.getMetadataField().getMetadataSchema().getName(),
                            meta.getMetadataField().getElement(), meta.getMetadataField().getQualifier())) {
                        continue;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                values.add(meta.getValue());
            }
            if (values.size() > 0) {
                if (values.size() > 1) {
                    manifestGenerator.addMetadata(field, values.get(0));
                    for (int i = 1; i < values.size(); i++) {
                        manifestGenerator.addMetadata(field, values.get(i));
                    }
                } else {
                    manifestGenerator.addMetadata(field, values.get(0));
                }
            }

        }
        String descrValue = item.getItemService().getMetadataFirstValue(item, "dc", "description", null, Item.ANY);
        if (StringUtils.isNotBlank(descrValue)) {
            manifestGenerator.setSummary(descrValue);
        }

        String licenseUriValue = item.getItemService().getMetadataFirstValue(item, "dc", "rights", "uri", Item.ANY);
        if (StringUtils.isNotBlank(licenseUriValue)) {
            manifestGenerator.setRights(licenseUriValue);
        }
    }

    /**
    * Adds a thumbnail to the manifest. Uses the first image in the manifest.
    * @param item the DSpace Item
    * @param context DSpace context
    */
    private void addThumbnail(Item item, Context context) {
        List<Bitstream> bitstreams = utils.getIIIFBitstreams(context, item);
        if (bitstreams != null && bitstreams.size() > 0) {
            String mimeType = utils.getBitstreamMimeType(bitstreams.get(0), context);
            ImageContentGenerator image = imageContentService
                    .getImageContent(bitstreams.get(0).getID(), mimeType,
                            thumbUtil.getThumbnailProfile(), THUMBNAIL_PATH);
            manifestGenerator.addThumbnail(image);
        }
    }

       /**
        * This method adds into the manifest one or more {@code seeAlso} reference to additional
        * resources found in the Item bundle(s). A typical use case would be METS / ALTO files
        * that describe the resource.
        *
        * @param item the DSpace Item.
        * @param context The DSpace context
        */
        private void addSeeAlso(Item item, Context context) {
            // Use the SeeAlsoService to retrieve ExternalLinksGenerator resources.
            List<ExternalLinksGenerator> elgs = seeAlsoService.getSeeAlsos(item, context);

            if (!elgs.isEmpty()) {
                for (ExternalLinksGenerator elg : elgs) {
                    elg.setLangue(DEFAULT_LANGUAGE);
                    manifestGenerator.addSeeAlso(elg);
                }
            } else {
                log.warn("Aucune ressource trouvée à ajouter dans la section 'seeAlso' du manifeste.");
            }
        }
    /**
     * This method looks for a PDF in the Item's ORIGINAL bundle and adds
     * it as the Rendering resource if found.
     *
     * @param item    DSpace Item
     * @param context DSpace context
     */
    private void addRendering(Item item, Context context) {
        List<Bundle> bundles = utils.getIIIFBundles(item);
           if (item != null && context != null && bundles != null ) {
               for (Bundle bundle : bundles) {
                   List<Bitstream> bitstreams = bundle.getBitstreams();
                   for (Bitstream bitstream : bitstreams) {
                       String mimeType = null;
                       try {
                           mimeType = bitstream.getFormat(context).getMIMEType();
                       } catch (SQLException e) {
                           log.error("Error getting MIME type for bitstream: " + bitstream.getID(), e);
                       }
                       // If the bundle contains a PDF, assume that it represents the
                       // item and add to rendering. Ignore other mime-types.
                       if (mimeType != null && mimeType.equals("application/pdf")) {
                           String id = BITSTREAM_PATH_PREFIX + "/" + bitstream.getID() + "/content";
                           List<Rendering> renderings = generateRenderingList(id, mimeType, utils.getIIIFLabel(bitstream, bitstream.getName()));
                           for (Rendering rendering : renderings) {
                               manifestGenerator.addRendering(rendering);
                           }
                       }
                   }
               }
           }
    }
    /**
     * Generates a list of rendering resources based on the configured properties.
     *
     * @param id     The ID of the rendering resource
     * @param type   The MIME type of the rendering resource
     * @param label  The label of the rendering resource
     * @return A list of rendering resources
     */
    private List<Rendering> generateRenderingList(String id, String type, String label) {
        List<Rendering> renderingList = new ArrayList<>();
        // Create a new Rendering object and add it to the list
        renderingList.add(new Rendering(id, type, new Label(DEFAULT_LANGUAGE, label)));
        return renderingList;
    }


}
