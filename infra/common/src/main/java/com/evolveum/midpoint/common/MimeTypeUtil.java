/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import static java.util.Map.entry;

import java.util.Map;

/**
 * @author skublik
 */
public class MimeTypeUtil {

    public static final String MIME_APPLICATION_ANDREW_INSET = "application/andrew-inset";
    public static final String MIME_APPLICATION_JSON = "application/json";
    public static final String MIME_APPLICATION_ZIP = "application/zip";
    public static final String MIME_APPLICATION_X_GZIP = "application/x-gzip";
    public static final String MIME_APPLICATION_TGZ = "application/tgz";
    public static final String MIME_APPLICATION_MSWORD = "application/msword";
    public static final String MIME_APPLICATION_MSWORD_2007 = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String MIME_APPLICATION_VND_TEXT = "application/vnd.oasis.opendocument.text";
    public static final String MIME_APPLICATION_POSTSCRIPT = "application/postscript";
    public static final String MIME_APPLICATION_PDF = "application/pdf";
    public static final String MIME_APPLICATION_JNLP = "application/jnlp";
    public static final String MIME_APPLICATION_MAC_BINHEX40 = "application/mac-binhex40";
    public static final String MIME_APPLICATION_MAC_COMPACTPRO = "application/mac-compactpro";
    public static final String MIME_APPLICATION_MATHML_XML = "application/mathml+xml";
    public static final String MIME_APPLICATION_OCTET_STREAM = "application/octet-stream";
    public static final String MIME_APPLICATION_ODA = "application/oda";
    public static final String MIME_APPLICATION_RDF_XML = "application/rdf+xml";
    public static final String MIME_APPLICATION_JAVA_ARCHIVE = "application/java-archive";
    public static final String MIME_APPLICATION_RDF_SMIL = "application/smil";
    public static final String MIME_APPLICATION_SRGS = "application/srgs";
    public static final String MIME_APPLICATION_SRGS_XML = "application/srgs+xml";
    public static final String MIME_APPLICATION_VND_MIF = "application/vnd.mif";
    public static final String MIME_APPLICATION_VND_MSEXCEL = "application/vnd.ms-excel";
    public static final String MIME_APPLICATION_VND_MSEXCEL_2007 = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String MIME_APPLICATION_VND_SPREADSHEET = "application/vnd.oasis.opendocument.spreadsheet";
    public static final String MIME_APPLICATION_VND_MSPOWERPOINT = "application/vnd.ms-powerpoint";
    public static final String MIME_APPLICATION_VND_RNREALMEDIA = "application/vnd.rn-realmedia";
    public static final String MIME_APPLICATION_X_BCPIO = "application/x-bcpio";
    public static final String MIME_APPLICATION_X_CDLINK = "application/x-cdlink";
    public static final String MIME_APPLICATION_X_CHESS_PGN = "application/x-chess-pgn";
    public static final String MIME_APPLICATION_X_CPIO = "application/x-cpio";
    public static final String MIME_APPLICATION_X_CSH = "application/x-csh";
    public static final String MIME_APPLICATION_X_DIRECTOR = "application/x-director";
    public static final String MIME_APPLICATION_X_DVI = "application/x-dvi";
    public static final String MIME_APPLICATION_X_FUTURESPLASH = "application/x-futuresplash";
    public static final String MIME_APPLICATION_X_GTAR = "application/x-gtar";
    public static final String MIME_APPLICATION_X_HDF = "application/x-hdf";
    public static final String MIME_APPLICATION_X_JAVASCRIPT = "application/x-javascript";
    public static final String MIME_APPLICATION_X_KOAN = "application/x-koan";
    public static final String MIME_APPLICATION_X_LATEX = "application/x-latex";
    public static final String MIME_APPLICATION_X_NETCDF = "application/x-netcdf";
    public static final String MIME_APPLICATION_X_OGG = "application/x-ogg";
    public static final String MIME_APPLICATION_X_SH = "application/x-sh";
    public static final String MIME_APPLICATION_X_SHAR = "application/x-shar";
    public static final String MIME_APPLICATION_X_SHOCKWAVE_FLASH = "application/x-shockwave-flash";
    public static final String MIME_APPLICATION_X_STUFFIT = "application/x-stuffit";
    public static final String MIME_APPLICATION_X_SV4CPIO = "application/x-sv4cpio";
    public static final String MIME_APPLICATION_X_SV4CRC = "application/x-sv4crc";
    public static final String MIME_APPLICATION_X_TAR = "application/x-tar";
    public static final String MIME_APPLICATION_X_RAR_COMPRESSED = "application/x-rar-compressed";
    public static final String MIME_APPLICATION_X_TCL = "application/x-tcl";
    public static final String MIME_APPLICATION_X_TEX = "application/x-tex";
    public static final String MIME_APPLICATION_X_TEXINFO = "application/x-texinfo";
    public static final String MIME_APPLICATION_X_TROFF = "application/x-troff";
    public static final String MIME_APPLICATION_X_TROFF_MAN = "application/x-troff-man";
    public static final String MIME_APPLICATION_X_TROFF_ME = "application/x-troff-me";
    public static final String MIME_APPLICATION_X_TROFF_MS = "application/x-troff-ms";
    public static final String MIME_APPLICATION_X_USTAR = "application/x-ustar";
    public static final String MIME_APPLICATION_X_WAIS_SOURCE = "application/x-wais-source";
    public static final String MIME_APPLICATION_VND_MOZZILLA_XUL_XML = "application/vnd.mozilla.xul+xml";
    public static final String MIME_APPLICATION_XHTML_XML = "application/xhtml+xml";
    public static final String MIME_APPLICATION_XSLT_XML = "application/xslt+xml";
    public static final String MIME_APPLICATION_XML = "application/xml";
    public static final String MIME_APPLICATION_XML_DTD = "application/xml-dtd";
    public static final String MIME_IMAGE_BMP = "image/bmp";
    public static final String MIME_IMAGE_CGM = "image/cgm";
    public static final String MIME_IMAGE_GIF = "image/gif";
    public static final String MIME_IMAGE_IEF = "image/ief";
    public static final String MIME_IMAGE_JPEG = "image/jpeg";
    public static final String MIME_IMAGE_TIFF = "image/tiff";
    public static final String MIME_IMAGE_PNG = "image/png";
    public static final String MIME_IMAGE_SVG_XML = "image/svg+xml";
    public static final String MIME_IMAGE_VND_DJVU = "image/vnd.djvu";
    public static final String MIME_IMAGE_WAP_WBMP = "image/vnd.wap.wbmp";
    public static final String MIME_IMAGE_X_CMU_RASTER = "image/x-cmu-raster";
    public static final String MIME_IMAGE_X_ICON = "image/x-icon";
    public static final String MIME_IMAGE_X_PORTABLE_ANYMAP = "image/x-portable-anymap";
    public static final String MIME_IMAGE_X_PORTABLE_BITMAP = "image/x-portable-bitmap";
    public static final String MIME_IMAGE_X_PORTABLE_GRAYMAP = "image/x-portable-graymap";
    public static final String MIME_IMAGE_X_PORTABLE_PIXMAP = "image/x-portable-pixmap";
    public static final String MIME_IMAGE_X_RGB = "image/x-rgb";
    public static final String MIME_AUDIO_BASIC = "audio/basic";
    public static final String MIME_AUDIO_MIDI = "audio/midi";
    public static final String MIME_AUDIO_MPEG = "audio/mpeg";
    public static final String MIME_AUDIO_X_AIFF = "audio/x-aiff";
    public static final String MIME_AUDIO_X_MPEGURL = "audio/x-mpegurl";
    public static final String MIME_AUDIO_X_PN_REALAUDIO = "audio/x-pn-realaudio";
    public static final String MIME_AUDIO_X_WAV = "audio/x-wav";
    public static final String MIME_CHEMICAL_X_PDB = "chemical/x-pdb";
    public static final String MIME_CHEMICAL_X_XYZ = "chemical/x-xyz";
    public static final String MIME_MODEL_IGES = "model/iges";
    public static final String MIME_MODEL_MESH = "model/mesh";
    public static final String MIME_MODEL_VRLM = "model/vrml";
    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String MIME_TEXT_RICHTEXT = "text/richtext";
    public static final String MIME_TEXT_RTF = "text/rtf";
    public static final String MIME_TEXT_HTML = "text/html";
    public static final String MIME_TEXT_CALENDAR = "text/calendar";
    public static final String MIME_TEXT_CSS = "text/css";
    public static final String MIME_TEXT_SGML = "text/sgml";
    public static final String MIME_TEXT_TAB_SEPARATED_VALUES = "text/tab-separated-values";
    public static final String MIME_TEXT_VND_WAP_XML = "text/vnd.wap.wml";
    public static final String MIME_TEXT_VND_WAP_WMLSCRIPT = "text/vnd.wap.wmlscript";
    public static final String MIME_TEXT_X_SETEXT = "text/x-setext";
    public static final String MIME_TEXT_X_COMPONENT = "text/x-component";
    public static final String MIME_VIDEO_QUICKTIME = "video/quicktime";
    public static final String MIME_VIDEO_MPEG = "video/mpeg";
    public static final String MIME_VIDEO_VND_MPEGURL = "video/vnd.mpegurl";
    public static final String MIME_VIDEO_X_MSVIDEO = "video/x-msvideo";
    public static final String MIME_VIDEO_X_MS_WMV = "video/x-ms-wmv";
    public static final String MIME_VIDEO_X_SGI_MOVIE = "video/x-sgi-movie";
    public static final String MIME_X_CONFERENCE_X_COOLTALK = "x-conference/x-cooltalk";

    private final static Map<String, String> MIMETYPE_EXTENSIONS;

    static {
        MIMETYPE_EXTENSIONS = Map.<String, String>ofEntries(
                entry(MIME_APPLICATION_VND_MOZZILLA_XUL_XML, "xul"),
                entry(MIME_APPLICATION_JSON, "json"),
                entry(MIME_X_CONFERENCE_X_COOLTALK, "ice"),
                entry(MIME_VIDEO_X_SGI_MOVIE, "movie"),
                entry(MIME_VIDEO_X_MSVIDEO, "avi"),
                entry(MIME_VIDEO_X_MS_WMV, "wmv"),
                entry(MIME_VIDEO_VND_MPEGURL, "m4u"),
                entry(MIME_TEXT_X_COMPONENT, "htc"),
                entry(MIME_TEXT_X_SETEXT, "etx"),
                entry(MIME_TEXT_VND_WAP_WMLSCRIPT, "wmls"),
                entry(MIME_TEXT_VND_WAP_XML, "wml"),
                entry(MIME_TEXT_TAB_SEPARATED_VALUES, "tsv"),
                entry(MIME_TEXT_SGML, "sgml"),
                entry(MIME_TEXT_CSS, "css"),
                entry(MIME_TEXT_CALENDAR, "ics"),
                entry(MIME_MODEL_VRLM, "vrlm"),
                entry(MIME_MODEL_MESH, "mesh"),
                entry(MIME_MODEL_IGES, "iges"),
                entry(MIME_IMAGE_X_RGB, "rgb"),
                entry(MIME_IMAGE_X_PORTABLE_PIXMAP, "ppm"),
                entry(MIME_IMAGE_X_PORTABLE_GRAYMAP, "pgm"),
                entry(MIME_IMAGE_X_PORTABLE_BITMAP, "pbm"),
                entry(MIME_IMAGE_X_PORTABLE_ANYMAP, "pnm"),
                entry(MIME_IMAGE_X_ICON, "ico"),
                entry(MIME_IMAGE_X_CMU_RASTER, "ras"),
                entry(MIME_IMAGE_WAP_WBMP, "wbmp"),
                entry(MIME_IMAGE_VND_DJVU, "djvu"),
                entry(MIME_IMAGE_SVG_XML, "svg"),
                entry(MIME_IMAGE_IEF, "ief"),
                entry(MIME_IMAGE_CGM, "cgm"),
                entry(MIME_IMAGE_BMP, "bmp"),
                entry(MIME_CHEMICAL_X_XYZ, "xyz"),
                entry(MIME_CHEMICAL_X_PDB, "pdb"),
                entry(MIME_AUDIO_X_PN_REALAUDIO, "ra"),
                entry(MIME_AUDIO_X_MPEGURL, "m3u"),
                entry(MIME_AUDIO_X_AIFF, "aiff"),
                entry(MIME_AUDIO_MPEG, "mp3"),
                entry(MIME_AUDIO_MIDI, "midi"),
                entry(MIME_APPLICATION_XML_DTD, "dtd"),
                entry(MIME_APPLICATION_XML, "xml"),
                entry(MIME_APPLICATION_XSLT_XML, "xslt"),
                entry(MIME_APPLICATION_XHTML_XML, "xhtml"),
                entry(MIME_APPLICATION_X_WAIS_SOURCE, "src"),
                entry(MIME_APPLICATION_X_USTAR, "ustar"),
                entry(MIME_APPLICATION_X_TROFF_MS, "ms"),
                entry(MIME_APPLICATION_X_TROFF_ME, "me"),
                entry(MIME_APPLICATION_X_TROFF_MAN, "man"),
                entry(MIME_APPLICATION_X_TROFF, "roff"),
                entry(MIME_APPLICATION_X_TEXINFO, "texi"),
                entry(MIME_APPLICATION_X_TEX, "tex"),
                entry(MIME_APPLICATION_X_TCL, "tcl"),
                entry(MIME_APPLICATION_X_SV4CRC, "sv4crc"),
                entry(MIME_APPLICATION_X_SV4CPIO, "sv4cpio"),
                entry(MIME_APPLICATION_X_STUFFIT, "sit"),
                entry(MIME_APPLICATION_X_SHOCKWAVE_FLASH, "swf"),
                entry(MIME_APPLICATION_X_SHAR, "shar"),
                entry(MIME_APPLICATION_X_SH, "sh"),
                entry(MIME_APPLICATION_X_NETCDF, "cdf"),
                entry(MIME_APPLICATION_X_LATEX, "latex"),
                entry(MIME_APPLICATION_X_KOAN, "skm"),
                entry(MIME_APPLICATION_X_JAVASCRIPT, "js"),
                entry(MIME_APPLICATION_X_HDF, "hdf"),
                entry(MIME_APPLICATION_X_GTAR, "gtar"),
                entry(MIME_APPLICATION_X_FUTURESPLASH, "spl"),
                entry(MIME_APPLICATION_X_DVI, "dvi"),
                entry(MIME_APPLICATION_X_DIRECTOR, "dir"),
                entry(MIME_APPLICATION_X_CSH, "csh"),
                entry(MIME_APPLICATION_X_CPIO, "cpio"),
                entry(MIME_APPLICATION_X_CHESS_PGN, "pgn"),
                entry(MIME_APPLICATION_X_CDLINK, "vcd"),
                entry(MIME_APPLICATION_X_BCPIO, "bcpio"),
                entry(MIME_APPLICATION_VND_RNREALMEDIA, "rm"),
                entry(MIME_APPLICATION_VND_MSPOWERPOINT, "ppt"),
                entry(MIME_APPLICATION_VND_MIF, "mif"),
                entry(MIME_APPLICATION_SRGS_XML, "grxml"),
                entry(MIME_APPLICATION_SRGS, "gram"),
                entry(MIME_APPLICATION_RDF_SMIL, "smil"),
                entry(MIME_APPLICATION_RDF_XML, "rdf"),
                entry(MIME_APPLICATION_X_OGG, "ogg"),
                entry(MIME_APPLICATION_ODA, "oda"),
                entry(MIME_APPLICATION_MATHML_XML, "mathml"),
                entry(MIME_APPLICATION_MAC_COMPACTPRO, "cpt"),
                entry(MIME_APPLICATION_MAC_BINHEX40, "hqx"),
                entry(MIME_APPLICATION_JNLP, "jnlp"),
                entry(MIME_APPLICATION_ANDREW_INSET, "ez"),
                entry(MIME_TEXT_PLAIN, "txt"),
                entry(MIME_TEXT_RTF, "rtf"),
                entry(MIME_TEXT_RICHTEXT, "rtx"),
                entry(MIME_TEXT_HTML, "html"),
                entry(MIME_APPLICATION_ZIP, "zip"),
                entry(MIME_APPLICATION_X_RAR_COMPRESSED, "rar"),
                entry(MIME_APPLICATION_X_GZIP, "gzip"),
                entry(MIME_APPLICATION_TGZ, "tgz"),
                entry(MIME_APPLICATION_X_TAR, "tar"),
                entry(MIME_IMAGE_GIF, "gif"),
                entry(MIME_IMAGE_JPEG, "jpg"),
                entry(MIME_IMAGE_TIFF, "tiff"),
                entry(MIME_IMAGE_PNG, "png"),
                entry(MIME_AUDIO_BASIC, "au"),
                entry(MIME_AUDIO_X_WAV, "wav"),
                entry(MIME_VIDEO_QUICKTIME, "mov"),
                entry(MIME_VIDEO_MPEG, "mpg"),
                entry(MIME_APPLICATION_MSWORD, "doc"),
                entry(MIME_APPLICATION_MSWORD_2007, "docx"),
                entry(MIME_APPLICATION_VND_TEXT, "odt"),
                entry(MIME_APPLICATION_VND_MSEXCEL, "xls"),
                entry(MIME_APPLICATION_VND_SPREADSHEET, "ods"),
                entry(MIME_APPLICATION_POSTSCRIPT, "ps"),
                entry(MIME_APPLICATION_PDF, "pdf"),
                entry(MIME_APPLICATION_OCTET_STREAM, "exe"),
                entry(MIME_APPLICATION_JAVA_ARCHIVE, "jar")
        );
    }

    public static String getExtension(String mimeType) {
        String ext = MIMETYPE_EXTENSIONS.get(mimeType.toLowerCase());
        if (ext == null) {
            return "";
        }
        return "." + ext;
    }
}
