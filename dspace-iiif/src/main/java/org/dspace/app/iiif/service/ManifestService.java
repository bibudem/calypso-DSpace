/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.iiif.model.generator.CanvasGenerator;
import org.dspace.app.iiif.model.generator.ContentSearchGenerator;
import org.dspace.app.iiif.model.generator.ExternalLinksGenerator;
import org.dspace.app.iiif.model.generator.ImageContentGenerator;
import org.dspace.app.iiif.model.generator.ManifestGenerator;
import org.dspace.app.iiif.model.generator.RangeGenerator;
import org.dspace.app.iiif.service.utils.IIIFUtils;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
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
public class ManifestService extends AbstractResourceService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ManifestService.class);

    /** NIMA 2026-05-14
     * Known dc.description label prefixes embedded by the CSV generation script.
     * Each value is separated from its label by " : " (space-colon-space).
     * Labels that appear multiple times (e.g. "Description") are intentionally
     * kept as-is — Mirador will display each value as a separate row under that label.
     */
    private static final String[] DESCRIPTION_LABEL_PREFIXES = {
        "Édition",
        "Description matérielle",
        "Collection"
    };
	
	private static final Map<String, String> LANGUAGE_MAP = Map.ofEntries(
		Map.entry("fre", "Français"),
		Map.entry("fra", "Français"),
		Map.entry("frm", "Français moyen"),
		Map.entry("fro", "Ancien français"),
		Map.entry("eng", "Anglais"),
		Map.entry("enm", "Moyen anglais"),
		Map.entry("ger", "Allemand"),
		Map.entry("deu", "Allemand"),
		Map.entry("spa", "Espagnol"),
		Map.entry("ita", "Italien"),
		Map.entry("lat", "Latin"),
		Map.entry("por", "Portugais"),
		Map.entry("dut", "Néerlandais"),
		Map.entry("rus", "Russe"),
		Map.entry("ara", "Arabe"),
		Map.entry("zho", "Chinois"),
		Map.entry("chi", "Chinois"),
		Map.entry("jpn", "Japonais"),
		Map.entry("gre", "Grec moderne"),
		Map.entry("grc", "Grec ancien"),
		Map.entry("heb", "Hébreu"),
		Map.entry("hin", "Hindi"),
		Map.entry("alg", "Langues algonquiennes"),
		Map.entry("ath", "Langues athapascanes"),
		Map.entry("bla", "Pied-noir"),
		Map.entry("chp", "Chipewyan"),
		Map.entry("chr", "Cherokee"),
		Map.entry("chy", "Cheyenne"),
		Map.entry("cho", "Choctaw"),
		Map.entry("cre", "Cri"),
		Map.entry("dak", "Dakota"),
		Map.entry("del", "Delaware"),
		Map.entry("den", "Esclave"),
		Map.entry("git", "Gitksan"),
		Map.entry("hai", "Haïda"),
		Map.entry("hur", "Halkomelem"),
		Map.entry("iku", "Inuktitut"),
		Map.entry("ipk", "Inupiaq"),
		Map.entry("iro", "Langues iroquoises"),
		Map.entry("kwk", "Kwakwaka’wakw"),
		Map.entry("mic", "Micmac"),
		Map.entry("moh", "Mohawk"),
		Map.entry("mus", "Muscogee"),
		Map.entry("nav", "Navajo"),
		Map.entry("nis", "Nez-percé"),
		Map.entry("oji", "Ojibwé"),
		Map.entry("sal", "Langues salish"),
		Map.entry("sec", "Sechelt"),
		Map.entry("squ", "Squamish"),
		Map.entry("str", "Salish des détroits"),
		Map.entry("tli", "Tlingit"),
		Map.entry("yup", "Langues yupik"),		
		Map.entry("mul", "Multilingue"),
		Map.entry("zxx", "Aucun contenu linguistique"),
		Map.entry("mis", "Langue non codée"),
		Map.entry("und", "Indéterminée")		
	);

    @Autowired
    protected ItemService itemService;

    @Autowired
    CanvasService canvasService;

    @Autowired
    RangeService rangeService;

    @Autowired
    SequenceService sequenceService;

    @Autowired
    RelatedService relatedService;

    @Autowired
    SeeAlsoService seeAlsoService;

    @Autowired
    ImageContentService imageContentService;

    @Autowired
    IIIFUtils utils;

    @Autowired
    ContentSearchGenerator contentSearchGenerator;

    @Autowired
    ManifestGenerator manifestGenerator;

    @Autowired
    MetadataExposureService metadataExposureService;

    protected String[] METADATA_FIELDS;

    /**
     * Estimate image dimension metadata.
     */
    boolean guessCanvasDimension;

    /**
     * Constructor.
     * @param configurationService the DSpace configuration service.
     */
    public ManifestService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
        METADATA_FIELDS = configurationService.getArrayProperty("iiif.metadata.item");
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
     */
    private void populateManifest(Item item, Context context) {
        String manifestId = getManifestId(item.getID());
        manifestGenerator.setIdentifier(manifestId);
        //manifestGenerator.setLabel(item.getName());
		// NIMA - 2026-05-14
		// Use dcterms.title as manifest label if available, fall back to dc.title
		String dctermsTitle = item.getItemService()
				.getMetadataFirstValue(item, "dcterms", "title", null, Item.ANY);
		manifestGenerator.setLabel(
				StringUtils.isNotBlank(dctermsTitle) ? dctermsTitle : item.getName()
		);
        setLogoContainer();
        addRelated(item);
        addSearchService(item);
        addMetadata(context, item);
        addViewingHint(item);
        addThumbnail(item, context);
        addCanvasAndRange(context, item, manifestId);
        manifestGenerator.addSequence(
                sequenceService.getSequence(item));
        addRendering(item, context);
        addSeeAlso(item);
    }

    /**
     * Add the ranges to the manifest structure. Ranges are generated from the
     * iiif.toc metadata.
     *
     * @param context the DSpace Context
     * @param item the DSpace Item to represent
     * @param manifestId the generated manifestId
     */
    private void addCanvasAndRange(Context context, Item item, String manifestId) {
        rangeService.setRootRange(manifestId);
        List<Bundle> bundles = utils.getIIIFBundles(item);
        if (guessCanvasDimension) {
            canvasService.guessCanvasDimensions(context, bundles);
        }
        for (Bundle bnd : bundles) {
            String bundleToCPrefix = null;
            if (bundles.size() > 1) {
                bundleToCPrefix = utils.getBundleIIIFToC(bnd);
            }
            for (Bitstream bitstream : utils.getIIIFBitstreams(context, bnd)) {
                CanvasGenerator canvas = sequenceService.addCanvas(context, item, bnd, bitstream);
                rangeService.updateRanges(bitstream, bundleToCPrefix, canvas);
            }
        }
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
     * For dc.description values, the CSV generation script embeds a human-readable
     * label prefix separated by " : " (e.g. "Collection : Titre de la série, vol. 3").
     * This method detects those prefixes and emits each value as a separate manifest
     * metadata entry with its own label, producing distinct labelled rows in Mirador.
     *
     * Known prefixes (from DESCRIPTION_LABEL_PREFIXES):
     *   "Édition"              ← 250 $a
     *   "Description matérielle" ← 300 $a $b $c
     *   "Collection"           ← 490 $a $v
     *   "Description"          ← 500 $a, 546 $a, 590 $a, 591 $a $c
     *
     * Values without a recognised prefix are emitted normally under the field name.
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
            List<MetadataValue> metadata = item.getItemService()
                    .getMetadata(item, schema, element, qualifier, Item.ANY);

            // Accumulate non-description values normally; split description values individually.
            List<String> regularValues = new ArrayList<String>();

            for (MetadataValue meta : metadata) {
                try {
                    if (metadataExposureService.isHidden(context,
                            meta.getMetadataField().getMetadataSchema().getName(),
                            meta.getMetadataField().getElement(),
                            meta.getMetadataField().getQualifier())) {
                        continue;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
				
				// ---- TRANSLATE LANGUAGE CODES ---- //
				if ("dcterms".equals(schema) && "language".equals(element) && qualifier == null) {
					for (MetadataValue langMeta : metadata) {
						try {
							if (metadataExposureService.isHidden(context,
									langMeta.getMetadataField().getMetadataSchema().getName(),
									langMeta.getMetadataField().getElement(),
									langMeta.getMetadataField().getQualifier())) {
								continue;
							}
						} catch (SQLException e) {
							throw new RuntimeException(e);
						}
						String code = langMeta.getValue().trim();
						String humanLabel = LANGUAGE_MAP.getOrDefault(code, code);
						manifestGenerator.addMetadata(field, humanLabel);
					}
					continue; // skip regular processing for this field
				}
				// ---- END LANGUAGE TRANSLATION ---- //

                // ---- SPLIT LABELLED dc.description VALUES ---- //
                if ("dc".equals(schema) && "description".equals(element) && qualifier == null) {
                    String raw = meta.getValue();
                    String splitLabel = extractDescriptionLabel(raw);
                    if (splitLabel != null) {
                        // Emit immediately as its own manifest metadata entry.
                        String splitValue = raw.substring(splitLabel.length() + 3).trim(); // skip " : "
                        if (!splitValue.isEmpty()) {
                            manifestGenerator.addMetadata(splitLabel, splitValue);
                            continue; // do not add to regularValues
                        }
                    }
                }
                // ---- END SPLIT ---- //

                regularValues.add(meta.getValue());
            }

            // Emit remaining (non-description or unrecognised-prefix) values normally.
            if (regularValues.size() > 0) {
                if (regularValues.size() > 1) {
                    manifestGenerator.addMetadata(field, regularValues.get(0),
                            regularValues.subList(1, regularValues.size())
                                         .toArray(new String[regularValues.size() - 1]));
                } else {
                    manifestGenerator.addMetadata(field, regularValues.get(0));
                }
            }
        }

        // Add IIIF manifest-level description (uses first dc.description value).
        String descrValue = item.getItemService()
                .getMetadataFirstValue(item, "dc", "description", null, Item.ANY);
        if (StringUtils.isNotBlank(descrValue)) {
            // Strip label prefix for the manifest-level description if present.
            String splitLabel = extractDescriptionLabel(descrValue);
            if (splitLabel != null) {
                String stripped = descrValue.substring(splitLabel.length() + 3).trim();
                manifestGenerator.addDescription(stripped);
            } else {
                manifestGenerator.addDescription(descrValue);
            }
        }

        String licenseUriValue = item.getItemService()
                .getMetadataFirstValue(item, "dc", "rights", "uri", Item.ANY);
        if (StringUtils.isNotBlank(licenseUriValue)) {
            manifestGenerator.addLicense(licenseUriValue);
        }
    }

    /**
     * Checks whether a dc.description value begins with one of the known label
     * prefixes followed by " : ".
     *
     * @param value the raw metadata value
     * @return the matched label string, or null if no known prefix is found
     */
    private String extractDescriptionLabel(String value) {
        if (value == null) {
            return null;
        }
        for (String prefix : DESCRIPTION_LABEL_PREFIXES) {
            if (value.startsWith(prefix + " : ")) {
                return prefix;
            }
        }
        return null;
    }

    /**
     * Adds a related item property to the manifest.
     *
     * @param item the DSpace Item
     */
    private void addRelated(Item item) {
        manifestGenerator.addRelated(relatedService.getRelated(item));
    }

    /**
     * Adds a viewing hint to the manifest.
     *
     * @param item the DSpace Item
     */
    private void addViewingHint(Item item) {
        manifestGenerator.addViewingHint(utils.getIIIFViewingHint(item, DOCUMENT_VIEWING_HINT));
    }

    /**
     * Adds seeAlso references to the manifest.
     *
     * @param item the DSpace Item.
     */
    private void addSeeAlso(Item item) {
        // 1. Default self-referencing AnnotationList — disabled, returns resources:[]
        // Uncomment when a real machine-readable aggregation endpoint exists
        // manifestGenerator.addSeeAlso(seeAlsoService.getSeeAlso(item));

        // 2. dc.source.uri — human-readable link (text/html)
        for (ExternalLinksGenerator link : seeAlsoService.getSourceUriLinks(item)) {
            manifestGenerator.addSeeAlso(link);
        }

        // 3. MARC XML via OAI-PMH GetRecord
        ExternalLinksGenerator marcLink = seeAlsoService.getMarcOaiSeeAlso(item);
        if (marcLink != null) {
            manifestGenerator.addSeeAlso(marcLink);
        }
    }

    /**
     * Adds a search service definition to the manifest when
     * the item metadata includes {@code iiif.search.enabled}.
     *
     * @param item the DSpace Item
     */
    private void addSearchService(Item item) {
        if (utils.isSearchable(item)) {
            contentSearchGenerator.setIdentifier(IIIF_ENDPOINT + item.getID() + "/manifest/search");
            manifestGenerator.addService(contentSearchGenerator);
        }
    }

    /**
     * Adds thumbnail to the manifest using the first image in the manifest.
     *
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
     * Adds the logo to the manifest when defined in DSpace configuration.
     */
    private void setLogoContainer() {
        if (IIIF_LOGO_IMAGE != null) {
            ImageContentGenerator logo = new ImageContentGenerator(IIIF_LOGO_IMAGE);
            manifestGenerator.addLogo(logo);
        }
    }

    /**
     * Looks for a PDF in the Item's ORIGINAL bundle and adds it as the
     * Rendering resource if found.
     *
     * @param item DSpace Item
     * @param context DSpace context
     */
    private void addRendering(Item item, Context context) {
        List<Bundle> bundles = utils.getIIIFBundles(item);
        for (Bundle bundle : bundles) {
            List<Bitstream> bitstreams = bundle.getBitstreams();
            for (Bitstream bitstream : bitstreams) {
                String mimeType = null;
                try {
                    mimeType = bitstream.getFormat(context).getMIMEType();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                if (mimeType != null && mimeType.contentEquals("application/pdf")) {
                    String id = BITSTREAM_PATH_PREFIX + "/" + bitstream.getID() + "/content";
                    manifestGenerator.addRendering(
                        new ExternalLinksGenerator(id)
                            .setLabel(utils.getIIIFLabel(bitstream, bitstream.getName()))
                            .setFormat(mimeType)
                    );
                }
            }
        }
    }
}