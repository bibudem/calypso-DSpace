/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.iiif.model.generator.AnnotationGenerator;
import org.dspace.app.iiif.model.generator.ExternalLinksGenerator;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Service for building seeAlso links in the IIIF manifest.
 *
 * Provides:
 *   1. The default self-referencing AnnotationList endpoint
 *   2. dc.source.uri values as human-readable related links (text/html)
 *   3. OAI-PMH GetRecord link for MARC XML (machine-readable, marcxml)
 *
 * @author Custom overlay — UdeM
 */
@RequestScope
@Component
public class SeeAlsoService extends AbstractResourceService {

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected HandleService handleService;

    public SeeAlsoService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    /**
     * Default self-referencing seeAlso AnnotationList endpoint.
     */
    public ExternalLinksGenerator getSeeAlso(Item item) {
        return new ExternalLinksGenerator(IIIF_ENDPOINT + item.getID() + "/manifest/seeAlso")
            .setType(AnnotationGenerator.TYPE)
            .setLabel("More descriptions of this resource");
    }

    /**
     * Returns one seeAlso entry per dc.source.uri value (human-readable, text/html).
     */
    public List<ExternalLinksGenerator> getSourceUriLinks(Item item) {
        List<MetadataValue> values = itemService.getMetadata(
            item, "dc", "source", "uri", Item.ANY);

        List<ExternalLinksGenerator> links = new ArrayList<>();
        for (MetadataValue mv : values) {
            String uri = mv.getValue();
            if (StringUtils.isNotBlank(uri) && uri.startsWith("http")) {
                links.add(
                    new ExternalLinksGenerator(uri)
                        .setType("dctypes:Text")
                        .setFormat("text/html")
                        .setLabel("Lien Sofia")
                );
            }
        }
        return links;
    }

    /**
	 * Returns an OAI-PMH GetRecord seeAlso link for the Dublin Core record.
	 *
	 * OAI identifier format: oai:{oai.identifier.prefix}:{handle}
	 * e.g. oai:collections-speciales.bib.umontreal.ca:123456789/42
	 *
	 * URL format:
	 * https://{dspace.server.url}/oai/request?verb=GetRecord
	 *   &metadataPrefix=oai_qdc
	 *   &identifier=oai:{prefix}:{handle}
	 *
	 * Returns null if the item has no handle.
	 */
	public ExternalLinksGenerator getMarcOaiSeeAlso(Item item) {
		String handle = item.getHandle();
		if (StringUtils.isBlank(handle)) {
			return null;
		}

		String oaiPrefix = configurationService.getProperty(
			"oai.identifier.prefix",
			configurationService.getProperty("dspace.ui.url", "")
				.replaceAll("https?://", "")
				.replaceAll("/.*", "")
		);

		String serverUrl = configurationService.getProperty("dspace.server.url");

		String identifier = "oai:" + oaiPrefix + ":" + handle;
		String oaiUrl = serverUrl + "/oai/request"
			+ "?verb=GetRecord"
			+ "&metadataPrefix=oai_qdc"
			+ "&identifier=" + identifier;

		return new ExternalLinksGenerator(oaiUrl)
			.setType("dataset")
			.setFormat("application/xml")
			.setLabel("Format Dublin Core (OAI-PMH)");
	}
}