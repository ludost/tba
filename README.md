Test case for TBA, Ludo Stellingwerff.

#Requirements for running
A jetty setup or maven jetty plugin (latter is preferred)

To run from commandline:

in tba/gui:  mvn jetty:run
in tba/dist: java -jar vehicle.jar vehicle.yaml

Open in webbrowser:
http://localhost:8080/gui/gui.html     (test case specific webinterface)
http://localhost:8081/agents/manager   (accessing the background agents)
