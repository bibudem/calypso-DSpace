/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.v3.service.utils;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * Expose the Bitstream format mime type as a IIIF V3 Metadata
 * 
 * @author
 *
 */
@Component(BitstreamIIIFVirtualMetadata.IIIFV3_BITSTREAM_VIRTUAL_METADATA_BEAN_PREFIX + "mimetype")
public class BitstreamMimetypeIIIFVirtualMetadata implements BitstreamIIIFVirtualMetadata {

    @Override
    public List<String> getValues(Context context, Bitstream bitstream) {
        try {
            return Collections.singletonList(bitstream.getFormat(context).getMIMEType());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
