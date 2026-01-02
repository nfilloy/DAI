<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:tns="http://www.esei.uvigo.es/dai/proyecto" version="1.0">

	<xsl:output method="html" indent="yes" encoding="utf-8" />

	<xsl:template match="/">
		<xsl:text disable-output-escaping="yes">&lt;!DOCTYPE html&gt;</xsl:text>
		<html>
			<head>
				<title>Collection</title>
			</head>
			<body>
				<div>
					<h1>Collection</h1>
					<h2>Discs 1</h2>
					<xsl:apply-templates select="tns:collection/disc1" />
					<hr/>
					<h2>Movies 1</h2>
					<xsl:apply-templates select="tns:collection/movie1" />
					<hr/>
					<h2>Books 1</h2>
					<xsl:apply-templates select="tns:collection/book1" />
				</div>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="disc1">
		<div class="disc1">
			<h3><xsl:value-of select="name"/>(<xsl:value-of select="@year"/>)</h3>
			<div class="artist">Artist: <xsl:value-of select="artist" /></div>
			<div class="genre">Genre: <xsl:value-of select="genre" /></div>
		</div>
	</xsl:template>

	<xsl:template match="movie1">
		<div class="movie1">
			<h3><xsl:value-of select="name"/>(<xsl:value-of select="@year"/>)</h3>
			<div class="director">Director: <xsl:value-of select="director" /></div>
			<div class="genre">Genre: <xsl:value-of select="genre" /></div>
		</div>
	</xsl:template>

	<xsl:template match="book1">
		<div class="book1">
			<h3><xsl:value-of select="name"/>(<xsl:value-of select="@year"/>)</h3>
			<div class="author">Author: <xsl:value-of select="author" /></div>
			<div class="genre">Genre: <xsl:value-of select="genre" /></div>
			<div class="pages">Pages: <xsl:value-of select="pages" /></div>
		</div>
	</xsl:template>
</xsl:stylesheet>