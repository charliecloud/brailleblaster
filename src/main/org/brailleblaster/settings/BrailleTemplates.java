/* BrailleBlaster Braille Transcription Application
  *
  * Copyright (C) 2014
* American Printing House for the Blind, Inc. www.aph.org
* and
  * ViewPlus Technologies, Inc. www.viewplus.com
  * and
  * Abilitiessoft, Inc. www.abilitiessoft.com
  * and
  * American Printing House for the Blind, Inc. www.aph.org www.aph.org
  *
  * All rights reserved
  *
  * This file may contain code borrowed from files produced by various 
  * Java development teams. These are gratefully acknowledged.
  *
  * This file is free software; you can redistribute it and/or modify it
  * under the terms of the Apache 2.0 License, as given at
  * http://www.apache.org/licenses/
  *
  * This file is distributed in the hope that it will be useful, but
  * WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE
  * See the Apache 2.0 License for more details.
  *
  * You should have received a copy of the Apache 2.0 License along with 
  * this program; see the file LICENSE.
  * If not, see
  * http://www.apache.org/licenses/
  *
  * Maintained by Keith Creasy <kcreasy@aph.org>, Project Manager
*/

package org.brailleblaster.settings;

/**
 * This class provides the methods for dealing with liblouisutdml 
 * configuration files. The user can chose a file to use, edit one, copy 
 * or create. Any new or modified files are stored in the 
 * liblouisutdml/lbu_files directory of the userProgramData directory, 
 * not in the built-in programData directory.
 */
public class BrailleTemplates {

String fileToEdit = null;

/**
 * Call the method that lists configuration files and then enables the 
 * user to pick one to use or to edit. Then if the latter choice has 
 * been made, call a method to do the editing. Files are sought first in 
 * the liblouisutdml/lbu_files of the userProgramData directory and then 
 * in the programData directory.
 */
public BrailleTemplates () {
showConfigFileList();
if (fileToEdit != null) {
editConfigFile(fileToEdit);
}
}

/**
 * List the files with the extension .cfg in liblouisutdml/lbu_files 
 * looking first in userProgramData and then in programData. Enable the 
 * user to pick one to use or edit.
 */
private void showConfigFileList() {
}

/**
 * Edit a configuration file. When finished store it in the 
 * liblouisutdml/lbu_files directory of the userProgramData directory. 
 * If a style is to be edited call the editStyle method.
 */
private void editConfigFile (String fileName) {
if (fileName == null) return;
}

 /**
 * Edit a style in a configuration file.
 */
private void editStyle (int lineNumber) {
}

}
