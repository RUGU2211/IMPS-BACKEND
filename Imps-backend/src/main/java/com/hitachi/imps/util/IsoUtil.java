package com.hitachi.imps.util;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;

import java.nio.charset.StandardCharsets;

public class IsoUtil {

    /* ===============================
       BYTE[] → ISOMsg  (REAL SWITCH)
       =============================== */
    public static ISOMsg unpack(byte[] data, ISOPackager packager) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(packager);
            iso.unpack(data);
            return iso;
        } catch (Exception e) {
            throw new RuntimeException("ISO unpack failed", e);
        }
    }

    /* ===============================
       STRING → ISOMsg (MOCK / LOG)
       =============================== */
    public static ISOMsg unpack(String isoText, ISOPackager packager) {
        try {
            return unpack(
                isoText.getBytes(StandardCharsets.ISO_8859_1),
                packager
            );
        } catch (Exception e) {
            throw new RuntimeException("ISO unpack failed (string)", e);
        }
    }

    /* ===============================
       ISOMsg → byte[]
       =============================== */
    public static byte[] pack(ISOMsg iso) {
        try {
            return iso.pack();
        } catch (Exception e) {
            throw new RuntimeException("ISO pack failed", e);
        }
    }
}
