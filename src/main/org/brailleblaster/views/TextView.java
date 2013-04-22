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

package org.brailleblaster.views;

import java.util.LinkedList;

import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;

import org.brailleblaster.abstractClasses.AbstractContent;
import org.brailleblaster.abstractClasses.AbstractView;
import org.brailleblaster.mapping.TextMapElement;
import org.brailleblaster.wordprocessor.BBEvent;
import org.brailleblaster.wordprocessor.BBSemanticsTable;
import org.brailleblaster.wordprocessor.BBSemanticsTable.Styles;
import org.brailleblaster.wordprocessor.BBSemanticsTable.StylesType;
import org.brailleblaster.wordprocessor.DocumentManager;
import org.brailleblaster.wordprocessor.Message;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Group;


public class TextView extends AbstractView {
	private int oldCursorPosition = -1;
	private int currentChar;
	private int currentStart, currentEnd, previousEnd, nextStart, selectionStart, selectionLength;
	private int currentChanges = 0;
	private int escapeChars;
	private boolean textChanged;
	private BBSemanticsTable stylesTable;
	private StyleRange range;
	private int[] selectionArray;
	
	public TextView (Group documentWindow, BBSemanticsTable table) {
		super (documentWindow, 16, 57, 0, 100);
		this.stylesTable = table;
		this.total = 0;
		this.spaceBeforeText = 0;
		this.spaceAfterText = 0;
		this.escapeChars = 0;
	}

	public void initializeListeners(final DocumentManager dm){	
		view.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				selectionArray = view.getSelectionRanges();
				if(selectionArray[1] > 0){
					selectionStart = selectionArray[0];
					selectionLength = selectionArray[1];
				}
			}		
		});
		
		view.addVerifyKeyListener(new VerifyKeyListener(){
			@Override
			public void verifyKey(VerifyEvent e) {
				oldCursorPosition = view.getCaretOffset();
				currentChar = e.keyCode;
				
				if(oldCursorPosition == currentStart && oldCursorPosition != previousEnd && e.character == SWT.BS && view.getLineAlignment(view.getLineAtOffset(currentStart)) != SWT.LEFT){
					Message message = new Message(BBEvent.ADJUST_ALIGNMENT);
					message.put("sender", "text");
					if(view.getLineAlignment(view.getLineAtOffset(currentStart)) == SWT.RIGHT){
						view.setLineAlignment(view.getLineAtOffset(currentStart), 1, SWT.CENTER);
						message.put("alignment", SWT.CENTER);
					}
					else {
						view.setLineAlignment(view.getLineAtOffset(currentStart), 1, SWT.LEFT);
						message.put("alignment", SWT.LEFT);
					}
					dm.dispatch(message);
					e.doit = false;
				}
				
				if(oldCursorPosition == currentStart && oldCursorPosition != previousEnd && e.character == SWT.BS && view.getLineIndent(view.getLineAtOffset(currentStart)) != 0){
					Message message = new Message(BBEvent.ADJUST_INDENT);
					message.put("sender", "text");
					message.put("indent", 0);
					view.setLineIndent(view.getLineAtOffset(currentStart), 1, 0);
					dm.dispatch(message);
					e.doit = false;
				}
			}		
		});
		
		view.addExtendedModifyListener(new ExtendedModifyListener(){
			@Override
			public void modifyText(ExtendedModifyEvent e) {
				if(e.length > 0){
					if(oldCursorPosition > currentEnd){
						Message adjustmentMessage = new Message(BBEvent.ADJUST_RANGE);
						adjustmentMessage.put("end", oldCursorPosition - currentEnd);
						dm.dispatch(adjustmentMessage);
						currentEnd += oldCursorPosition - currentEnd;
						nextStart += oldCursorPosition - currentEnd;
						
					}
					else if(oldCursorPosition < currentStart){
						Message adjustmentMessage = new Message(BBEvent.ADJUST_RANGE);
						adjustmentMessage.put("start", currentStart - oldCursorPosition);
						dm.dispatch(adjustmentMessage);
						currentStart -= currentStart - oldCursorPosition;
						
					}
					makeTextChange(e.length);
					range = view.getStyleRangeAtOffset(currentStart);
					if(range != null){
						updateRange(range, currentStart, currentEnd - currentStart);
					}
					
				}
				else {
					int offset = view.getCaretOffset() - oldCursorPosition;
					
					if(selectionLength > 0){
						deleteSelection(dm);
					}
					else if(currentChar == SWT.BS){
						if(oldCursorPosition == currentStart && view.getCaretOffset() >= previousEnd){
							shiftLeft(offset);
							sendDeleteSpaceMessage(dm, offset);
						}
						else if(oldCursorPosition == currentStart && view.getCaretOffset() < previousEnd){
							if(textChanged == true)
								sendUpdate(dm);
							setCurrent(dm);
							makeTextChange(offset);
						}
						else if(oldCursorPosition > currentEnd){
							shiftLeft(offset);
							currentChar = SWT.DEL;
							sendDeleteSpaceMessage(dm, offset);
						}
						else{
							makeTextChange(offset);
						}
					}
					else if(currentChar == SWT.DEL){
						if(offset == 0){
							offset = -1;
						}
						
						if(oldCursorPosition == currentEnd && oldCursorPosition < nextStart){
							nextStart += offset;
							sendDeleteSpaceMessage(dm, offset);
						}
						else if(oldCursorPosition == currentEnd && view.getCaretOffset() == nextStart){
							if(textChanged == true)
								sendUpdate(dm);
							view.setCaretOffset(view.getCaretOffset() + 1);
							setCurrent(dm);
							view.setCaretOffset(view.getCaretOffset() - 1);
							makeTextChange(offset);
						}
						else if(oldCursorPosition < currentStart && previousEnd == -1){
							nextStart += offset;
							currentStart += offset;
							currentEnd += offset;
							currentChar = SWT.BS;
							sendDeleteSpaceMessage(dm, offset);
						}
						else {
							makeTextChange(offset);
						}				
					}
					
					if(currentStart == currentEnd && (currentStart == previousEnd || currentStart == nextStart)){
						if(currentStart == currentEnd && textChanged == true){
							sendUpdate(dm);	
							setCurrent(dm);
						}
					}
				}
			}
		});	
		
		view.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {
				Message message = new Message(BBEvent.GET_CURRENT);
				message.put("sender", "text");
			    message.put("offset",view.getCaretOffset());
				dm.dispatch(message);
				setViewData(message);
				if(oldCursorPosition == -1 || oldCursorPosition < currentStart || oldCursorPosition > currentEnd)
					view.setCaretOffset((Integer)message.getValue("start"));
			}

			@Override
			public void focusLost(FocusEvent e) {
				if(textChanged == true)
					sendUpdate(dm);	
			}
		});
		
		view.addCaretListener(new CaretListener(){
			@Override
			public void caretMoved(CaretEvent e) {
				//System.out.println("Current Cursor Positon:\t" + view.getCaretOffset());
				if(currentChar == SWT.ARROW_DOWN || currentChar == SWT.ARROW_LEFT || currentChar == SWT.ARROW_RIGHT || currentChar == SWT.ARROW_UP){
					if(e.caretOffset >= currentEnd || e.caretOffset < currentStart){
						if(textChanged == true){
							sendUpdate(dm);
						}
						setCurrent(dm);
					}
				}
			}
		});
		
		view.addMouseListener(new MouseListener(){
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub	
			}

			@Override
			public void mouseDown(MouseEvent e) {
				if( (view.getCaretOffset() > currentEnd || view.getCaretOffset() < currentStart) && textChanged == true){
					sendUpdate(dm);
				}
				
				if(view.getCaretOffset() > currentEnd || view.getCaretOffset() < currentStart){
					setCurrent(dm);
				}
			}

			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub			
			}		
		});
		
		view.addTraverseListener(new TraverseListener(){
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.stateMask == SWT.CONTROL && e.keyCode == SWT.ARROW_DOWN && nextStart != -1){
					if(textChanged == true){
						sendUpdate(dm);
					}
					sendIncrementCurrent(dm);
					view.setCaretOffset(currentStart);
					e.doit = false;
				}
				else if(e.stateMask == SWT.CONTROL && e.keyCode == SWT.ARROW_UP && previousEnd != -1){
					if(textChanged == true){
						sendUpdate(dm);
					}
					sendDecrementCurrent(dm);
					view.setCaretOffset(currentStart);
					e.doit = false;
				}
			}
		});
	}
	
	private void sendUpdate(DocumentManager dm){
			Message updateMessage = new Message(BBEvent.UPDATE);
			updateMessage.put("offset", view.getCaretOffset());
			updateMessage.put("newText", getString(currentStart, currentEnd - currentStart));
			updateMessage.put("length", currentChanges);
			dm.dispatch(updateMessage);
			currentChanges = 0;
			textChanged = false;
	}
	
	private void setCurrent(DocumentManager dm){
		Message message = new Message(BBEvent.SET_CURRENT);
		message.put("offset", view.getCaretOffset());
		dm.dispatch(message);
		setViewData(message);
	}
	
	private void sendDeleteSpaceMessage(DocumentManager dm, int offset){
		Message message = new Message(BBEvent.TEXT_DELETION);
		message.put("length", offset);
		message.put("deletionType", currentChar);
		message.put("update", false);
		dm.dispatch(message);
		
		if(message.getValue("update").equals(true)){								
			currentChanges += offset;
			if(textChanged == true){
				sendUpdate(dm);
			}
			setCurrent(dm);
			currentEnd += offset;
			incrementNext(offset);
			textChanged = true;
		}
	}
	
	private void sendIncrementCurrent(DocumentManager dm){
		Message message = new Message(BBEvent.INCREMENT);
		dm.dispatch(message);
		setViewData(message);
	}
	
	private void sendDecrementCurrent(DocumentManager dm){
		Message message = new Message(BBEvent.DECREMENT);
		dm.dispatch(message);
		setViewData(message);
	}
	
	private void setViewData(Message message){
		currentStart = (Integer)message.getValue("start");
		currentEnd = (Integer)message.getValue("end");
		previousEnd = (Integer)message.getValue("previous");
		nextStart = (Integer)message.getValue("next");
	}
	
	private void makeTextChange(int offset){
		currentEnd += offset;
		incrementNext(offset);
		currentChanges += offset;
		textChanged = true;
	}
	
	private void incrementNext(int offset){
		if(nextStart != -1){
			nextStart += offset;
		}
	}
	
	private void shiftLeft(int offset){
		currentStart += offset;
		currentEnd += offset;
		nextStart+= offset;
	}
	
	public void setCursor(int offset){
		view.setFocus();
		view.setCaretOffset(offset);
	}

	public void setText(Node n, LinkedList<TextMapElement>list){
		String key = this.stylesTable.getKeyFromAttribute((Element)n.getParent());
		Styles style = this.stylesTable.makeStylesElement(key, n);
		
		appendToView(n);
		handleStyle(style, n);
		
		list.add(new TextMapElement(this.spaceBeforeText + this.total, this.spaceBeforeText + this.total + n.getValue().length() + this.escapeChars,n));
		this.total += this.spaceBeforeText + n.getValue().length() + this.spaceAfterText + this.escapeChars;
		this.spaceAfterText = 0;
		this.spaceBeforeText = 0;
		this.escapeChars = 0;
		view.setCaretOffset(0);
	}
	
	private void appendToView(Node n){
		Element brl = getBrlNode(n);
		int start = 0;
		int end = 0;
		int totalLength = 0;
		
		if(brl != null){
			int[] indexes = getIndexArray(brl);
			if(indexes != null){
				for(int i = 0; i < brl.getChildCount(); i++){
					if(brl.getChild(i) instanceof Text){
						if(i > 0 && ((Element)brl.getChild(i - 1)).getLocalName().equals("newline")){
							String brltext = brl.getChild(i).getValue();
							
							totalLength += brltext.length();
							end = indexes[totalLength - 1];
							if(totalLength == indexes.length){
								view.append("\n" + n.getValue().substring(start));
								if(start == 0)
									this.spaceBeforeText++;
								else
									this.escapeChars++;
							}
							else{
								    end += indexes[totalLength] - end;
									view.append("\n" + n.getValue().substring(start, end));
									if(start == 0)
										this.spaceBeforeText++;
									else
										this.escapeChars++;
							}
							start = end;
						}
						else {
							String brltext = brl.getChild(i).getValue();
							totalLength += brltext.length();
							end = indexes[totalLength - 1];
							if(totalLength == indexes.length){
								view.append(n.getValue().substring(start));
							}
							else{
								end += indexes[totalLength] - end;
								view.append(n.getValue().substring(start, end));
							}
							start = end;
						}
					}
				}
			}
			else {
					view.append("\n" + n.getValue());
					this.spaceBeforeText++;
			}
		}
		else {
			view.append(n.getValue());
		}
	}
	
	private Element getBrlNode(Node n){
		Element e = (Element)n.getParent();
		int index = e.indexOf(n);
		if(index != e.getChildCount() - 1)
			return (Element)e.getChild(index + 1);
		else
			return null;
	}
	
	private int[] getIndexArray(Element e){
		int[] indexArray;
		try {
			String arr = e.getAttributeValue("index");
			String[] tokens = arr.split(" ");
			indexArray = new int[tokens.length];
			for(int i = 0; i < tokens.length; i++){
				indexArray[i] = Integer.valueOf(tokens[i]);
			}
			return indexArray;
		}
		catch(Exception ex){
			return null;
		}
	}
	
	private void handleStyle(Styles style, Node n){
		String viewText = n.getValue();
		for (StylesType styleType : style.getKeySet()) {
			switch(styleType){
				case linesBefore:
	//				String textBefore = makeInsertionString(Integer.valueOf((String)style.get(styleType)),'\n');
	//				insertBefore(this.spaceBeforeText + this.total, textBefore);
					break;
				case linesAfter:
					String textAfter = makeInsertionString(Integer.valueOf((String)style.get(styleType)), '\n');
					insertAfter(this.spaceBeforeText + this.total + viewText.length() + this.spaceAfterText, textAfter);
					break;
				case firstLineIndent: 
					if(isFirst(n)){
						int spaces = Integer.valueOf((String)style.get(styleType));
						this.view.setLineIndent(this.view.getLineAtOffset(this.spaceBeforeText + this.total + this.spaceAfterText + this.escapeChars) , 1, spaces * getFontWidth());
					}
					break;
				case format:
					this.view.setLineAlignment(this.view.getLineAtOffset(this.spaceBeforeText + this.total + this.spaceAfterText), 1, Integer.valueOf((String)style.get(styleType)));	
					break;	
				case Font:
					 setFontRange(this.total, n.getValue().length() + this.spaceAfterText, SWT.ITALIC);
					 break;
				case leftMargin:
				//	this.view.setLineWrapIndent(this.view.getLineAtOffset(this.spaceBeforeText + this.total), 1, this.view.getLineIndent(this.view.getLineAtOffset(this.spaceBeforeText + this.total))+ (2 * getFontWidth()));
					break;
				default:
				//	System.out.println(styleType);
			}
		}	
	}

	private void deleteSelection(DocumentManager dm){
		if(selectionStart >= currentStart && selectionStart + selectionLength <= currentEnd){
			makeTextChange(-selectionLength);
		}
		
		selectionStart = -1;
		selectionLength = -1;
	}
	
	private boolean isFirst(Node n){
		Element parent = (Element)n.getParent();
		if(parent.indexOf(n) == 0){
			if(parent.getLocalName().equals("em") || parent.getLocalName().equals("strong")){
				return isFirstElement(parent);
			}
			else 
				return true;
		}
		else {
			return false;
		}
	}
	
	private boolean isFirstElement(Element child){
		Element parent = (Element)child.getParent();
		while(!this.stylesTable.getKeyFromAttribute(parent).equals("para")){
			if(parent.indexOf(child) != 0)
				return false;
			
			child = parent;
			parent = (Element)parent.getParent(); 
		}
		
		if(parent.indexOf(child) == 0)
			return true;
		else
			return false;
	}
	
	public String getString(int start, int length){
		return view.getTextRange(start, length);
	}

	/* This is a derivative work from 
	 * org.eclipse.swt.custom.DefaultContent.java 
	*/
	private class TextContent extends AbstractContent {
	}

}