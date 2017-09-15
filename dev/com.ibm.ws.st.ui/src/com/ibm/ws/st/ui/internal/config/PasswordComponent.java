/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.crypto.CustomPasswordEncryptionInfo;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * A utility component for entering passwords.
 * Assumes the component has 2 columns by default.
 */
public class PasswordComponent {
    private static final char PASSWORD_CHAR = '\u25CF';

    protected static String ENCODING_XOR = "xor";
    protected static String ENCODING_AES = "aes";
    protected static String ENCODING_HASH = "hash";
    protected List<String> encodingMethodList = new ArrayList<String>();
    protected Map<String, CustomPasswordEncryptionInfo> customEncryptionMap = null;
    protected boolean supportsHash = false;
    protected WebSphereRuntime wsRuntime;

    protected String password = "";
    protected String encodingMethod;
    protected String key = "";
    protected Text passwordText;
    protected ModifyListener listener;

    /**
     * Create a password component for all encoding/encrypting methods. Uses a default label and assumes 2 columns.
     *
     * @param composite
     * @param wsRuntime
     * @param supportsHash
     */

    public PasswordComponent(Composite composite, WebSphereRuntime wsRuntime, boolean supportsHash) {
        this(composite, null, 2, wsRuntime, supportsHash, null);
    }

    /**
     * Create a password component for all encoding/encrypting methods. Uses a default label and assumes 2 columns.
     *
     * @param composite
     * @param wsRuntime
     * @param supportsHash
     * @param customEncryptionList list of CustomPasswordEncryptionInfo. null is acceptable.
     */
    public PasswordComponent(Composite composite, WebSphereRuntime wsRuntime, boolean supportsHash, Map<String, CustomPasswordEncryptionInfo> customEncryptionMap) {
        this(composite, null, 2, wsRuntime, supportsHash, customEncryptionMap);
    }

    /**
     * Create a password component for all encoding/encrypting methods.
     *
     * @param composite
     * @param label
     * @param numColumns
     * @param wsRuntime
     * @param supportsHash
     */
    public PasswordComponent(Composite composite, String passwordLabel, int numColumns, WebSphereRuntime wsRuntime, boolean supportsHash) {
        this(composite, passwordLabel, numColumns, wsRuntime, supportsHash, null);
    }

    /**
     * Create a password component for all encoding/encrypting methods.
     *
     * @param composite
     * @param label
     * @param numColumns
     * @param wsRuntime
     * @param supportsHash
     * @param customEncryptionList list of CustomPasswordEncryptionInfo. null is acceptable.
     */
    public PasswordComponent(Composite composite, String passwordLabel, int numColumns, WebSphereRuntime wsRuntime, boolean supportsHash,
                             Map<String, CustomPasswordEncryptionInfo> customEncryptionMap) {
        this.wsRuntime = wsRuntime;
        this.supportsHash = supportsHash;
        this.customEncryptionMap = customEncryptionMap;

        if (numColumns < 2)
            throw new IllegalArgumentException("Invalid number of columns");

        initializeEncodingMethodArray(customEncryptionMap);
        createControl(composite, passwordLabel, numColumns);
    }

    /**
     * Compose the list of encoding methods.
     *
     */
    private void initializeEncodingMethodArray(Map<String, CustomPasswordEncryptionInfo> customEncryptionMap) {
        encodingMethodList.add(ENCODING_XOR);
        // check whether aes needs to be included.
        if (!(wsRuntime != null && wsRuntime.getRuntimeVersion().startsWith("8.5.0"))) {
            encodingMethodList.add(ENCODING_AES);
        }
        // check whether hash needs to be included.
        if (supportsHash) {
            encodingMethodList.add(ENCODING_HASH);
        }
        // check custom encryption.
        if (customEncryptionMap != null) {
            encodingMethodList.addAll(customEncryptionMap.keySet());
        }
    }

    private void createControl(Composite composite, String passwordLabel, int numColumns) {
        Label label = new Label(composite, SWT.NONE);
        if (passwordLabel == null)
            label.setText(Messages.password);
        else
            label.setText(passwordLabel);
        GridData data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        label.setLayoutData(data);

        Composite comp = new Composite(composite, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 5;
        layout.numColumns = 2;
        comp.setLayout(layout);
        comp.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, numColumns - 1, 1));

        passwordText = new Text(comp, SWT.NONE | SWT.BORDER);
        data = new GridData(GridData.FILL, GridData.CENTER, true, false);
        passwordText.setLayoutData(data);
        passwordText.setEchoChar(PASSWORD_CHAR);

        final Button showPassword = new Button(comp, SWT.CHECK);
        showPassword.setText(Messages.passwordShow);
        data = new GridData(GridData.END, GridData.CENTER, false, false);
        showPassword.setLayoutData(data);
        showPassword.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (showPassword.getSelection())
                    passwordText.setEchoChar('\0');
                else
                    passwordText.setEchoChar(PASSWORD_CHAR);
            }
        });

        final Label encodingLabel = new Label(composite, SWT.NONE);
        encodingLabel.setText(Messages.passwordEncoding);
        data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        encodingLabel.setLayoutData(data);

        final Combo encodingCombo = new Combo(composite, SWT.READ_ONLY);
        List<String> methods = createEncodingSelectionList();

        encodingCombo.setItems(methods.toArray(new String[methods.size()]));
        data = new GridData(GridData.FILL, GridData.CENTER, true, false, numColumns - 1, 1);
        encodingCombo.setLayoutData(data);

        final Label keyLabel = new Label(composite, SWT.NONE);
        keyLabel.setText(Messages.passwordKey);
        data = new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false);
        keyLabel.setLayoutData(data);

        final Text keyText = new Text(composite, SWT.PASSWORD | SWT.BORDER);
        keyText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, numColumns - 1, 1));
        keyText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                key = keyText.getText();
            }
        });

        encodingCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                int sel = encodingCombo.getSelectionIndex();
                encodingMethod = encodingMethodList.get(sel);
                boolean isAES = ENCODING_AES.equals(encodingMethod);
                keyLabel.setEnabled(isAES);
                if (isAES) {
                    keyText.setEnabled(true);
                } else {
                    keyText.setEnabled(false);
                }
            }
        });

        passwordText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                password = passwordText.getText();
                if (listener != null)
                    listener.modifyText(event);
            }
        });

        encodingCombo.select(0);
        encodingMethod = ENCODING_XOR;
        if (wsRuntime != null && wsRuntime.getRuntimeVersion().startsWith("8.5.0")) {
            encodingLabel.setEnabled(false);
            encodingCombo.setEnabled(false);
        }
        keyLabel.setEnabled(false);
        keyText.setEnabled(false);

    }

    /**
     * Create a list of encoding methods for the dropdown list from the list of supported
     * encoding method. If there are custom encoding, then lookup the description of custom encoding
     * and format it as <encoding name> (<description>) which is the same format of the predefined encoding method.
     */
    private List<String> createEncodingSelectionList() {
        List<String> methodsList = new ArrayList<String>();
        for (String method : encodingMethodList) {
            if (ENCODING_XOR.equals(method)) {
                methodsList.add(Messages.passwordXOR);
            } else if (ENCODING_AES.equals(method)) {
                methodsList.add(Messages.passwordAES);
            } else if (ENCODING_HASH.equals(method)) {
                methodsList.add(Messages.passwordHash);
            } else {
                // the custom encryption.
                StringBuffer sb = new StringBuffer(method);
                String description = getCustomEncryptionDescription(method);
                if (description != null && description.length() > 0) {
                    sb.append(" ").append(NLS.bind(Messages.passwordExplanation, getCustomEncryptionDescription(method)));
                }
                methodsList.add(sb.toString());
            }
        }
        return methodsList;
    }

    /**
     * Returns the description which corresponds the given method (algorithm).
     *
     */
    private String getCustomEncryptionDescription(String method) {
        String output = null;
        if (customEncryptionMap != null) {
            CustomPasswordEncryptionInfo info = customEncryptionMap.get(method);
            if (info != null) {
                output = info.getDescription();
            }
        }
        return output;
    }

    /**
     * Add a modify listener. Only a single listener is supported for now.
     *
     * @param listener
     */
    public void addModifyListener(ModifyListener listener) {
        this.listener = listener;
    }

    public String getPassword() {
        return password;
    }

    public String getPasswordEncoding() {
        return encodingMethod;
    }

    public String getPasswordKey() {
        return key;
    }

    public IStatus validate() {
        if (password != null && !password.isEmpty())
            return Status.OK_STATUS;

        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, null);
    }

    public String getEncodedPassword(IProgressMonitor monitor) throws CoreException {
        if (encodingMethod != null) {
            if (ENCODING_XOR.equals(encodingMethod)) {
                return wsRuntime.encodePassword(password, monitor);
            } else if (ENCODING_AES.equals(encodingMethod)) {
                return wsRuntime.encryptPassword("aes", key, password, monitor);
            } else {
                return wsRuntime.encryptPassword(encodingMethod, null, password, monitor);
            }
        }
        return null;
    }

    public boolean setFocus() {
        return passwordText.setFocus();
    }
}
