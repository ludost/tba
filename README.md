Test case for TBA, Ludo Stellingwerff.

#Requirements for running, either one of:
- A jetty setup 
- maven + jetty plugin (this is preferred)

To run from commandline with maven jetty plugin:
- in tba/gui:  mvn jetty:run
- in tba/dist: java -jar vehicle.jar vehicle.yaml

Alternatively you can place the dist/gui.war file in some other jetty/tomcat servlet container.

Open in webbrowser:
- http://localhost:8080/gui/gui.html     (test case specific webinterface, if running in different container, please adjust the port number)
- http://localhost:8081/agents/manager   (accessing the background agents)

Tested in Google Chrome only.
