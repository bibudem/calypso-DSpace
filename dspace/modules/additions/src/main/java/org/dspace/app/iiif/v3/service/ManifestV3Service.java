package org.dspace.app.iiif.v3.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.dspace.services.ConfigurationService;

import org.dspace.app.iiif.v3.model.generator.ManifestV3Generator;
import org.dspace.app.iiif.v3.model.generator.ImageContentGenerator;
import org.dspace.app.iiif.v3.model.generator.ProfileGenerator;
import org.dspace.content.Item;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;

import org.dspace.app.iiif.v3.service.utils.IIIFUtils;
import org.dspace.app.util.service.MetadataExposureService;

import info.freelibrary.iiif.presentation.v3.Manifest;
import info.freelibrary.iiif.presentation.v3.Resource;
import info.freelibrary.iiif.presentation.v3.services.ImageService3;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Component
@RequestScope
public class ManifestV3Service extends AbstractResourceService {

    private static final Log log = LogFactory.getLog(ManifestV3Service.class);

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
    private ProfileGenerator profileGenerator;

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

        addMetadata(context, item);
        addThumbnail(item, context);
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

}
