package com.hitachi.imps.service.pay.reqpay;

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hitachi.imps.exception.CommonCodeValidationException;
import com.hitachi.imps.exception.ReqPayValidationException;
import com.hitachi.imps.service.iso.XmlUtil;
import com.hitachi.imps.service.validation.CommonCodeValidationService;
import com.hitachi.imps.spec.NpciReqPayRules;

/**
 * Applies NPCI ReqPay rules (Section 8.1) and fixed enumerations (8.2).
 * Validates incoming ReqPay XML before conversion to ISO / Switch.
 * Common Head/Txn rules (019, 020, 021, 022) are applied via CommonCodeValidationService for all txn types; here we add ReqPay-specific rules.
 */
@Service
public class ReqPayValidationService {

    @Autowired
    private CommonCodeValidationService commonCodeValidationService;

    private static final Pattern TWO_DECIMALS = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d+$");

    /**
     * Validate ReqPay XML: common rules (019, 020, 021, 022) then ReqPay-specific (024, 026, 029, 034, 035, 042–044, 048, 049, 051, 052, 8.2).
     *
     * @param xml ReqPay request XML
     * @throws ReqPayValidationException if any rule fails
     */
    public void validate(String xml) throws ReqPayValidationException {
        ReqPayValidationException errors = new ReqPayValidationException();

        // Apply common Head/Txn rules (019, 020, 021, 022) – same as for all request types
        try {
            commonCodeValidationService.validateCommonHeadTxn(xml);
        } catch (CommonCodeValidationException e) {
            for (int i = 0; i < e.getRuleIds().size(); i++) {
                errors.addError(
                    e.getRuleIds().get(i),
                    i < e.getMessages().size() ? e.getMessages().get(i) : e.getMessage()
                );
            }
        }

        Map<String, String> tags = XmlUtil.parseReqPay(xml);

        // Rule 029 – Payer @type PERSON | ENTITY
        String payerType = XmlUtil.read(xml, "//*[local-name()='Payer']/@type");
        if (payerType != null && !payerType.isBlank() && !NpciReqPayRules.isPayerPayeeTypeAllowed(payerType)) {
            errors.addError("029_Payer/Payee_Type", "Payer type must be PERSON or ENTITY; got: " + payerType);
        }

        // Rule 024 – Payer @code: 0000 for PERSON, 4-digit for ENTITY
        String payerCode = XmlUtil.read(xml, "//*[local-name()='Payer']/@code");
        if (payerType != null && payerCode != null && !payerCode.isBlank() && !NpciReqPayRules.isPayerPayeeCodeValid(payerType, payerCode)) {
            errors.addError("024_Txn_code", "Payer code must be 0000 for PERSON or 4-digit MCC for ENTITY; got: " + payerCode);
        }

        // Rule 026 – Payer Info/Rating verifiedAddress TRUE | FALSE
        String payerRating = XmlUtil.read(xml, "//*[local-name()='Payer']//*[local-name()='Rating']/@verifiedAddress");
        if (payerRating != null && !payerRating.isBlank() && !NpciReqPayRules.isInfoRatingAllowed(payerRating)) {
            errors.addError("026_Payer/Payee_InfoRating", "Rating verifiedAddress must be TRUE or FALSE; got: " + payerRating);
        }

        // Rule 029 – Payee @type
        String payeeType = XmlUtil.read(xml, "//*[local-name()='Payee']/@type");
        if (payeeType != null && !payeeType.isBlank() && !NpciReqPayRules.isPayerPayeeTypeAllowed(payeeType)) {
            errors.addError("029_Payer/Payee_Type", "Payee type must be PERSON or ENTITY; got: " + payeeType);
        }

        // Rule 024 – Payee @code
        String payeeCode = XmlUtil.read(xml, "//*[local-name()='Payee']/@code");
        if (payeeType != null && payeeCode != null && !payeeCode.isBlank() && !NpciReqPayRules.isPayerPayeeCodeValid(payeeType, payeeCode)) {
            errors.addError("024_Txn_code", "Payee code must be 0000 for PERSON or 4-digit MCC for ENTITY; got: " + payeeCode);
        }

        // Rule 035 – Device Tag name=TYPE: allowed enum, length 1–20
        String deviceType = XmlUtil.read(xml, "//*[local-name()='Payer']//*[local-name()='Device']//*[local-name()='Tag'][@name='TYPE']/@value");
        if (deviceType != null && !deviceType.isBlank() && !NpciReqPayRules.isDeviceTypeAllowed(deviceType.trim())) {
            errors.addError("035_ReqPay_DeviceDetails_type", "Device TYPE must be one of MOB, INET, WAP, IVR, ATM, BRC, MAT, SMS (length 1–20); got: " + deviceType);
        }

        // Rule 034 – Device Tag ID length 1–35 when present
        String deviceId = XmlUtil.read(xml, "//*[local-name()='Payer']//*[local-name()='Device']//*[local-name()='Tag'][@name='cardAccIdCode']/@value");
        if (deviceId != null && !deviceId.isBlank()) {
            int len = deviceId.length();
            if (len < NpciReqPayRules.DEVICE_ID_MIN_LEN || len > NpciReqPayRules.DEVICE_ID_MAX_LEN) {
                errors.addError("034_ReqPay_DeviceDetails_Values", "Device ID (cardAccIdCode) length must be 1–35; got: " + len);
            }
        }

        // 8.2 – Head.prodType: UPI | IMPS | AEPS
        String prodType = XmlUtil.read(xml, "//*[local-name()='Head']/@prodType");
        if (prodType != null && !prodType.isBlank() && !NpciReqPayRules.isProdTypeAllowed(prodType)) {
            errors.addError("8.2_prodType", "prodType must be one of: UPI, IMPS, AEPS; got: " + prodType);
        }

        // 8.1 Rule 052 – refCategory 00–09 when txnType is PAY or CREDIT
        String txnType = tags.get("txnType");
        String refCategory = XmlUtil.read(xml, "//*[local-name()='Txn']/@refCategory");
        if (refCategory != null && !refCategory.isBlank()
            && ("PAY".equals(txnType) || "CREDIT".equals(txnType))
            && !NpciReqPayRules.isRefCategoryAllowed(refCategory)) {
            errors.addError("052_ReqPay_Txn_RefCategory", "refCategory must be 00–09 for PAY/CREDIT; got: " + refCategory);
        }

        // 8.1 Rule 051 – Amount: numeric, 2 decimal places
        String amount = tags.get("amount");
        if (amount != null && !amount.isBlank()) {
            if (!TWO_DECIMALS.matcher(amount.trim()).matches()) {
                errors.addError("051_ReqPay_Amount_Value", "amount must be numeric with up to 2 decimal places; got: " + amount);
            }
        }

        // addrType: ACCOUNT → Rule 048 (IFSC 11-char, ACNUM max 30); MOBILE → Rule 049 (MOBNUM 12, MMID 7)
        String payerAddrType = XmlUtil.read(xml, "//*[local-name()='Payer']//*[local-name()='Ac']/@addrType");
        if (payerAddrType == null || payerAddrType.isBlank()) {
            payerAddrType = "ACCOUNT"; // default when Ac present
        }
        if ("ACCOUNT".equalsIgnoreCase(payerAddrType)) {
            String ifsc = tags.get("payer_ifsc");
            if (ifsc != null && !ifsc.isBlank() && ifsc.length() != NpciReqPayRules.IFSC_LENGTH) {
                errors.addError("048_ReqPay_Ac_name_Account", "IFSC must be 11 characters; got length: " + (ifsc != null ? ifsc.length() : 0));
            }
            String actype = tags.get("payer_actype");
            if (actype != null && !actype.isBlank() && !NpciReqPayRules.isActypeAllowed(actype)) {
                errors.addError("048_ReqPay_Ac_name_Account", "Payer ACTYPE must be one of: SAVINGS, DEFAULT, CURRENT, NRE, NRO, PPIWALLET, BANKWALLET, CREDIT, SOD, UOD, SEMICLOSEDPPIWALLET, SEMICLOSEDBANKWALLET, SNRR; got: " + actype);
            }
            String acnum = tags.get("payer_acnum");
            if (acnum != null && !acnum.isBlank()) {
                String digits = acnum.replaceAll("\\D", "");
                if (digits.length() > NpciReqPayRules.ACNUM_MAX_DIGITS) {
                    errors.addError("048_ReqPay_Ac_name_Account", "ACNUM max 30 digits; got: " + digits.length());
                }
            }
        } else if ("MOBILE".equalsIgnoreCase(payerAddrType)) {
            String mobNum = XmlUtil.read(xml, "//*[local-name()='Payer']//*[local-name()='Detail'][@name='MOBNUM']/@value");
            if (mobNum != null && !mobNum.isBlank()) {
                String digits = mobNum.replaceAll("\\D", "");
                if (digits.length() != NpciReqPayRules.MOBNUM_LENGTH) {
                    errors.addError("049_ReqPay_Ac_Name_Mobile", "MOBNUM must be +91 + 10 digits (12 digits); got: " + digits.length());
                }
            }
            String mmid = XmlUtil.read(xml, "//*[local-name()='Payer']//*[local-name()='Detail'][@name='MMID']/@value");
            if (mmid != null && !mmid.isBlank() && (!DIGITS_ONLY.matcher(mmid).matches() || mmid.length() != NpciReqPayRules.MMID_LENGTH)) {
                errors.addError("049_ReqPay_Ac_Name_Mobile", "MMID must be 7-digit numeric");
            }
        }

        // Payee addrType (same rules)
        String payeeAddrType = XmlUtil.read(xml, "//*[local-name()='Payee']//*[local-name()='Ac']/@addrType");
        if (payeeAddrType == null || payeeAddrType.isBlank()) payeeAddrType = "ACCOUNT";
        if ("ACCOUNT".equalsIgnoreCase(payeeAddrType)) {
            String ifsc = tags.get("payee_ifsc");
            if (ifsc != null && !ifsc.isBlank() && ifsc.length() != NpciReqPayRules.IFSC_LENGTH) {
                errors.addError("048_ReqPay_Ac_name_Account", "Payee IFSC must be 11 characters; got length: " + (ifsc != null ? ifsc.length() : 0));
            }
            String payeeActype = tags.get("payee_actype");
            if (payeeActype != null && !payeeActype.isBlank() && !NpciReqPayRules.isActypeAllowed(payeeActype)) {
                errors.addError("048_ReqPay_Ac_name_Account", "Payee ACTYPE must be from allowed enum (SAVINGS, DEFAULT, CURRENT, etc.); got: " + payeeActype);
            }
            String acnum = tags.get("payee_acnum");
            if (acnum != null && !acnum.isBlank()) {
                String digits = acnum.replaceAll("\\D", "");
                if (digits.length() > NpciReqPayRules.ACNUM_MAX_DIGITS) {
                    errors.addError("048_ReqPay_Ac_name_Account", "Payee ACNUM max 30 digits; got: " + digits.length());
                }
            }
        }

        // 8.1 Rule 042 – initiationMode "12" → Institution block mandatory (we only check type/route if present)
        String initiationMode = XmlUtil.read(xml, "//*[local-name()='Txn']/@initiationMode");
        if (NpciReqPayRules.INITIATION_MODE_FIR.equals(initiationMode)) {
            String instType = XmlUtil.read(xml, "//*[local-name()='Institution']/@type");
            if (instType != null && !instType.isBlank() && !NpciReqPayRules.isInstitutionTypeAllowed(instType)) {
                errors.addError("043_ReqPay_Institution_Type", "Institution type must be MTO or BANK; got: " + instType);
            }
            String route = XmlUtil.read(xml, "//*[local-name()='Institution']/@route");
            if (route != null && !route.isBlank() && !NpciReqPayRules.isRouteAllowed(route)) {
                errors.addError("044_ReqPay_Institution_Route", "Institution route must be MTSS or RDA; got: " + route);
            }
        }

        if (errors.hasErrors()) {
            throw errors;
        }
    }
}
