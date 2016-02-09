This file gives general instructions and explains the structure of this 
directory, which is also the structure of BrailleBlaster. It also gives 
brief information on how BrailleBlaster operates.

The files COPYRIGHT.txt and LICENSE.txt contain the information which 
their names imply. BrailleBlaster is copyrighted by ViewPlus 
Technologies, Inc. and Abilitiessoft, Inc. It is licensed under the 
Apache license, version 2.0.

For instructions on building BrailleBlaster see BUILD.txt the 
brailleblaster.jar
file will be placed in the dist directory. Note that you must compile 
the software listed in BUILD.txt as going into the native subdirectory 
to complete the build.
You must also place the jar files listed in BUILD.txt in the dist/lib 
subdirectory. Building from the repository is recommended. Note, 
however, that the repository does not contain any  binary files. Once 
you have provided these you can simply pull the latest changes from the 
Mercurial repoository and run Ant in this directory.

Besides the various text files, the top-level directory also contains 
the dist and src directories.

The dist directory contains what will be distributed to the user. Note 
the file contributors-credit.txt. the getting-started.txt basically 
points to the helpDocs directory, which contains The real 
getting-started file and other documents which can be read with either a 
text edditor or a browser. The programData subdirectory contains 
liblouis tables and liblouisutdml configuration and semantic-action 
files. The lib subdirectory helds jar files needed by BrailleBlaster. 
The native subdirectory contains C libraries and applications.

The src directory is loosely patterned on the src directories of Apache 
projects. It contains the subdirectories main, resources and test. The 
main subdirectory contains the directory tree for BrailleBlaster source 
and also some classes from the DAISY pipeline.
 
the resources directory contains various files useful in development or 
necessary for running. 

The test directory contains the directory tree for Junit tests. 
Currently there are no tests. They will be added as human resources 
allow.

The main directory contains the org directory which in turn contains the 
brailleblaster directory. There are four classes in this package and a 
number of subpackages. The first class is Main.java and takes care of 
starting and ending BrailleBlaster. It calls BBBIni.java to do general 
setup and fetch platform-dependent information. If there are no 
arguments it passes control directly to the word processor. If there are 
arguments it passes control to the class Subcommands.java. This class 
processes subcommands like translate, emboss, checktable, help, etc. 
There are arguments such as -nogui which proceed the actual subcommands. 
If the first argument following any "-" arguments is not a subcommand it 
is assumed to be a file to be opened and is passed to the word 
processor. The class ParseSubcommands is not used at present, but may be 
in the future.

The subpackages are in their own directories under org/brailleblaster. 
They are embossers, exporters, importers, localization, louisutdml, 
printers, settings and wordprocessor.

The most interesting of the subpackages is wordprocessor. The class in 
this package which initially receives control is WPManager.java. It 
takes care of getting things set up and co-ordinating word processing. 
DocumentManager.java will eventually handle multiple documents in an MDI 
environment. It co-ordinates BrailleView.java and DaisyView.java for 
each document.

The org.brailleblaster.louisutdml package might be better named. It 
contains classes used to mediate between BrailleBlaster and the 
liblouisutdml bindings. Problems are handled by 
LiblouisutdmlException.java in this package.

The embossers package contains drivers for various ebossers.

The printers package contains printer drivers where necessary and also 
classes used for preparing driver input.

The importers package handles reading various types of input and 
converting 
them into the wordprocessor's internal format.

The exporters package contains classes that convert the wordprocessors 
internal format to various file types.

The localization package contains classes and properties files used to 
make BrailleBlaster usable and friendly in various languages and 
countries.

The util package contains classes used in various parts of 
BrailleBlaster, such as ProgramCaller.java for calling other programs.

