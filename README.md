# Ticketing Integration Microservice for Cumulocity IoT

This microservice allows integration of any ticketing platform with Cumulocity IoT. It supports integration with webMethods AgileApps out-of-the-box. However, for integration with other platforms like ServiceNow, ZenDesk, etc. it relies on other integration platforms like webMethods.io.

### Features
1. View tickets in Cumulocity IoT dashboards.
2. Create tickets automatically based on configured device and alarm type mappings.
3. Create tickets manually based on specific alarms.

### Installation
1. Download the latest `ticketing-{version}.zip` file from the Releases section.
2. Switch to Cumulocity IoT `Administration` app using App Switcher. Upload the downloaded microservice zip file. Steps vary based on Cumulocity IoT version you are using.
3. Subscribe the microservice. Steps vary based on Cumulocity IoT version you are using.
4. Refresh the page and click on `Logs` tab to view the microservice logs and if there any potential errors.

### Configuration - to integrate Cumulocity IoT with Ticketing Platform
1. webMethods AgileApps
	1. Ensure you have AgileApps tenant reachable from Cumulocity IoT tenant.
	2. Create a AgileApps user account that has read and write access to `Cases` object in `Service Desk` application. Tickets will be created as records in `Cases` object.
	3. Create a record in `Accounts` object and get it's `Record id`. Case records will be linked to this Accounts record.
2. Other ticketing platform
	1. Microservice expects access to three APIs to integrate with a Ticketing platform using Basic Authentication.
		1. Get tickets - To get tickets from the Ticketing platform.
		2. Get ticket comments - To get comments related to a ticket from the Ticketing platform.
		3. Create ticket - To create a new ticket in the Ticketing platform.
	2. Three APIs need to be as per the API definition in the swagger file https://raw.githubusercontent.com/SoftwareAG/c8y-ticketing-integration-setup-widget/master/src/c8y-ticketing-integration-setup-widget/assets/apis-swagger.yaml.
	3. Three APIs can be developed and made accessible using any platform including wM.io which provides out-of-the-box connectors for almost all ticketing platforms.
Once above requirements are met, download the `Ticketing Integration Setup Widget` https://github.com/SoftwareAG/c8y-ticketing-integration-setup-widget and refer to widget documentation.

### Development - to do the enhancements and testing locally
1. Refer to https://cumulocity.com/guides/microservice-sdk/java/#create-the-application and create a proxy microservice application. Provide name as 'ticketing-dev' and key as 'ticketing-dev-key'.
2. Refer to https://cumulocity.com/guides/microservice-sdk/java/#acquire-the-microservice-bootstrap-user and get the bootstrap user credentials.
3. I use `Spring Tool Suite 4.4.14.1` as an IDE.
4. Clone the Git repository on local machine using `git clone https://github.com/SoftwareAG/c8y-ticketing-integration-microservice.git`.
5. Uncomment the `application.name`, `C8Y.bootstrap.register`, `C8Y.bootstrap.tenant`, `C8Y.baseURL`, `C8Y.bootstrap.user`, `C8Y.bootstrap.password`, `C8Y.microservice.isolation` properties in the `src/main/resources/application.propeties` file.
6. Provide values for `C8Y.bootstrap.tenant`, `C8Y.baseURL`, `C8Y.bootstrap.user`, `C8Y.bootstrap.password` properties in the `src/main/resources/application.propeties` file.
7. Update `microservice.name` to `ticketing-dev` in pom.xml file.
8. You may need to install maven dependencies using `Run > Maven install` and/ or `Maven > Update project`.
9. Run the project as Spring Boot App. Microservice REST endpoints will be accessible at http://localhost/{endpoint-name}.
10. (Optional) push the changes back to the repository after proper testing.

### Build - to create a new build for the microservice
1. Finish the development and testing on your local machine. Change the `version` in pom.xml file, if required.
2. Make sure you have docker installed. I have Docker version `20.10.14, build a224086` running on my windows machine.
3. Run `Maven clean`. Then `Maven install` to create the build.
4. Use `ticketing-{version}.zip` file in the target folder as a distribution.

------------------------------

These tools are provided as-is and without warranty or support. They do not constitute part of the Software AG product suite. Users are free to use, fork and modify them, subject to the license agreement. While Software AG welcomes contributions, we cannot guarantee to include every contribution in the master project.

------------------------------

For more information you can Ask a Question in the [TECHcommunity Forums](http://tech.forums.softwareag.com/techjforum/forums/list.page?product=cumulocity).
  
  
You can find additional information in the [Software AG TECHcommunity](http://techcommunity.softwareag.com/home/-/product/name/cumulocity).