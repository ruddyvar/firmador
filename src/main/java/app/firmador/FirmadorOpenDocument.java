/* Firmador is a program to sign documents using AdES standards.

Copyright (C) 2019 Firmador authors.

This file is part of Firmador.

Firmador is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Firmador is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Firmador.  If not, see <http://www.gnu.org/licenses/>.  */

package app.firmador;



import java.security.KeyStore.PasswordProtection;


import java.util.List;


import app.firmador.gui.GUIInterface;
import com.google.common.base.Throwables;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.SignaturePackaging;

import eu.europa.esig.dss.asic.xades.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.asic.xades.signature.ASiCWithXAdESService;


import eu.europa.esig.dss.service.tsp.OnlineTSPSource;

import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.validation.CertificateVerifier;


public class FirmadorOpenDocument extends CRSigner {



    public FirmadorOpenDocument(GUIInterface gui) {
        super(gui);
    }







    public DSSDocument sign(DSSDocument toSignDocument,
        PasswordProtection pin) {



        CertificateVerifier verifier = this.getCertificateVerifier();
        verifier.setCheckRevocationForUntrustedChains(true);

        ASiCWithXAdESService service = new ASiCWithXAdESService(verifier);

        ASiCWithXAdESSignatureParameters parameters =
            new ASiCWithXAdESSignatureParameters();
        SignatureValue signatureValue = null;

        DSSDocument signedDocument = null;

        try {
            SignatureTokenConnection token = getSignatureConnection(pin);
            DSSPrivateKeyEntry privateKey = getPrivateKey(token);
            CertificateToken certificate = privateKey.getCertificate();
            parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
            parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_LT);
            parameters.setSignaturePackaging(SignaturePackaging.ENVELOPED);
            parameters.setSigningCertificate(certificate);
            parameters.setSignWithExpiredCertificate(true);
            //parameters.setPrettyPrint(true);
            List<CertificateToken> certificateChain = getCertificateChain(
                verifier, parameters);
            parameters.setCertificateChain(certificateChain);
            parameters.setEn319132(false);
            OnlineTSPSource onlineTSPSource = new OnlineTSPSource(TSA_URL);
            service.setTspSource(onlineTSPSource);






























            ToBeSigned dataToSign = service.getDataToSign(toSignDocument,
                parameters);

            signatureValue = token.sign(dataToSign,
                parameters.getDigestAlgorithm(), privateKey);
        } catch (DSSException|Error e) {
            gui.showError(Throwables.getRootCause(e));
        }

        try {
            signedDocument = service.signDocument(toSignDocument, parameters,
                signatureValue);
        } catch (Exception e) {
            e.printStackTrace();
            gui.showMessage(
                "Aviso: no se ha podido agregar el sello de tiempo y la " +
                "información de revocación porque es posible\n" +
                "que haya problemas de conexión con los servidores del " +
                "sistema de Firma Digital.\n" +
                "Detalle del error: " + Throwables.getRootCause(e) + "\n" +
                "\n" +
                "Se ha agregado una firma básica solamente. No obstante, si " +
                "el sello de tiempo resultara importante\n" +
                "para este documento, debería agregarse lo antes posible " +
                "antes de enviarlo al destinatario.\n" +
                "\n" +
                "Si lo prefiere, puede cancelar el guardado del documento " +
                "firmado e intentar firmarlo más tarde.\n");

            parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_B);
            try {
                signedDocument = service.signDocument(toSignDocument,
                    parameters, signatureValue);
            } catch (Exception ex) {
                e.printStackTrace();
                gui.showError(Throwables.getRootCause(e));
            }
        }

        return signedDocument;
    }

    public DSSDocument extend(DSSDocument document) {
        ASiCWithXAdESSignatureParameters parameters =
            new ASiCWithXAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_LTA);
        //parameters.setPrettyPrint(true);
        parameters.aSiC().setContainerType(ASiCContainerType.ASiC_E);
        CertificateVerifier verifier = this.getCertificateVerifier();
        verifier.setCheckRevocationForUntrustedChains(true);

        ASiCWithXAdESService service = new ASiCWithXAdESService(verifier);

        OnlineTSPSource onlineTSPSource = new OnlineTSPSource(TSA_URL);
        service.setTspSource(onlineTSPSource);

        DSSDocument extendedDocument = null;
        try {
            extendedDocument = service.extendDocument(document,
                parameters);
        } catch (Exception e) {
            e.printStackTrace();
            gui.showMessage(
                "Aviso: no se ha podido agregar el sello de tiempo y la " +
                "información de revocación porque es posible\n" +
                "que haya problemas de conexión con los servidores del " +
                "sistema de Firma Digital.\n" +
                "Detalle del error: " + Throwables.getRootCause(e) + "\n" +
                "\n" +
                "Inténtelo de nuevo más tarde. Si el problema persiste, " +
                "compruebe su conexión a Internet o verifique\n" +
                "que no se trata de un problema de los servidores de Firma " +
                "Digital o de un error de este programa.\n");
        }

        return extendedDocument;
    }

}
