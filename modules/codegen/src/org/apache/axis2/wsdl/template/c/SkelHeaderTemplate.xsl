<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements. See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership. The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License. You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied. See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text"/>
    <xsl:template match="/interface">
    <xsl:variable name="svc_name"><xsl:value-of select="@name"/></xsl:variable>
    <xsl:variable name="method-prefix"><xsl:value-of select="@prefix"/></xsl:variable>
    <xsl:variable name="qname"><xsl:value-of select="@qname"/></xsl:variable>

    /**
     * <xsl:value-of select="@name"/>.h
     *
     * This file was auto-generated from WSDL for "<xsl:value-of select="$qname"/>" service
     * by the Apache Axis2/C version: #axisVersion# #today#
     * <xsl:value-of select="@name"/> Axis2/C skeleton for the axisService- Header file
     */


	#include &lt;axis2_svc_skeleton.h&gt;
	#include &lt;axutil_log_default.h&gt;
	#include &lt;axutil_error_default.h&gt;
	#include &lt;axiom_text.h&gt;
	#include &lt;axiom_node.h&gt;
	#include &lt;axiom_element.h&gt;
    #include &lt;stdio.h&gt;


   <xsl:for-each select="method">
    <xsl:for-each select="input/param[@type!='' and @ours]">
     <xsl:variable name="inputtype">adb_<xsl:value-of select="@type"></xsl:value-of></xsl:variable>
     #include "<xsl:value-of select="$inputtype"/>.h"
    </xsl:for-each>
    <xsl:for-each select="output/param[@type!='' and @ours]">
     <xsl:variable name="outputtype">adb_<xsl:value-of select="@type"></xsl:value-of></xsl:variable>
     #include "<xsl:value-of select="$outputtype"/>.h"
    </xsl:for-each>
   </xsl:for-each>

	#ifdef __cplusplus
	extern "C" {
	#endif

     <xsl:for-each select="method">
         <xsl:variable name="outputours"><xsl:value-of select="output/param/@ours"></xsl:value-of></xsl:variable>
         <xsl:variable name="isUnwrapParameters" select="input/param[@location='body' and @type!='']/@unwrappParameters"/>
         <xsl:variable name="outputtype">
            <xsl:choose>
                <xsl:when test="$isUnwrapParameters">
                    <xsl:choose>
                        <xsl:when test="output/param/param/@ours">
                            <xsl:text>adb_</xsl:text><xsl:value-of select="output/param/param/@type"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="output/param/param/@type"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:when test="output/param/@ours=''">axis2_status_t</xsl:when>
                <xsl:when test="output/param/@ours">adb_<xsl:value-of select="output/param/@type"></xsl:value-of>_t*</xsl:when>
                <xsl:otherwise><xsl:value-of select="output/param/@type"></xsl:value-of></xsl:otherwise>
            </xsl:choose>
         </xsl:variable>
         <xsl:variable name="count"><xsl:value-of select="count(output/param)"/></xsl:variable>

		 <!-- regardless of the sync or async status, the generated method signature would be just a usual
	           c function-->
        /**
         * auto generated function declaration
         * for "<xsl:value-of select="@qname"/>" operation.
         * @param env environment ( mandatory)<xsl:if test="not($isUnwrapParameters)"><xsl:for-each select="input/param[@type!='']"><xsl:text>
         </xsl:text>* @param _<xsl:value-of select="@name"/> of the <xsl:if test="@ours"> adb type adb_</xsl:if><xsl:value-of select="@type"/><xsl:if test="@ours">_t*</xsl:if></xsl:for-each></xsl:if>
         <xsl:if test="$isUnwrapParameters"><xsl:for-each select="input/param/param[@type!='']"><xsl:text>
         </xsl:text>* @param _<xsl:value-of select="@name"/> of the <xsl:if test="@ours"> adb type adb_</xsl:if><xsl:value-of select="@type"/><xsl:if test="@ours">_t*</xsl:if></xsl:for-each></xsl:if>
         *<xsl:for-each select="output/param[@location='soap_header']"><xsl:text>
         </xsl:text>* @param dp_<xsl:value-of select="@name"/> - output header</xsl:for-each>
         * @return <xsl:value-of select="$outputtype"/><xsl:text>
         */
        </xsl:text>

        <xsl:variable name="inputparams">
            <xsl:choose>
            <xsl:when test="$isUnwrapParameters">
                                          <xsl:for-each select="input/param/param[@type!='']">,
                                              <xsl:variable name="inputtype"><xsl:if test="@ours">adb_</xsl:if><xsl:value-of select="@type"/><xsl:if test="@ours">_t*</xsl:if></xsl:variable>
                                              <xsl:value-of select="$inputtype"/><xsl:text> _</xsl:text><xsl:value-of select="@name"/>
                                          </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
                                          <xsl:for-each select="input/param[@type!='']">,
                                              <xsl:variable name="inputtype"><xsl:if test="@ours">adb_</xsl:if><xsl:value-of select="@type"/><xsl:if test="@ours">_t*</xsl:if></xsl:variable>
                                              <xsl:value-of select="$inputtype"/><xsl:text> _</xsl:text><xsl:value-of select="@name"/>
                                          </xsl:for-each>
            </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:choose>
        <xsl:when test="$outputtype=''">axis2_status_t </xsl:when>
        <xsl:when test="$outputtype!=''"><xsl:value-of select="$outputtype"/></xsl:when>
        </xsl:choose>
        <xsl:text> </xsl:text>
        <xsl:value-of select="$method-prefix"/>_<xsl:value-of select="@name"/><xsl:text>(const axutil_env_t *env</xsl:text>
                                          <xsl:value-of select="$inputparams"/><xsl:for-each select="output/param[@location='soap_header']">,
                                            <xsl:variable name="outputtype"><xsl:if test="@ours">adb_</xsl:if><xsl:value-of select="@type"/><xsl:if test="@ours">_t**</xsl:if></xsl:variable>
                                            <xsl:value-of select="$outputtype"/><xsl:text> dp_</xsl:text><xsl:value-of select="@name"/><xsl:text> /* output header double ptr*/</xsl:text>
                                          </xsl:for-each> );
     </xsl:for-each>

	#ifdef __cplusplus
	}
	#endif
    </xsl:template>
 </xsl:stylesheet>
