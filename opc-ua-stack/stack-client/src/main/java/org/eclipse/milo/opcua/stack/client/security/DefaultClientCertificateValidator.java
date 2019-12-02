/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.stack.client.security;

import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.TrustListManager;
import org.eclipse.milo.opcua.stack.core.util.validation.CertificateValidationUtil;
import org.eclipse.milo.opcua.stack.core.util.validation.ValidationCheck;

public class DefaultClientCertificateValidator implements ClientCertificateValidator {

    private final TrustListManager trustListManager;
    private final ImmutableSet<ValidationCheck> validationChecks;

    public DefaultClientCertificateValidator(TrustListManager trustListManager) {
        this(trustListManager, ValidationCheck.NO_OPTIONAL_CHECKS);
    }

    public DefaultClientCertificateValidator(
        TrustListManager trustListManager,
        Set<ValidationCheck> validationChecks
    ) {

        this.trustListManager = trustListManager;
        this.validationChecks = ImmutableSet.copyOf(validationChecks);
    }

    @Override
    public void validateCertificateChain(List<X509Certificate> certificateChain) throws UaException {
        PKIXCertPathBuilderResult certPathResult = CertificateValidationUtil.buildTrustedCertPath(
            certificateChain,
            trustListManager.getTrustedCertificates(),
            trustListManager.getIssuerCertificates()
        );

        List<X509CRL> crls = new ArrayList<>();
        crls.addAll(trustListManager.getTrustedCrls());
        crls.addAll(trustListManager.getIssuerCrls());

        CertificateValidationUtil.validateTrustedCertPath(
            certPathResult.getCertPath(),
            certPathResult.getTrustAnchor(),
            crls,
            validationChecks
        );
    }

    @Override
    public void validateCertificateChain(
        List<X509Certificate> certificateChain,
        String applicationUri,
        String... validHostNames
    ) throws UaException {

        validateCertificateChain(certificateChain);

        X509Certificate certificate = certificateChain.get(0);

        CertificateValidationUtil.checkApplicationUri(certificate, applicationUri);
        CertificateValidationUtil.checkHostnameOrIpAddress(certificate, validHostNames);
    }

}
