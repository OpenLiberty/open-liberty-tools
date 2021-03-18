# open-liberty-tools

[![Twitter](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/OpenLibertyIO)
![Linux](https://img.shields.io/badge/os-linux-green.svg?style=flat)
[![License](https://img.shields.io/badge/License-EPL%201.0-green.svg)](https://opensource.org/licenses/EPL-1.0)

# Summary
Open Liberty Tools are lightweight tools for developing, assembling, and deploying apps to [Open Liberty](https://github.com/OpenLiberty/open-liberty).

# Table of Contents
* [Getting Started](https://github.com/OpenLiberty/open-liberty-tools#getting-started)
* [Contribute to Open Liberty Tools](https://github.com/OpenLiberty/open-liberty-tools#contribute-to-open-liberty-tools)
* [Community](https://github.com/OpenLiberty/open-liberty-tools#community)

## Getting Started 
To install the Open Liberty Tools and other WebSphere Developer Tools features:
1. If you don’t already have Eclipse, install [Eclipse Oxygen for Java EE Developers ( 4.7 )](https://www.eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/oxygen1a)
2. Start your Eclipse workbench.
3. Start the installation using either of the following methods.
    * Locate the installation files from your Eclipse workbench:
      1. Click **Help** > **Eclipse Marketplace.**
      2. In the Find field, type Liberty.
      3. In the list of results, locate **IBM Liberty Developer Tools for Eclipse Oxygen** (and later) and then click **Install**. The Confirm Selected Features page opens.
    * Drag an Install icon from a web page to your Eclipse workbench:
      1. Click [download Liberty page](https://developer.ibm.com/wasdev/downloads/)
      2. Select **IBM Liberty Developer Tools for Eclipse Oxygen** (and later) from the asset list.
      3. Locate the **Install** icon for **WebSphere Application Server Liberty**. 
      4. Select and drag the **Install** icon to your Eclipse workbench, and drop it on the Eclipse toolbar. The **Confirm Selected Features page** opens.
4. On the **Confirm Selected Features** page, expand the parent nodes and select the **WebSphere Application Server Liberty Tools** feature and any other feature that are needed. When you are finished, click **Confirm**.
5. On the **Review Licenses** page, review the license text. If you agree to the terms, click **I accept the terms of the license agreements** and then click **Finish**. The installation process starts.
   
```
  Note:
      During the installation, a Security Warning dialog box might open and display the following message:
      Warning: You are installing software that contains unsigned content. The authenticity or validity of this software cannot be established. Do you want to continue with the installation? 
      You can safely ignore the message and click 'install anyway' to continue.
```

6. When the installation process completes, restart the workbench.

To install the Open Liberty Tools only from Open Liberty site, follow [this instruction](https://github.com/OpenLiberty/open-liberty-tools/blob/master/INSTALL_OPENLIBERTYTOOLS.md).

Visit the [WASdev Community](https://developer.ibm.com/wasdev/) for documentation and tutorials. [Here](https://developer.ibm.com/wasdev/docs/category/tools/) are a few related to the tools:

## Contribute to Open Liberty Tools
1. Clone the repository to your system.

    ```git clone https://github.com/OpenLiberty/open-liberty-tools```
    
2. Run a gradle build.

    ```cd open-liberty-tools/dev```
    
    ```./gradlew```
 
3. Go [Open issues](https://github.com/OpenLiberty/open-liberty-tools/issues), [Review existing contributions](https://github.com/OpenLiberty/open-liberty-tools/pulls), or [Submit fixes](https://github.com/OpenLiberty/open-liberty-tools/blob/master/CONTRIBUTING.md).

## Community
1. [Open Liberty group.io](https://groups.io/g/openliberty)
2. [OpenLibertyIO on Twitter](https://twitter.com/OpenLibertyIO)
3. [ibm-wdt tag on stackoverflow](https://stackoverflow.com/questions/tagged/ibm-wdt)

