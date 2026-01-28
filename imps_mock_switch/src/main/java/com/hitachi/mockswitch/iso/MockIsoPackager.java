package com.hitachi.mockswitch.iso;

import org.jpos.iso.*;

/**
 * ISO 8583 Packager for IMPS Mock Switch.
 * Must match EXACTLY with ImpsIsoPackager in IMPS Backend.
 */
public class MockIsoPackager extends ISOBasePackager {

    protected ISOFieldPackager[] fld = new ISOFieldPackager[129];

    public MockIsoPackager() {
        super();
        setFieldPackager(fld);

        // -------------------------------------------------
        // MTI & BITMAPS (handled by ISOBasePackager)
        // -------------------------------------------------
        fld[0] = new IFA_NUMERIC(4, "Message Type Indicator");
        fld[1] = new IFA_BITMAP(16, "Bitmap");

        // -------------------------------------------------
        // BASIC TRANSACTION DATA
        // -------------------------------------------------
        fld[2]  = new IFB_LLNUM(19, "PAN / Account Number", false);   // DE-2
        fld[3]  = new IFB_NUMERIC(6, "Processing Code", true);       // DE-3
        fld[4]  = new IFB_NUMERIC(12, "Transaction Amount", true);  // DE-4
        fld[5]  = new IFB_NUMERIC(12, "Settlement Amount", true);   // DE-5
        fld[6]  = new IFB_NUMERIC(12, "Cardholder Amount", true);   // DE-6
        fld[7]  = new IFB_NUMERIC(10, "Transmission Date Time", true); // DE-7
        fld[11] = new IFB_NUMERIC(6, "STAN", true);                 // DE-11
        fld[12] = new IFB_NUMERIC(6, "Local Time", true);           // DE-12
        fld[13] = new IFB_NUMERIC(4, "Local Date", true);           // DE-13
        fld[15] = new IFB_NUMERIC(4, "Settlement Date", true);      // DE-15
        fld[18] = new IFB_NUMERIC(4, "Merchant Type", true);        // DE-18
        fld[22] = new IFB_NUMERIC(3, "POS Entry Mode", true);       // DE-22
        fld[24] = new IFB_NUMERIC(3, "Function Code", true);        // DE-24
        fld[25] = new IFB_NUMERIC(2, "POS Condition Code", true);   // DE-25

        // -------------------------------------------------
        // AMOUNT FIELDS
        // -------------------------------------------------
        fld[28] = new IFB_AMOUNT(0, "Transaction Fee Amount", false);         // DE-28
        fld[29] = new IFB_AMOUNT(0, "Settlement Fee Amount", false);          // DE-29
        fld[30] = new IFB_AMOUNT(0,"Processing Fee Amount", false);          // DE-30

        // -------------------------------------------------
        // IDENTIFIERS
        // -------------------------------------------------
        fld[31] = new IFA_LLCHAR(11, "Acquirer Reference Data");     // DE-31
        fld[32] = new IFA_LLCHAR(11, "Acquiring Institution ID");    // DE-32 (IFSC)
        fld[33] = new IFA_LLCHAR(11, "Forwarding Institution ID");   // DE-33 (IFSC)
        fld[35] = new IFA_LLCHAR(37, "Track 2 Data");               // DE-35
        fld[37] = new IF_CHAR(12, "RRN");                           // DE-37
        fld[38] = new IF_CHAR(6, "Authorization ID");              // DE-38
        fld[39] = new IF_CHAR(2, "Response Code");                 // DE-39

        // -------------------------------------------------
        // TERMINAL / MERCHANT
        // -------------------------------------------------
        fld[41] = new IF_CHAR(8, "Terminal ID");                    // DE-41
        fld[42] = new IF_CHAR(15, "Merchant ID");                   // DE-42
        fld[43] = new IF_CHAR(40, "Merchant Name & Location");      // DE-43

        // -------------------------------------------------
        // ADDITIONAL DATA
        // -------------------------------------------------
        fld[44] = new IFA_LLCHAR(25, "Additional Response Data");   // DE-44
        fld[46] = new IFA_LLLCHAR(999, "Additional Data ISO");      // DE-46
        fld[48] = new IFA_LLLCHAR(999, "Additional Data Private");  // DE-48

        // -------------------------------------------------
        // CURRENCY & SECURITY
        // -------------------------------------------------
        fld[49] = new IF_CHAR(3, "Transaction Currency Code");      // DE-49
        fld[52] = new IFB_BINARY(8, "PIN Data");                    // DE-52
        fld[64] = new IFB_BINARY(8, "MAC");                         // DE-64

        // -------------------------------------------------
        // ACCOUNT IDENTIFICATION (IMPS SPECIFIC)
        // -------------------------------------------------
        fld[102] = new IFA_LLCHAR(28, "Account ID 1 - Payer Account");    // DE-102
        fld[103] = new IFA_LLCHAR(28, "Account ID 2 - Payee Account");    // DE-103

        // -------------------------------------------------
        // ADDITIONAL IMPS FIELDS
        // -------------------------------------------------
        fld[120] = new IFA_LLLCHAR(999, "Record Data");             // DE-120
        fld[121] = new IFA_LLLCHAR(999, "Authorizing Agent ID");    // DE-121
        fld[123] = new IFA_LLLCHAR(999, "Receipt Free Text");       // DE-123
        fld[124] = new IFA_LLLCHAR(999, "Info Text");               // DE-124
        fld[125] = new IFA_LLLCHAR(999, "Network Data");            // DE-125
        fld[126] = new IFA_LLLCHAR(999, "IMPS Specific Data");      // DE-126

        fld[128]= new IFB_BINARY(8, "Secondary MAC");               // DE-128
    }
}
