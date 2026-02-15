package com.hitachi.mockswitch.util;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import com.hitachi.mockswitch.iso.MockIsoPackager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Generates sample ISO 8583 bytes for Switch → IMPS flow (Req* and Resp*).
 * Run as main to print base64 strings for collection variables.
 * Run with argument "write" to write .bin files to ../postman/samples/
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

        boolean writeFiles = args.length > 0 && "write".equalsIgnoreCase(args[0]);
        Path outDir = writeFiles ? Paths.get("..", "postman", "samples").toAbsolutePath().normalize() : null;
        if (writeFiles && outDir != null) {
            try { Files.createDirectories(outDir); } catch (IOException e) { System.err.println("Cannot create " + outDir + ": " + e.getMessage()); }
        }

        // Req* (0200/0800)
        byte[] reqPay = buildReqPay(packager, nowTime, nowDate, stan, rrn);
        byte[] reqChkTxn = buildReqChkTxn(packager, nowTime, nowDate, stan, rrn);
        byte[] reqHbt = buildReqHbt(packager, nowTime, nowDate, stan, rrn);
        byte[] reqListAccPvd = buildReqListAccPvd(packager, nowTime, nowDate, stan, rrn);
        byte[] reqValAdd = buildReqValAdd(packager, nowTime, nowDate, stan, rrn);

        System.out.println("-- ReqPay 0200 --");
        System.out.println(toBase64(reqPay));
        System.out.println("-- ReqChkTxn 0200 --");
        System.out.println(toBase64(reqChkTxn));
        System.out.println("-- ReqHbt 0800 --");
        System.out.println(toBase64(reqHbt));
        System.out.println("-- ReqListAccPvd 0200 --");
        System.out.println(toBase64(reqListAccPvd));
        System.out.println("-- ReqValAdd 0200 --");
        System.out.println(toBase64(reqValAdd));

        // Resp* (0210/0810) – reverse flow: Switch → IMPS
        byte[] respPay = buildRespPay(packager, nowTime, nowDate, stan, rrn);
        byte[] respChkTxn = buildRespChkTxn(packager, nowTime, nowDate, stan, rrn);
        byte[] respHbt = buildRespHbt(packager, nowTime, nowDate, stan, rrn);
        byte[] respListAccPvd = buildRespListAccPvd(packager, nowTime, nowDate, stan, rrn);
        byte[] respValAdd = buildRespValAdd(packager, nowTime, nowDate, stan, rrn);

        System.out.println("-- RespPay 0210 --");
        System.out.println(toBase64(respPay));
        System.out.println("-- RespChkTxn 0210 --");
        System.out.println(toBase64(respChkTxn));
        System.out.println("-- RespHbt 0810 --");
        System.out.println(toBase64(respHbt));
        System.out.println("-- RespListAccPvd 0210 --");
        System.out.println(toBase64(respListAccPvd));
        System.out.println("-- RespValAdd 0210 --");
        System.out.println(toBase64(respValAdd));

        if (writeFiles && outDir != null) {
            write(outDir, "iso_reqpay.bin", reqPay);
            write(outDir, "iso_reqchktxn.bin", reqChkTxn);
            write(outDir, "iso_reqhbt.bin", reqHbt);
            write(outDir, "iso_reqlistaccpvd.bin", reqListAccPvd);
            write(outDir, "iso_reqvaladd.bin", reqValAdd);
            write(outDir, "iso_resppay.bin", respPay);
            write(outDir, "iso_respchktxn.bin", respChkTxn);
            write(outDir, "iso_resphbt.bin", respHbt);
            write(outDir, "iso_resplistaccpvd.bin", respListAccPvd);
            write(outDir, "iso_respvaladd.bin", respValAdd);
            System.out.println("Written .bin files to " + outDir);
        }
    }

    private static void write(Path dir, String name, byte[] bytes) {
        try {
            Files.write(dir.resolve(name), bytes);
        } catch (IOException e) {
            System.err.println("Failed to write " + name + ": " + e.getMessage());
        }
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

    // ---------- Resp* (0210/0810) for Switch → IMPS reverse flow ----------
    private static byte[] buildRespPay(MockIsoPackager packager, String time, String date, String stan, String rrn) throws ISOException {
        ISOMsg iso = new ISOMsg();
        iso.setPackager(packager);
        iso.setMTI("0210");
        iso.set(3, "400000");
        iso.set(4, "000000100000");
        iso.set(11, stan);
        iso.set(12, time);
        iso.set(13, date);
        iso.set(37, rrn);
        iso.set(38, "APPR01");
        iso.set(39, "00");
        iso.set(41, "IMPSTERM");
        iso.set(49, "356");
        iso.set(102, "1234567890123456");
        iso.set(103, "1111222233334444");
        iso.set(120, "BAN00000000000000000000000000000001");
        return pack(iso);
    }

    private static byte[] buildRespChkTxn(MockIsoPackager packager, String time, String date, String stan, String rrn) throws ISOException {
        ISOMsg iso = new ISOMsg();
        iso.setPackager(packager);
        iso.setMTI("0210");
        iso.set(3, "380000");
        iso.set(11, stan);
        iso.set(12, time);
        iso.set(13, date);
        iso.set(37, rrn);
        iso.set(38, "CHK001");
        iso.set(39, "00");
        iso.set(41, "IMPSTERM");
        iso.set(48, "BAN00000000000000000000000000000001");
        iso.set(120, "BAN00000000000000000000000000000002");
        return pack(iso);
    }

    private static byte[] buildRespHbt(MockIsoPackager packager, String time, String date, String stan, String rrn) throws ISOException {
        ISOMsg iso = new ISOMsg();
        iso.setPackager(packager);
        iso.setMTI("0810");
        iso.set(3, "990000");
        iso.set(11, stan);
        iso.set(12, time);
        iso.set(13, date);
        iso.set(24, "831");
        iso.set(37, rrn);
        iso.set(39, "00");
        iso.set(41, "IMPSTERM");
        iso.set(120, "BAN00000000000000000000000000000003");
        return pack(iso);
    }

    private static byte[] buildRespListAccPvd(MockIsoPackager packager, String time, String date, String stan, String rrn) throws ISOException {
        ISOMsg iso = new ISOMsg();
        iso.setPackager(packager);
        iso.setMTI("0210");
        iso.set(3, "320000");
        iso.set(11, stan);
        iso.set(12, time);
        iso.set(13, date);
        iso.set(37, rrn);
        iso.set(39, "00");
        iso.set(41, "IMPSTERM");
        iso.set(48, "HDFC|ICICI|SBI|AXIS");
        iso.set(120, "BAN00000000000000000000000000000004");
        return pack(iso);
    }

    private static byte[] buildRespValAdd(MockIsoPackager packager, String time, String date, String stan, String rrn) throws ISOException {
        ISOMsg iso = new ISOMsg();
        iso.setPackager(packager);
        iso.setMTI("0210");
        iso.set(3, "310000");
        iso.set(11, stan);
        iso.set(12, time);
        iso.set(13, date);
        iso.set(33, "ICIC0000001");
        iso.set(37, rrn);
        iso.set(38, "VAL001");
        iso.set(39, "00");
        iso.set(41, "IMPSTERM");
        iso.set(48, "Sajid Mulla");
        iso.set(102, "1111222233334444");
        iso.set(120, "BAN00000000000000000000000000000005");
        return pack(iso);
    }
}
