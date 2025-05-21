package com.agms.backend.service;

import com.agms.backend.dto.ESignRequest;
import com.agms.backend.dto.ESignResponse;

/**
 * Service interface for managing e-sign operations.
 */
public interface ESignService {
    /**
     * Signs a document with e-signature
     * @param request The e-sign request containing email and password
     * @return ESignResponse containing timestamp and eSignId
     */
    ESignResponse signESign(ESignRequest request);
}
