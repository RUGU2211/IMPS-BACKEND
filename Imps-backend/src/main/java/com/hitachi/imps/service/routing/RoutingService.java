package com.hitachi.imps.service.routing;

import java.util.Optional;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.hitachi.imps.config.RoutingConfig;
import com.hitachi.imps.entity.InstitutionMaster;
import com.hitachi.imps.repository.InstitutionMasterRepository;
import com.hitachi.imps.util.IsoUtil;

/**
 * Service for routing messages between NPCI and Switch.
 * Uses RoutingConfig for endpoint configuration.
 */
@Service
public class RoutingService {

    @Autowired
    private RoutingConfig routingConfig;

    @Autowired
    private InstitutionMasterRepository institutionRepo;

    private final RestTemplate rest = new RestTemplate();

    /**
     * Send ISO message to Switch
     */
    public byte[] sendToSwitch(String endpointKey, ISOMsg iso) {
        String url = routingConfig.getSwitch().getFullUrl(endpointKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        HttpEntity<byte[]> request = new HttpEntity<>(IsoUtil.pack(iso), headers);

        try {
            return rest.postForObject(url, request, byte[].class);
        } catch (Exception e) {
            System.err.println("Switch send failed [" + endpointKey + "]: " + e.getMessage());
            return null;
        }
    }

    /**
     * Send ISO bytes to Switch
     */
    public byte[] sendToSwitch(String endpointKey, byte[] isoBytes) {
        String url = routingConfig.getSwitch().getFullUrl(endpointKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        HttpEntity<byte[]> request = new HttpEntity<>(isoBytes, headers);

        try {
            return rest.postForObject(url, request, byte[].class);
        } catch (Exception e) {
            System.err.println("Switch send failed [" + endpointKey + "]: " + e.getMessage());
            return null;
        }
    }

    /**
     * Send XML to NPCI
     */
    public String sendToNpci(String endpointKey, String xml) {
        String url = routingConfig.getNpci().getFullUrl(endpointKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);

        HttpEntity<String> request = new HttpEntity<>(xml, headers);

        try {
            return rest.postForObject(url, request, String.class);
        } catch (Exception e) {
            System.err.println("NPCI send failed [" + endpointKey + "]: " + e.getMessage());
            return null;
        }
    }

    /**
     * Route to institution by IFSC code
     */
    public InstitutionMaster routeByIfsc(String payeeIfsc) {
        if (payeeIfsc == null || payeeIfsc.isEmpty()) {
            return null;
        }

        // Extract first 4 characters for bank identification
        String bankPrefix = payeeIfsc.length() >= 4 ? payeeIfsc.substring(0, 4) : payeeIfsc;

        Optional<InstitutionMaster> institution = institutionRepo.findByIfscCode(payeeIfsc);

        return institution.orElse(null);
    }

    /**
     * Get NPCI base URL
     */
    public String getNpciBaseUrl() {
        return routingConfig.getNpci().getBaseUrl();
    }

    /**
     * Get Switch base URL
     */
    public String getSwitchBaseUrl() {
        return routingConfig.getSwitch().getBaseUrl();
    }
}
