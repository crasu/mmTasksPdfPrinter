## Getting started

1.  Install sbt http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html
2.  clone git repo
3.  mmTasksPdfPrinter$ sbt test
4.  mmTasksPdfPrinter$ sbt package

## Running it locally 
Use 'sbt' and then 'container:start' to start Jetty.
Use 'container:reload /' to load changes.
Jetty should be reachable on Port 8080.

## Continuous Testing 
Using a '~ ' before your command will execute it whenever you change/save the sources.
Run 'sbt' and execute the following command:
* ~ test
* ~ run main --file <.mm-FilepathWithoutSpaces> --sprint <SprintnameWithoutSpaces> --pdf <.pdf-FilepathToPutTheGeneratedPdf>

Hint: You can start multiple instances of sbt!

