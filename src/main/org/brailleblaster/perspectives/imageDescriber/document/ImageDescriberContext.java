/* BrailleBlaster Braille Transcription Application
  *
  * Copyright (C) 2010, 2012
  * ViewPlus Technologies, Inc. www.viewplus.com
  * and
  * Abilitiessoft, Inc. www.abilitiessoft.com
  * All rights reserved
  *
  * This file may contain code borrowed from files produced by various 
  * Java development teams. These are gratefully acknoledged.
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
  * this program; see the file LICENSE.txt
  * If not, see
  * http://www.apache.org/licenses/
  *
  * Maintained by John J. Boyer john.boyer@abilitiessoft.com
*/

package org.brailleblaster.perspectives.imageDescriber.document;

import nu.xom.Attribute;
import nu.xom.Element;

public class ImageDescriberContext {
	
	// The document type.
	public static int ET_UNKNOWN = -1;
	public static int ET_NIMAS = 0;
	public static int ET_EPUB = 1;
	int documentType = ET_UNKNOWN;
	
	//////////////////////////////////////////////////////////////////////////////
	// Constructor. Inits variables.
	public ImageDescriberContext()
	{
		
	} // ImageDescriberContext()
	
	//////////////////////////////////////////////////////////////////////////////
	// Returns true if element has a parent <imggroup>, <fig>, etc.
	public boolean hasContainer(Element e)
	{
		// NIMAS.
		if( documentType == ET_NIMAS )
		{
			// Does this element have <imggroup> as a parent?
			if( ((nu.xom.Element)(e.getParent())).getLocalName().compareTo("imggroup") == 0 )
				return true;
			else
				return false;
		} // if( NIMAS...
		else if( documentType == ET_EPUB ) // EPUB
		{
			// Does this element have <imggroup> as a parent?
			if( ((nu.xom.Element)(e.getParent())).getLocalName().compareTo("figure") == 0 )
				return true;
			else
				return false;
		} // else if( EPUB...
		
		
		// If we made it to this line, something went wrong.
		return false;
		
	} // hasContainer()

	//////////////////////////////////////////////////////////////////////////////
	// Adds container, i.e. wraps image element in appropriate group.
	public Element addContainer(Element e)
	{
		// Grab namespace first.
		String nameSpace = e.getDocument().getRootElement().getNamespaceURI();
		
		// NIMAS.
		if( documentType == ET_NIMAS )
		{
			// Create all elements.
			Element imgGrpElm = new Element("imggroup", nameSpace);
			Element prodElm = new Element("prodnote", nameSpace);
			Element captElm = new Element("caption", nameSpace);
			Element copyElm = (nu.xom.Element)e.copy();
			
			// If there was no id attribute in the <img> element, add one.
			if(copyElm.getAttribute("id") == null)
				copyElm.addAttribute( new Attribute("id", "TODO!") );
			
			// If the original didn't have an ID value, add one.
			String idValue = copyElm.getAttributeValue("id");
			if(idValue == null)
				idValue = "TODO!";
			
			// Add <prodnote> attributes.
			prodElm.addAttribute( new Attribute("id", "TODO!") );
			prodElm.addAttribute( new Attribute("imgref", idValue) );
			prodElm.addAttribute( new Attribute("render", "required") );
			// Add <caption> attributes.
			captElm.addAttribute( new Attribute("id", "TODO!") );
			captElm.addAttribute( new Attribute("imgref", idValue) );
			
			// Arrange child hierarchy.
			imgGrpElm.insertChild(captElm, 0);
			imgGrpElm.insertChild(prodElm, 0);
			imgGrpElm.insertChild(copyElm, 0);
			
			// Replace given element with this updated one.
			e.getParent().replaceChild(e, imgGrpElm);
			
			// Return newly parented copy of element passed.
			return copyElm;
		}
		else if( documentType == ET_EPUB ) // EPUB
		{
			// Create all elements.
			Element figureElm = new Element("figure", nameSpace);
			Element asideElm = new Element("aside", nameSpace);
			Element copyElm = (nu.xom.Element)e.copy();
			
			// If there was no id attribute in the <img> element, add one.
			if(copyElm.getAttribute("id") == null)
				copyElm.addAttribute( new Attribute("id", "TODO!") );
			
			// If the original didn't have an ID value, add one.
			String idValue = copyElm.getAttributeValue("id");
			if(idValue == null)
				idValue = "TODO!";
			
			// Add aria-describedby attribute if not there.
			if(copyElm.getAttribute("aria-describedby") == null)
				copyElm.addAttribute( new Attribute("aria-describedby", "img-desc") );
			
			// If there was no value in aria-describedby, add one.
			String ariaValue = copyElm.getAttributeValue("aria-describedby");
			if(ariaValue == null)
				ariaValue = "img-desc";
			
			// Add <aside> attributes.
			asideElm.addAttribute( new Attribute("id", ariaValue) );
			
			// Arrange child hierarchy.
			figureElm.insertChild(asideElm, 0);
			figureElm.insertChild(copyElm, 0);
			
			// Replace given element with this updated one.
			e.getParent().replaceChild(e, figureElm);
			
			// Return newly parented copy of element passed.
			return copyElm;
		}
		
		// Type not supported.
		return null;
		
	} // addContainer()
	
	//////////////////////////////////////////////////////////////////////////////
	// Returns named attribute value for given element.
	public String getAttribute(Element e, String atName)
	{
		// Does this attribute exist for the element?
		if( e.getAttribute(atName) != null )
			return e.getAttribute(atName).getValue();
		
		// It doesn't have one.
		return null;
		
	} // getAttribute()
	
	//////////////////////////////////////////////////////////////////////////////
	// Sets the given attribute for the element. Does nothing if the 
	// attribute doesn't exist.
	public void setAttribute(Element e, String atName, String atValue)
	{
		// Make sure the strings actually contain something.
		if(atName == null || atValue == null)
			return;
		
		// Does this attribute exist for the element?
		if( e.getAttribute(atName) != null )
			e.getAttribute(atName).setValue(atValue);
		
	} // setAttribute()
	
	//////////////////////////////////////////////////////////////////////////////
	// Checks for a description element.
	public boolean hasDescElement(Element e)
	{
		// Get parent of <img> element.
		nu.xom.Node parNode = e.getParent();
		
		///////////////////////////
				
		// Find <prodnote>.
		Element ch = null;
		for(int curC = 0; curC < parNode.getChildCount(); curC++) {
			// Make sure this is an element and not a comment or something crazy.
			if( parNode.getChild(curC).getClass().getName().compareTo("nu.xom.Element") == 0) {
				
				// Get the element.
				ch = (Element)parNode.getChild(curC);
				if( documentType == ET_NIMAS ) {
					if( ch.getLocalName().compareTo("prodnote") == 0 )
						return true;
				}
				else if( documentType == ET_EPUB )
					if( ch.getLocalName().compareTo("aside") == 0 )
						return true;
				
			} // if element and not comment.
		
		} // for(int curC = 0...
		
		// Return false... there was either no description element, or we can't handle 
		// the document type.
		return false;
		
	} // hasDescElement()
	
	//////////////////////////////////////////////////////////////////////////////
	// Adds a prodnote
	public void addDescElement()
	{
		
	} // addDescElement()
	
	//////////////////////////////////////////////////////////////////////////////
	// Gets the image description for the given element.
	public String getDescription(Element e)
	{
		// String for description text.
		String descText = "NO DESCRIPTION.";
		
		// Get parent of <img> element.
		nu.xom.Node parNode = e.getParent();
		
		///////////////////////////
		
		// Find description.
		Element ch = null;
		for(int curC = 0; curC < parNode.getChildCount(); curC++) {
			if( parNode.getChild(curC).getClass().getName().compareTo("nu.xom.Element") == 0) {
				ch = (Element)parNode.getChild(curC);
				
				if( documentType == ET_NIMAS ) {
					if( ch.getLocalName().compareTo("prodnote") == 0 ) {
						if(ch.getChildCount() == 0)
							ch.appendChild( new nu.xom.Text("ADD DESCRIPTION!") );
						descText = ch.getChild(0).getValue();
						break;
					} // if element is prodnote...
				}
				else if( documentType == ET_EPUB )
					if( ch.getLocalName().compareTo("aside") == 0 ) {
						if(ch.getChildCount() == 0)
							ch.appendChild( new nu.xom.Text("ADD DESCRIPTION!") );
						descText = ch.getChild(0).getValue();
						break;
					} // if element is aside...
			
			} // If child is an element...
			
		} // for(int curC = 0...
		
		// Return description text.
		return descText;
		
	} // getDescription()
	
	//////////////////////////////////////////////////////////////////////////////
	// Sets the description for a particular element.
	public void setDescription(Element e, String text)
	{
		// Get parent of <img> element.
		nu.xom.Node parNode = e.getParent();
		
		///////////////////////////
		
		// Find description.
		Element ch = null;
		int curC = 0;
		for( ; curC < parNode.getChildCount(); curC++) {
			if( parNode.getChild(curC).getClass().getName().compareTo("nu.xom.Element") == 0) {
				
				// Grab element...
				ch = (Element)parNode.getChild(curC);
				
				// Handle all doc types.
				if( documentType == ET_NIMAS ) {
					if( ch.getLocalName().compareTo("prodnote") == 0 )
					{
						// Append text node if one didn't exist.
						if(ch.getChildCount() == 0)
							ch.appendChild( new nu.xom.Text("ADD DESCRIPTION!") );
						
						// Set text value.
						if(text != null)
						{
							// Set the text.
							nu.xom.Node oldNode = ch.getChild(0);
							nu.xom.Node newNode = new nu.xom.Text(text);
							ch.replaceChild(oldNode, newNode);
							
						} // if(text != null)
						
						break;
					} // if element is prodnote...
				} // If nimas
				else if( documentType == ET_EPUB ) {
					if( ch.getLocalName().compareTo("aside") == 0 )
					{
						// Append text node if one didn't exist.
						if(ch.getChildCount() == 0)
							ch.appendChild( new nu.xom.Text("ADD DESCRIPTION!") );
						
						// Set text value.
						if(text != null)
						{
							// Set the text.
							nu.xom.Node oldNode = ch.getChild(0);
							nu.xom.Node newNode = new nu.xom.Text(text);
							ch.replaceChild(oldNode, newNode);
							
						} // if(text != null)
						
						break;
					} // if element is aside...
				} // if epub.
				
			} // if element and not comment...
			
		} // for(int curC = 0...
		
	} // setDescription()
	
	//////////////////////////////////////////////////////////////////////////////
	// Returns the current document type, whether it be epub, nimas, etc.
	public int getDocType()
	{
		return documentType;
	} // getDocType()
	
	//////////////////////////////////////////////////////////////////////////////
	// Sets doc type. Must call before other methods.
	public void setDocType(int docType)
	{
		documentType = docType;
	} // setDocType()
	
} // class ImageDescriberContext