/*
 * Copyright (C) 2022 Yubico.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yubico.yubikit.testing.piv;

import static com.yubico.yubikit.testing.piv.PivJcaUtils.setupJca;
import static com.yubico.yubikit.testing.piv.PivJcaUtils.tearDownJca;
import static com.yubico.yubikit.testing.piv.PivTestConstants.DEFAULT_MANAGEMENT_KEY;
import static com.yubico.yubikit.testing.piv.PivTestConstants.DEFAULT_PIN;

import com.yubico.yubikit.core.Logger;
import com.yubico.yubikit.core.application.BadResponseException;
import com.yubico.yubikit.core.smartcard.ApduException;
import com.yubico.yubikit.piv.KeyType;
import com.yubico.yubikit.piv.ManagementKeyType;
import com.yubico.yubikit.piv.PinPolicy;
import com.yubico.yubikit.piv.PivSession;
import com.yubico.yubikit.piv.Slot;
import com.yubico.yubikit.piv.TouchPolicy;
import com.yubico.yubikit.piv.jca.PivAlgorithmParameterSpec;

import org.junit.Assert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.HashSet;
import java.util.Set;

public class PivJcaSigningTests {

    private static Set<String> signatureAlgorithmsWithPss = new HashSet<>();

    public static void testSign(PivSession piv) throws NoSuchAlgorithmException, IOException, ApduException, InvalidKeyException, BadResponseException, InvalidAlgorithmParameterException, SignatureException {
        setupJca(piv);
        for (KeyType keyType : KeyType.values()) {
            testSign(piv, keyType);
        }
        tearDownJca();
    }

    public static void testSign(PivSession piv, KeyType keyType) throws NoSuchAlgorithmException, IOException, ApduException, InvalidKeyException, BadResponseException, InvalidAlgorithmParameterException, SignatureException {
        piv.authenticate(ManagementKeyType.TDES, DEFAULT_MANAGEMENT_KEY);
        Logger.d("Generate key: " + keyType);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("YKPiv" + keyType.params.algorithm.name());
        kpg.initialize(new PivAlgorithmParameterSpec(Slot.SIGNATURE, keyType, PinPolicy.DEFAULT, TouchPolicy.DEFAULT, DEFAULT_PIN));
        KeyPair keyPair = kpg.generateKeyPair();

        signatureAlgorithmsWithPss = getAllSignatureAlgorithmsWithPSS();

        switch (keyType.params.algorithm) {
            case EC:
                testSign(keyPair, "SHA1withECDSA", null);
                testSign(keyPair, "SHA256withECDSA", null);
                //noinspection SpellCheckingInspection
                testSign(keyPair, "NONEwithECDSA", null);
                testSign(keyPair, "SHA3-256withECDSA", null);
                break;
            case RSA:
                testSign(keyPair, "SHA1withRSA", null);
                testSign(keyPair, "SHA256withRSA", null);

                String signatureAlgorithm = "SHA1WITHRSA/PSS";
                if (signatureAlgorithmsWithPss.contains(signatureAlgorithm)) {
                    testSign(keyPair, signatureAlgorithm, new PSSParameterSpec("SHA-1", "MGF1", MGF1ParameterSpec.SHA1, 8, 1));
                    PSSParameterSpec param = new PSSParameterSpec("SHA-1", "MGF1", MGF1ParameterSpec.SHA1, 0, 1);
                    byte[] sig1 = testSign(keyPair, signatureAlgorithm, param);
                    byte[] sig2 = testSign(keyPair, signatureAlgorithm, param);
                    Assert.assertArrayEquals("PSS parameters not used, signatures are not identical!", sig1, sig2);
                }

                try {
                    signatureAlgorithm = "SHA224WITHRSA/PSS";
                    if (signatureAlgorithmsWithPss.contains(signatureAlgorithm)) {
                        @SuppressWarnings("NewApi")
                        PSSParameterSpec saltedParam = new PSSParameterSpec("SHA-224", "MGF1", MGF1ParameterSpec.SHA224, 8, 1);
                        testSign(keyPair, signatureAlgorithm, saltedParam);

                        @SuppressWarnings("NewApi")
                        PSSParameterSpec param = new PSSParameterSpec("SHA-224", "MGF1", MGF1ParameterSpec.SHA224, 0, 1);
                        byte[] sig1 = testSign(keyPair, signatureAlgorithm, param);
                        byte[] sig2 = testSign(keyPair, signatureAlgorithm, param);
                        Assert.assertArrayEquals("PSS parameters not used, signatures are not identical!", sig1, sig2);
                    }
                } catch (NoSuchFieldError noSuchFieldError) {
                    // MGF1ParameterSpec.SHA224 is supported from Android API 26
                    Logger.d("Ignoring following error: " + noSuchFieldError.getMessage());
                }

                signatureAlgorithm = "SHA256WITHRSA/PSS";
                if (signatureAlgorithmsWithPss.contains(signatureAlgorithm)) {
                    testSign(keyPair, signatureAlgorithm, new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 8, 1));
                    PSSParameterSpec param = new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 0, 1);
                    byte[] sig1 = testSign(keyPair, signatureAlgorithm, param);
                    byte[] sig2 = testSign(keyPair, signatureAlgorithm, param);
                    Assert.assertArrayEquals("PSS parameters not used, signatures are not identical!", sig1, sig2);
                }

                signatureAlgorithm = "SHA384WITHRSA/PSS";
                if (signatureAlgorithmsWithPss.contains(signatureAlgorithm)) {
                    testSign(keyPair, signatureAlgorithm, new PSSParameterSpec("SHA-384", "MGF1", MGF1ParameterSpec.SHA384, 8, 1));
                    PSSParameterSpec param = new PSSParameterSpec("SHA-384", "MGF1", MGF1ParameterSpec.SHA384, 0, 1);
                    byte[] sig1 = testSign(keyPair, signatureAlgorithm, param);
                    byte[] sig2 = testSign(keyPair, signatureAlgorithm, param);
                    Assert.assertArrayEquals("PSS parameters not used, signatures are not identical!", sig1, sig2);
                }

                // RSA1024 is too small for SHA512WITHRSA/PSS
                if (keyType == KeyType.RSA2048) {
                    signatureAlgorithm = "SHA512WITHRSA/PSS";
                    if (signatureAlgorithmsWithPss.contains(signatureAlgorithm)) {
                        testSign(keyPair, signatureAlgorithm, new PSSParameterSpec("SHA-512", "MGF1", MGF1ParameterSpec.SHA512, 8, 1));
                        PSSParameterSpec param = new PSSParameterSpec("SHA-512", "MGF1", MGF1ParameterSpec.SHA512, 0, 1);
                        byte[] sig1 = testSign(keyPair, signatureAlgorithm, param);
                        byte[] sig2 = testSign(keyPair, signatureAlgorithm, param);
                        Assert.assertArrayEquals("PSS parameters not used, signatures are not identical!", sig1, sig2);
                    }
                }

                signatureAlgorithm = "RAWRSASSA-PSS";
                if (signatureAlgorithmsWithPss.contains(signatureAlgorithm)) {
                    testSign(keyPair, signatureAlgorithm, new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 8, 1));
                    PSSParameterSpec param = new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 0, 1);
                    byte[] sig1 = testSign(keyPair, signatureAlgorithm, param);
                    byte[] sig2 = testSign(keyPair, signatureAlgorithm, param);
                    Assert.assertArrayEquals("PSS parameters not used, signatures are not identical!", sig1, sig2);
                }

                signatureAlgorithm = "RSASSA-PSS";
                if (signatureAlgorithmsWithPss.contains(signatureAlgorithm)) {
                    testSign(keyPair, signatureAlgorithm, new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 8, 1));
                    PSSParameterSpec param = new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 0, 1);
                    byte[] sig1 = testSign(keyPair, signatureAlgorithm, param);
                    byte[] sig2 = testSign(keyPair, signatureAlgorithm, param);
                    Assert.assertArrayEquals("PSS parameters not used, signatures are not identical!", sig1, sig2);
                }

                break;
        }
    }

    public static byte[] testSign(KeyPair keyPair, String signatureAlgorithm, AlgorithmParameterSpec param) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, SignatureException {
        byte[] message = "Hello world!".getBytes(StandardCharsets.UTF_8);

        Logger.d("Create signature using " + signatureAlgorithm);
        Signature signer = Signature.getInstance(signatureAlgorithm);
        signer.initSign(keyPair.getPrivate());
        if (param != null) signer.setParameter(param);
        signer.update(message);
        byte[] signature = signer.sign();

        try {
            Signature verifier = Signature.getInstance(signatureAlgorithm);
            verifier.initVerify(keyPair.getPublic());
            if (param != null) verifier.setParameter(param);
            verifier.update(message);
            Assert.assertTrue("Verify signature", verifier.verify(signature));
            Logger.d("Signature verified for: " + signatureAlgorithm);
            return signature;
        } catch (InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    public static Set<String> getAllSignatureAlgorithmsWithPSS() {
        signatureAlgorithmsWithPss.clear();
        Set<String> pssSignatures = new HashSet<>();
        Provider provider = Security.getProvider("YKPiv");
        Set<Provider.Service> allServices = provider.getServices();
        for (Provider.Service service : allServices) {
            if (service.getType().equals("Signature") && service.getAlgorithm().endsWith("PSS")) {
                pssSignatures.add(service.getAlgorithm());
            }
        }

        return pssSignatures;
    }
}
