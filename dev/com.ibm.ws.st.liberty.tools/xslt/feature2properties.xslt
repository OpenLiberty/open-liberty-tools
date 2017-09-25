<?xml version="1.0"?>
<xsl:transform version="1.0" 
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:param name="buildLevel" />
    <xsl:output method="text"/>

    <xsl:template match="*"><xsl:apply-templates select="*|@*"/></xsl:template>
    <xsl:template match="text()"/>
    <xsl:template match="/feature/@version">
feature.version=<xsl:value-of select="."/>
    </xsl:template>
    <xsl:template match="@*"/>
</xsl:transform>