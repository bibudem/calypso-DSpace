package org.dspace.app.iiif.v3.service.utils;

import java.util.List;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;

/**
 * Interface to expose additional information at the canvas level
 * for v3 bitstreams.
 */
public interface BitstreamIIIFVirtualMetadata {

    // v3-specific bean prefix to avoid collisions with v2
    String IIIFV3_BITSTREAM_VIRTUAL_METADATA_BEAN_PREFIX = "iiifv3.bitstream.";

    /**
     * Return virtual metadata values for a bitstream in a given context
     *
     * @param context the DSpace context
     * @param bitstream the bitstream
     * @return list of virtual metadata values
     */
    List<String> getValues(Context context, Bitstream bitstream);
}
