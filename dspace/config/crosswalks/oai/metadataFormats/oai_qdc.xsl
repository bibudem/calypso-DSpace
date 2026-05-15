<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:doc="http://www.lyncode.com/xoai"
    version="1.0">

    <xsl:output omit-xml-declaration="yes" method="xml" indent="yes"/>

    <xsl:template match="/">
        <oai_qdc:qualifieddc
            xmlns:oai_qdc="http://worldcat.org/xmlschemas/qdc-1.0/"
            xmlns:dc="http://purl.org/dc/elements/1.1/"
            xmlns:dcterms="http://purl.org/dc/terms/"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://worldcat.org/xmlschemas/qdc-1.0/
                http://worldcat.org/xmlschemas/qdc/1.0/qdc.xsd
                http://purl.org/dc/terms/
                http://dublincore.org/schemas/xmls/qdc/dcterms.xsd">

            <!-- dc.* fields (except description, handled separately below) -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name!='description']/doc:element/doc:field[@name='value']">
                <xsl:element name="dc:{../../@name}">
                    <xsl:value-of select="."/>
                </xsl:element>
            </xsl:for-each>

            <!-- dc.description — strip IIIF label prefixes before OAI-PMH output -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element/doc:field[@name='value']">
                <dc:description>
                    <xsl:variable name="raw" select="normalize-space(.)"/>
                    <xsl:choose>
                        <xsl:when test="starts-with($raw, 'Édition : ')">
                            <xsl:value-of select="substring-after($raw, 'Édition : ')"/>
                        </xsl:when>
                        <xsl:when test="starts-with($raw, 'Description mat&#xE9;rielle : ')">
                            <xsl:value-of select="substring-after($raw, 'Description mat&#xE9;rielle : ')"/>
                        </xsl:when>
                        <xsl:when test="starts-with($raw, 'Collection : ')">
                            <xsl:value-of select="substring-after($raw, 'Collection : ')"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$raw"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </dc:description>
            </xsl:for-each>

            <!-- dc.description.* (not provenance) -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name!='provenance']/doc:element/doc:field[@name='value']">
                <dc:description>
                    <xsl:variable name="raw" select="normalize-space(.)"/>
                    <xsl:choose>
                        <xsl:when test="starts-with($raw, 'Édition : ')">
                            <xsl:value-of select="substring-after($raw, 'Édition : ')"/>
                        </xsl:when>
                        <xsl:when test="starts-with($raw, 'Description_materielle : ')">
                            <xsl:value-of select="substring-after($raw, 'Description_materielle : ')"/>
                        </xsl:when>
                        <xsl:when test="starts-with($raw, 'Collection : ')">
                            <xsl:value-of select="substring-after($raw, 'Collection : ')"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$raw"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </dc:description>
            </xsl:for-each>

            <!-- dcterms.* fields -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dcterms']/doc:element/doc:element/doc:field[@name='value']">
                <xsl:element name="dcterms:{../../@name}">
                    <xsl:value-of select="."/>
                </xsl:element>
            </xsl:for-each>

        </oai_qdc:qualifieddc>
    </xsl:template>

</xsl:stylesheet>