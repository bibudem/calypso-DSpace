/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.iiif.v3.service.AbstractResourceService;
import org.dspace.app.iiif.model.generator.ManifestGenerator;
import org.dspace.app.iiif.service.utils.IIIFUtils;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.util.FrontendUrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This service creates the manifest. There should be a single instance of this service per request.
 * The {@code @RequestScope} provides a single instance created and available during complete lifecycle
 * of the HTTP request. This is needed because some configurations are cached in the
 * instance. Moreover, many injected dependencies are also request scoped or
 * prototype (that will turn in a request scope when injected in a request scope
 * bean). The generators for top-level domain objects need to be request scoped as they act as a builder
 * storing the object state during each incremental building step until the final object is returned (IIIF Resource).
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RequestScope
@Component
public class ManifestV3Service  extends AbstractResourceService{

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ManifestV3Service.class);

    @Autowired
    protected ItemService itemService;

    @Autowired
    IIIFUtils utils;

    @Autowired
    ManifestGenerator manifestGenerator;

    @Autowired
    MetadataExposureService metadataExposureService;

    @Autowired
    FrontendUrlService frontendUrlService;

    protected String[] METADATA_FIELDS;
    protected String RENDERING_BUNDLE_NAME;
    protected String RENDERING_ITEM_LABEL;

    /**
     * Estimate image dimension metadata.
     */
    boolean guessCanvasDimension;

    /**
     * Constructor.
     * @param configurationService the DSpace configuration service.
     */
    public ManifestV3Service(ConfigurationService configurationService) {
        setConfiguration(configurationService);
        METADATA_FIELDS = configurationService.getArrayProperty("iiif.metadata.item");
        RENDERING_BUNDLE_NAME = configurationService.getProperty("iiif.rendering.bundle");
        RENDERING_ITEM_LABEL = configurationService.getProperty("iiif.rendering.item");
    }

    /**
     * Returns JSON manifest response for a DSpace item.
     *
     * @param item the DSpace Item
     * @param context the DSpace context
     * @return manifest as JSON
     */
    public String getManifest(Item item, Context context) {
        // If default dimensions are provided via configuration do not guess the default dimension.
        String wid = configurationService.getProperty("iiif.canvas.default-width");
        String hgt = configurationService.getProperty("iiif.canvas.default-height");
        guessCanvasDimension = (wid == null && hgt == null);
        populateManifest(item, context);
        return utils.asJson(manifestGenerator.generateResource());
    }

    /**
     * Populates the manifest for a DSpace Item.
     *
     * @param item the DSpace Item
     * @param context the DSpace context
     * @return manifest domain object
     */
    private void populateManifest(Item item, Context context) {
        String manifestId = getManifestId(item.getID());
        manifestGenerator.setIdentifier(manifestId);
        manifestGenerator.setLabel(item.getName());
        addMetadata(context, item);
        addViewingHint(item);
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
                    manifestGenerator.addMetadata(field, values.get(0),
                            values.subList(1, values.size()).toArray(new String[values.size() - 1]));
                } else {
                    manifestGenerator.addMetadata(field, values.get(0));
                }
            }
        }
        String descrValue = item.getItemService().getMetadataFirstValue(item, "dc", "description", null, Item.ANY);
        if (StringUtils.isNotBlank(descrValue)) {
            manifestGenerator.addDescription(descrValue);
        }

        String licenseUriValue = item.getItemService().getMetadataFirstValue(item, "dc", "rights", "uri", Item.ANY);
        if (StringUtils.isNotBlank(licenseUriValue)) {
            manifestGenerator.addLicense(licenseUriValue);
        }
    }

    /**
     * Adds a viewing hint to the manifest. This is a hint to the client as to the most
     * appropriate method of displaying the resource.
     *
     * @param item the DSpace Item
     */
    private void addViewingHint(Item item) {
        manifestGenerator.addViewingHint(utils.getIIIFViewingHint(item, DOCUMENT_VIEWING_HINT));
    }


}
