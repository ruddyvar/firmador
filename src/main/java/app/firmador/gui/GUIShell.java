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

package app.firmador.gui;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.security.KeyStore.PasswordProtection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import app.firmador.FirmadorPAdES;
//import app.firmador.FirmadorXAdES;
import com.google.common.base.Throwables;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;

public class GUIShell implements GUIInterface {

    public void loadGUI() {
        String fileName = getDocumentToSign();

        if (fileName != null) {
            // FirmadorXAdES firmador = new FirmadorXAdES(this);
            FirmadorPAdES firmador = new FirmadorPAdES(this);
            firmador.selectSlot();

            PasswordProtection pin = getPin();
            DSSDocument toSignDocument = new FileDocument(fileName);
            DSSDocument signedDocument = firmador.sign(toSignDocument, pin);
            try {
                pin.destroy();
            } catch (Exception e) {}

            if (signedDocument != null) {
                fileName = getPathToSave();
                try {
                    signedDocument.save(fileName);
                    showMessage(
                        "Documento guardado satisfactoriamente en \n" +
                        fileName);
                } catch (IOException e) {
                    showError(Throwables.getRootCause(e));
                }
            }
        }
    }

    public void setArgs(String[] args) {
        List<String> arguments = new ArrayList<String>();
        for (String params : args) {
            if (!params.startsWith("-")) {
                arguments.add(params);
            }
        }
        if (arguments.size() > 1) {
            Paths.get(arguments.get(0)).toAbsolutePath().toString();
        }
        if (arguments.size() > 2) {
            Paths.get(arguments.get(1)).toAbsolutePath().toString();
        }
    }

    public void showError(Throwable error) {
        System.err.println("Exception: " + error.getClass().getName());
        System.err.println("Message: " + error.getLocalizedMessage());
        System.exit(1);
    }

    private String readFromInput(String message) {
        System.out.println(message);
        String plaintext = null;
        BufferedReader br =
            new BufferedReader(new InputStreamReader(System.in));
        try {
            plaintext = br.readLine();

        } catch (IOException e) {
            System.err.println("I can't read in stdin");
            System.exit(1);
        }

        return plaintext;
    }

    public String getDocumentToSign() {
        String docpath = readFromInput("Path del documento a firmar: ");

        return Paths.get(docpath).toAbsolutePath().toString();
    }

    public String getPathToSave() {
        String docpath = readFromInput("Path del documento a guardar: ");

        return Paths.get(docpath).toAbsolutePath().toString();
    }

    public PasswordProtection getPin() {
        Console console = System.console();
        char[] password = null;

        if (console != null) {
            password = console.readPassword("PIN: ");
        } else {
            password = readFromInput("PIN: ").toCharArray();
        }
        PasswordProtection pin = new PasswordProtection(password);
        Arrays.fill(password, (char) 0);

        return pin;
    }

    @Override
    public void showMessage(String message) {
         System.out.println(message);
    }

    @Override
    public int getSelection(String[] options) {
        int dev = -1;
        String selected;

        while(dev == -1) {
            for(int x = 0; x < options.length; x++) {
                System.out.println(x + ") " + options[x]);
            }
            selected = readFromInput("Opción a seleccionar: ");
            try {
                dev = Integer.parseInt(selected);
                if(dev >= options.length){
                    System.err.println("Opción invalida debe ser menor a "
                        + options.length);
                    dev = -1;
                }
            } catch (Exception e) {
                dev = -1;
                System.err.println("Debe ingresar un número");
            }
        }

        return 0;
    }

}
