# DbFit [![Build Status](https://travis-ci.org/crasu/mmTasksPdfPrinter.png?branch=master)](https://travis-ci.org/crasu/mmTasksPdfPrinter)

# Introduction 
mmTasksPdfPrinter helps you to print your Story and Task cards out of a Freemind
or Freeplane file. It is a Scala/Lift Servlet which parses a mm-file and creates
a PDF with your Story and Task cards.

## Contributing
See the [CONTRIBUTING file](CONTRIBUTING.md) for details on how to set up a test environment, contributing etc.

## Convention 
The tool assumes that you've applied these rules on your Mind Map:
* Sprints are on the first hierarchic level and match the pattern 'Sprint \d{4}*\d+' e. g. 'Sprint 2010*05'
* Stories are marked with an Excellent/Star icon
* There might be an arbitrary number of nodes between a Sprint and a Story
    and the text inside these nodes is added to the Story name
* Tasks are marked with an Look here/Paperclip icon
* There might be an arbitrary number of nodes between a Story and a Task
    and the text inside these nodes is used as Category of the Task
* There might be an arbitrary number of nodes after a Task
    and the text inside these nodes is treated as Subtask
* If Sprints, Stories or Tasks contain parenthesis (ether '(' and ')' or '{' and '}')
    with a number inside, the tool will assume that this is the number of Scrum Points
* The sequence of nodes determines the priority of a Sprint

Hint: Per default Alt+i is the hotkey for attaching icons to a node in Freemind

## Configuration 
The configuration is completely optional. The files are searched in the
current working directory during startup.

## printer.props 

An example of the configuration is located in confi _example.

## logo.png, logo.gif or logo.jpg 
This image will be used instead of the TNG Logo.

## Credits 
The idea behind this tool is based on the MM2CSV script created by Martin Kreidenweis.
