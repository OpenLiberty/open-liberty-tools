<?xml version="1.0"?>
<xsl:transform version="1.0" 
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:man="java.util.jar.Manifest"
               xmlns:fis="java.io.FileInputStream"
               xmlns:attr="java.util.jar.Attributes"
               xmlns:file="java.io.File">
    <xsl:param name="buildLevel" />
    <xsl:param name="basedir"/>
    <xsl:output method="xml" indent="yes"/>


     <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/feature/@version">
      <xsl:variable name="version" select="substring-before(string(), 'qualifier')"/>
      <xsl:attribute name="version"><xsl:value-of select="concat($version, $buildLevel)"/></xsl:attribute>
    </xsl:template>
    <xsl:template match="/feature/plugin/@version">
      <xsl:choose>
        <xsl:when test=". = '0.0.0'">
          <xsl:variable name="path" select="concat($basedir, '/../', ../@id, '/META-INF/MANIFEST.MF')"/>
          <xsl:variable name="theFile" select="file:new($path)"/>
          <xsl:choose>
            <xsl:when test="file:exists($theFile)">
            <xsl:variable name="in" select="fis:new($path)"/>
            <xsl:variable name="man" select="man:new($in)"/>
            <xsl:variable name="attributes" select="man:getMainAttributes($man)"/>
            <xsl:variable name="rawVersion" select="attr:getValue($attributes, 'Bundle-Version')"/>
            <xsl:variable name="version" select="substring-before($rawVersion, 'qualifier')"/>
            <xsl:attribute name="version"><xsl:value-of select="concat($version, $buildLevel)"/></xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
              <xsl:attribute name="version"><xsl:value-of select="."/></xsl:attribute>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="version"><xsl:value-of select="."/></xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:template>
</xsl:transform>