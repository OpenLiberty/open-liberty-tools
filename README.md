# open-liberty-tools

[![Twitter](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/OpenLibertyIO)
![Linux](https://img.shields.io/badge/os-linux-green.svg?style=flat)
[![License](https://img.shields.io/badge/License-EPL%201.0-green.svg)](https://opensource.org/licenses/EPL-1.0)

# Summary
Open Liberty Tools are (classic) lightweight tools for developing, assembling, and deploying apps to [Open Liberty](https://github.com/OpenLiberty/open-liberty) in Eclipse IDE.

Alternatively, try out [Liberty Tools](https://marketplace.eclipse.org/content/liberty-tools), our next-generation open source development tools for Liberty. You can also help shape the future of these tools by providing feedback or contributing through this [GitHub repository](https://github.com/OpenLiberty/liberty-tools-eclipse).

# Table of Contents
* [Prereqs](https://github.com/OpenLiberty/open-liberty-tools#prereqs)
* [Known Issues](https://github.com/OpenLiberty/open-liberty-tools#known-issues)
* [Getting Started](https://github.com/OpenLiberty/open-liberty-tools#getting-started)
* [Contribute to Open Liberty Tools](https://github.com/OpenLiberty/open-liberty-tools#contribute-to-open-liberty-tools)
* [Community](https://github.com/OpenLiberty/open-liberty-tools#community)

## Prereqs
Java 11 is now required as of version 21.0.0.3 of the tools.

The 21.0.0.3 release supports Eclipse versions [2021-03](https://www.eclipse.org/downloads/packages/release/2021-03/r/eclipse-ide-enterprise-java-and-web-developers), [2020-12](https://www.eclipse.org/downloads/packages/release/2020-12/r/eclipse-ide-enterprise-java-developers) and [2020-09](https://www.eclipse.org/downloads/packages/release/2020-09/r/eclipse-ide-enterprise-java-developers).

## Known Issues
Please see the [Liberty Tools known issues](https://github.com/OpenLiberty/open-liberty-tools/wiki/Liberty-Tools-known-issues) page for known issues and workarounds.

## Getting Started 
To install the Open Liberty Tools and other WebSphere Developer Tools features:
1. If you don’t already have Eclipse, install [Eclipse 2021-03 for Enterprise Java and Web Developers ( 4.19 )](https://www.eclipse.org/downloads/packages/release/2021-03/r/eclipse-ide-enterprise-java-and-web-developers)
2. Download Open Liberty tools by going to the [Open Liberty Tools list of repositories](https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/tools/release/?C=N;O=D), choosing a folder that is close in time to your Open Liberty release date and downloading the openlibertytools-*.zip file therein. 
3. Start your Eclipse workbench.
4. Start the installation using the following method.
    * Locate the installation files from your Eclipse workbench:
      1. Click **Help** > **Install New Software.**
      2. Click **Add**, click **Archive.**
      3. Select the archive downloaded in step 2 and click **Open**. Give your repository a name and click **Add**.
5. In the list of available software, expand the parent nodes and select the **WebSphere Application Server Liberty Tools** feature and any other feature that are needed. When you are finished, click **Confirm**.
6. On the **Review Licenses** page, review the license text. If you agree to the terms, click **I accept the terms of the license agreements** and then click **Finish**. The installation process starts.
   
```
  Note:
      During the installation, a Security Warning dialog box might open and display the following message:
      Warning: You are installing software that contains unsigned content. The authenticity or validity of this software cannot be established. Do you want to continue with the installation? 
      You can safely ignore the message and click 'install anyway' to continue.
```

6. When the installation process completes, restart the workbench.

To install the Open Liberty Tools only from Open Liberty site, follow [this instruction](https://github.com/OpenLiberty/open-liberty-tools/blob/main/INSTALL_OPENLIBERTYTOOLS.md).

Visit the [WASdev Community](https://developer.ibm.com/wasdev/) for documentation and tutorials. [Here](https://developer.ibm.com/wasdev/docs/category/tools/) are a few related to the tools:

## Contribute to Open Liberty Tools
1. Clone the repository to your system.

    ```git clone https://github.com/OpenLiberty/open-liberty-tools```
    
2. Run a gradle build.

    ```cd open-liberty-tools/dev```
    
    ```./gradlew```
 
3. Go [Open issues](https://github.com/OpenLiberty/open-liberty-tools/issues), [Review existing contributions](https://github.com/OpenLiberty/open-liberty-tools/pulls), or [Submit fixes](https://github.com/OpenLiberty/open-liberty-tools/blob/main/CONTRIBUTING.md).

## Community
1. [Open Liberty group.io](https://groups.io/g/openliberty)
2. [OpenLibertyIO on Twitter](https://twitter.com/OpenLibertyIO)
3. [ibm-wdt tag on stackoverflow](https://stackoverflow.com/questions/tagged/ibm-wdt)

