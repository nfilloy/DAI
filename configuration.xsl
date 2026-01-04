<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:tns="http://www.esei.uvigo.es/dai/hybridserver">

    <xsl:output method="html" indent="yes" encoding="UTF-8"/>

    <xsl:template match="/">
        <xsl:text disable-output-escaping="yes">&lt;!DOCTYPE html&gt;</xsl:text>
        <html>
            <head>
                <title>Hybrid Server Configuration</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    h1 { color: #333; }
                    h2 { color: #666; border-bottom: 1px solid #ccc; padding-bottom: 5px; }
                    table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }
                    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                    th { background-color: #4CAF50; color: white; }
                    tr:nth-child(even) { background-color: #f2f2f2; }
                </style>
            </head>
            <body>
                <h1>Hybrid Server Configuration</h1>
                <xsl:apply-templates select="tns:configuration"/>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="tns:configuration">
        <h2>Connections</h2>
        <table>
            <tr>
                <th>Parameter</th>
                <th>Value</th>
            </tr>
            <tr>
                <td>HTTP Port</td>
                <td><xsl:value-of select="tns:connections/tns:http"/></td>
            </tr>
            <tr>
                <td>Web Service URL</td>
                <td><xsl:value-of select="tns:connections/tns:webservice"/></td>
            </tr>
            <tr>
                <td>Number of Clients</td>
                <td><xsl:value-of select="tns:connections/tns:numClients"/></td>
            </tr>
        </table>

        <h2>Database</h2>
        <table>
            <tr>
                <th>Parameter</th>
                <th>Value</th>
            </tr>
            <tr>
                <td>User</td>
                <td><xsl:value-of select="tns:database/tns:user"/></td>
            </tr>
            <tr>
                <td>Password</td>
                <td><xsl:value-of select="tns:database/tns:password"/></td>
            </tr>
            <tr>
                <td>URL</td>
                <td><xsl:value-of select="tns:database/tns:url"/></td>
            </tr>
        </table>

        <h2>Servers</h2>
        <xsl:choose>
            <xsl:when test="tns:servers/tns:server">
                <table>
                    <tr>
                        <th>Name</th>
                        <th>WSDL</th>
                        <th>Namespace</th>
                        <th>Service</th>
                        <th>HTTP Address</th>
                    </tr>
                    <xsl:for-each select="tns:servers/tns:server">
                        <tr>
                            <td><xsl:value-of select="@name"/></td>
                            <td><xsl:value-of select="@wsdl"/></td>
                            <td><xsl:value-of select="@namespace"/></td>
                            <td><xsl:value-of select="@service"/></td>
                            <td><xsl:value-of select="@httpAddress"/></td>
                        </tr>
                    </xsl:for-each>
                </table>
            </xsl:when>
            <xsl:otherwise>
                <p>No servers configured.</p>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
