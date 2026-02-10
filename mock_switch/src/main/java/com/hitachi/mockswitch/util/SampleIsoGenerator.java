package com.hitachi.mockswitch.util;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import com.hitachi.mockswitch.iso.MockIsoPackager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Generates sample ISO 8583 bytes for Postman (Switch → IMPS flow).
 * Run as main to print base64 strings for collection variables.
 * Packager matches IMPS Backend (ImpsIsoPackager) for compatibility.
 */
public final class SampleIsoGenerator {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMdd");

    public static void main(String[] args) throws ISOException {
        MockIsoPackager packager = new MockIsoPackager();
        String nowTime = LocalDateTime.now().format(TIME_FORMAT);
        String nowDate = LocalDateTime.now().format(DATE_FORMAT);
        String stan = "123456";
        String rrn = String.valueOf(System.currentTimeMillis()).substring(1, 13);

        System.out.println("-- ReqPay 0200 --");
        System.out.println(toBase64(buildReqPay(packager, nowTime, nowDate, stan, rrn)));
        System.out.println("-- ReqChkTxn 0200 --");
        System.out.println(toBase64(buildReqChkTxn(packager, nowTime, nowDate, stan, rrn)));
        System.out.println("-- ReqHbt 0800 --");
        System.out.println(toBase64(buildReqHbt(packager, nowTime, nowDate, stan, rrn)));
        System.out.println("-- ReqListAccPvd 0200 --");
        System.out.println(toBase64(buildReqListAccPvd(packager, nowTime, nowDate, stan, rrn)));
        System.out.println("-- ReqValAdd 0200 --");
        System.out.println(toBase64(buildReqValAdd(packager, nowTime, nowDate, stan, rrn)));
    }

    private static String toBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] pack(ISOMsg iso) throws ISOException {
        return iso.pack();
    }

    private static byte[] buildReqPay(MockIsoPackager packager, String time, String date, String stan, String rrn) throws ISOException {
        ISOMsg iso = new ISOMsg();
        iso.setPackager(packager);
        iso.setMTI("0200");
        iso.set(3, "400000");
        iso.set(4, "000000100000");
        iso.set(11, stan);
        iso.set(12, time);
        iso.set(13, date);
        iso.set(32, "HDFC");
        iso.set(33, "ICIC0000001");
        iso.set(37, rrn);
        iso.set(41, "IMPSTERM");
        iso.set(49, "356");
        iso.set(102, "1234567890123456");
        iso.set(103, "1111222233334444");
        iso.set(120, "BAN00000000000000000000000000000001");
        return pack(iso);
    }

    private static byte[] buildReqChkTxn(MockIsoPackager packager, String time, String date, String stan, String rrn) throws ISOException {
        ISOMsg iso = new ISOMsg();
        iso.setPackager(packager);
        iso.setMTI("0200");
        iso.set(3, "380000");
        iso.set(4, "000000100000");
        iso.set(11, stan);
        iso.set(12, time);
        iso.set(13, date);
        iso.set(37, rrn);
        iso.set(41, "IMPSTERM");
        iso.set(49, "356");
        iso.set(48, "BAN00000000000000000000000000000001");
        iso.set(120, "BAN00000000000000000000000000000002");
        return pack(iso);
    }

    private static byte[] buildReqHbt(MockIsoPackager packager, String time, String date, String stan, String rrn) throws ISOException {
        ISOMsg iso = new ISOMsg();
        iso.setPackager(packager);
        iso.setMTI("0800");
        iso.set(3, "990000");
        iso.set(11, stan);
        iso.set(12, time);
        iso.set(13, date);
        iso.set(24, "831");
        iso.set(37, rrn);
        iso.set(41, "IMPSTERM");
        iso.set(48, "ALIVE");
        iso.set(120, "BAN00000000000000000000000000000003");
        return pack(iso);
    }

    private static byte[] buildReqListAccPvd(MockIsoPackager packager, String time, String date, String stan, String rrn) throws ISOException {
        ISOMsg iso = new ISOMsg();
        iso.setPackager(packager);
        iso.setMTI("0200");
        iso.set(3, "320000");
        iso.set(11, stan);
        iso.set(12, time);
        iso.set(13, date);
        iso.set(37, rrn);
        iso.set(41, "IMPSTERM");
        iso.set(120, "BAN00000000000000000000000000000004");
        return pack(iso);
    }

    private static byte[] buildReqValAdd(MockIsoPackager packager, String time, String date, String stan, String rrn) throws ISOException {
        ISOMsg iso = new ISOMsg();
        iso.setPackager(packager);
        iso.setMTI("0200");
        iso.set(3, "310000");
        iso.set(11, stan);
        iso.set(12, time);
        iso.set(13, date);
        iso.set(33, "ICIC0000001");
        iso.set(37, rrn);
        iso.set(41, "IMPSTERM");
        iso.set(49, "356");
        iso.set(102, "1111222233334444");
        iso.set(120, "BAN00000000000000000000000000000005");
        return pack(iso);
    }
}
