package com.evolveum.midpoint.web.util;

import io.nayuki.qrcodegen.QrCode;

public class QRCodeUtils {

    public static String generateSvg(String data) {
        QrCode code = QrCode.encodeText(data, QrCode.Ecc.MEDIUM);

        long border = 1;

        long size = code.size + border * 2;
        StringBuilder sb = new StringBuilder()
                .append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" ")
                .append("viewBox=\"0 0 ").append(size).append(" ").append(size).append("\" ")
                .append("stroke=\"none\">\n")
                .append("\t<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n")
                .append("\t<path d=\"");
        for (int y = 0; y < code.size; y++) {
            for (int x = 0; x < code.size; x++) {
                if (code.getModule(x, y)) {
                    if (x != 0 || y != 0) {
                        sb.append(" ");
                    }
                    sb.append("M").append(x + border).append(",").append(y + border).append("h1v1h-1z");
                }
            }
        }

        sb.append("\" fill=\"#000000\"/>\n")
                .append("</svg>");

        return sb.toString();
    }
}
