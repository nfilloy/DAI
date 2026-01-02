<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:tns="http://www.esei.uvigo.es/dai/proyecto" version="1.0">

	<xsl:output method="html" indent="yes" encoding="utf-8" />

	<xsl:template match="/">
		<xsl:text disable-output-escaping="yes">&lt;!DOCTYPE html&gt;</xsl:text>
		<html>
			<head>
				<title>People</title>
			</head>
			<body>
				<div>
					<h1>People</h1>
					<xsl:apply-templates select="tns:people/tns:person" />
				</div>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="tns:person">
		<div class="person">
			<h3>DNI: <xsl:value-of select="@dni"/></h3>
			<div class="name"><xsl:value-of select="tns:name" /></div>
		</div>
	</xsl:template>
</xsl:stylesheet>