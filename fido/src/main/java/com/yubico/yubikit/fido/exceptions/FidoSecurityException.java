/*
 * Copyright (C) 2019 Yubico.
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

package com.yubico.yubikit.fido.exceptions;

/**
 * Instance of security exception for Fido
 * Might occur if auth server is not aware of client requesting authentication
 */
public class FidoSecurityException extends FidoException {
    static final long serialVersionUID = 1L;

    public FidoSecurityException(String message) {
        super(message);
    }
}
