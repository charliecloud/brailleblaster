package org.brailleblaster.perspectives.braille.stylers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Text;

import org.brailleblaster.localization.LocaleHandler;
import org.brailleblaster.perspectives.braille.Manager;
import org.brailleblaster.perspectives.braille.document.BBSemanticsTable;
import org.brailleblaster.perspectives.braille.document.BBSemanticsTable.Styles;
import org.brailleblaster.perspectives.braille.document.BBSemanticsTable.StylesType;
import org.brailleblaster.perspectives.braille.document.BrailleDocument;
import org.brailleblaster.perspectives.braille.mapping.elements.BrlOnlyMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.PageMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement;
import org.brailleblaster.perspectives.braille.mapping.maps.MapList;
import org.brailleblaster.perspectives.braille.messages.Message;
import org.brailleblaster.perspectives.braille.messages.Sender;
import org.brailleblaster.perspectives.braille.viewInitializer.ViewInitializer;
import org.brailleblaster.perspectives.braille.views.tree.BBTree;
import org.brailleblaster.perspectives.braille.views.tree.XMLTree;
import org.brailleblaster.perspectives.braille.views.wp.BrailleView;
import org.brailleblaster.perspectives.braille.views.wp.TextView;

public class BoxlineHandler extends Handler{
	
	BrailleDocument document;
	BBSemanticsTable styles;
	BBTree treeView;
	TextView text;
	BrailleView braille;
	
	public BoxlineHandler(Manager manager, ViewInitializer vi,  MapList list){
		super(manager, vi, list);

		this.document = manager.getDocument();
		this.styles = manager.getStyleTable();
		this.text = manager.getText();
		this.braille = manager.getBraille();
		this.treeView = manager.getTreeView();
	}
	
	public void handleBoxline(Message message){
		if(message.getValue("multiSelect").equals(false)) 
			handleSingleBoxLine(message);
		else 
			handleMultiBoxLine(message);
	}
	
	/** Prepares object needed by boxline handler to create boxline around a single element
	 * @param message: Message object passed containing information from style table manager
	 */
	private void handleSingleBoxLine(Message message){
		Element parent = parentStyle(list.getCurrent(), message);
		ArrayList<Element>parents = new ArrayList<Element>();
		parents.add(parent);
		ArrayList<TextMapElement> itemList = list.findTextMapElements(list.getCurrentIndex(), parent, true);
		
		if(((Styles)message.getValue("Style")).getName().equals("boxline"))
			createSingleBoxLine(itemList, parents, message);
		else 
			removeSingleBoxLine(itemList, parent, message);
	}
	
	private void createSingleBoxLine(ArrayList<TextMapElement> itemList, ArrayList<Element>parents, Message message){
		boolean invalid = false;
		for(int i = 0; i < itemList.size() && !invalid; i++){
			if(itemList.get(i) instanceof PageMapElement)
				invalid = true;
		}
		
		if(!invalid){
			getBounds(itemList, message);
			createBoxline(parents, message, itemList);
		}
		else{
			LocaleHandler lh = new LocaleHandler();
			manager.notify(lh.localValue("invalidBoxline.containsPage"));
		}
	}
	
	private void removeSingleBoxLine(ArrayList<TextMapElement> itemList, Element parent, Message message){
		getBounds(itemList, message);
		TextMapElement box = list.findJoiningBoxline((BrlOnlyMapElement)itemList.get(0));
		if(box != null){
			if(list.indexOf(box) < list.indexOf(itemList.get(0)))
				itemList.add(0, box);
			else
				itemList.add(box);
		}
		
		removeSingleBoxline(parent, itemList);
		
		if(list.getCurrentIndex() > list.size())
			manager.dispatch(Message.createSetCurrentMessage(Sender.TEXT, list.get(list.size() - 1).start, false));
		else if(list.size() > 0)
			manager.dispatch(Message.createSetCurrentMessage(Sender.TEXT, list.get(list.getCurrentIndex()).start, false));
	}
	
	/** Prepares object needed by boxline handler to create boxline around a multiple elements
	 * @param message: Message object passed containing information from style table manager
	 */
	private void handleMultiBoxLine(Message message){
		int start=text.getSelectedText()[0];
		int end=text.getSelectedText()[1];
		
		Set<TextMapElement> itemSet = manager.getElementInSelectedRange(start, end);		
		Iterator<TextMapElement> itr = itemSet.iterator();
		ArrayList<Element>parents = new ArrayList<Element>();
		ArrayList<TextMapElement>itemList = new ArrayList<TextMapElement>();
		
		boolean invalid = false;
		
		while(itr.hasNext() && !invalid){
			TextMapElement tempElement= itr.next();
			if(tempElement instanceof BrlOnlyMapElement){
				BrlOnlyMapElement b = list.findJoiningBoxline((BrlOnlyMapElement)tempElement);
				if((b == null && !tempElement.parentElement().getAttributeValue(SEMANTICS).contains("middleBox") && !tempElement.parentElement().getAttributeValue(SEMANTICS).contains("bottomBox") )
						|| (b != null && (b.start > end || b.end < start))){
					invalid = true;
					LocaleHandler lh = new LocaleHandler();
					manager.notify(lh.localValue("invalidBoxline.incorrectSelection"));
					break;
				}
			}
			Element parent = parentStyle(tempElement, message);
			itemList.addAll(list.findTextMapElements(list.getNodeIndex(tempElement), parent, true));
			parents.add(parent);
		}
		
		for(int i = 0; i < itemList.size() && !invalid; i++){
			if(itemList.get(i) instanceof PageMapElement){
				invalid = true;
				LocaleHandler lh = new LocaleHandler();
				manager.notify(lh.localValue("invalidBoxline.containsPage"));
			}
		}
		
		if(!invalid){
			if(((Styles)message.getValue("Style")).getName().equals("boxline"))
				createMultipleBoxline(itemList, parents,message);
			else 
				removeMultipleBoxlines(itemList);
		}
	}
	
	private void createMultipleBoxline(ArrayList<TextMapElement> itemList, ArrayList<Element> parents, Message message){
		getBounds(itemList, message);
		createBoxline(parents, message, itemList);	
	}
	
	private void removeMultipleBoxlines(ArrayList<TextMapElement> itemList){
		removeMultiBoxline(itemList);
		
		if(list.getCurrentIndex() > list.size())
			manager.dispatch(Message.createSetCurrentMessage(Sender.TEXT, list.get(list.size() - 1).start, false));
		else if(list.size() > 0)
			manager.dispatch(Message.createSetCurrentMessage(Sender.TEXT, list.get(list.getCurrentIndex()).start, false));
		
		manager.dispatch(Message.createUpdateCursorsMessage(Sender.TREE));
	}
	
	/** Wraps a block level element in the appropriate tag then translates and adds boxline brl top and bottom nodes
	 * @param p: parent of text nodes, the block element to be wrapped in a boxline
	 * @param m: message passed to views containing offset positions
	 * @param itemList: arraylist containing text nodes of the block element
	 */
	private void createBoxline(ArrayList<Element>parents, Message m, ArrayList<TextMapElement> itemList){		
		Element wrapper = document.wrapElement(parents, BOXLINE);
		if(wrapper != null){
			ArrayList<Element>sidebarList = findBoxlines(wrapper);
			if(sidebarList.size() > 1)
				createMultipleBoxlines(sidebarList, wrapper, parents, itemList);
			else 
				createFullBoxline(wrapper, parents, m, itemList);
			
			manager.dispatch(Message.createSetCurrentMessage(Sender.TREE, list.get(list.getCurrentIndex() + 1).start, false));
			manager.dispatch(Message.createUpdateCursorsMessage(Sender.TREE));
		}
	}
	
	private void createMultipleBoxlines(ArrayList<Element> elList, Element wrapper, ArrayList<Element>parents, ArrayList<TextMapElement> itemList){			
		for(int i = 0; i < elList.size(); i++){
			if(i == 0)
				setStyle(elList.get(i), TOPBOX);
			else if(i == elList.size() - 1)
				setStyle(elList.get(i), BOTTOMBOX);
			else
				setStyle(elList.get(i), MIDDLEBOX);
		}
			
		Document doc = document.translateElements(elList);
		Element parent = (Element)doc.getChild(0);
		
		String style = getSemanticAttribute(wrapper);
		Message m = new Message(null);
		if(style.equals(TOPBOX))
			createFullBoxline(wrapper, parents, m, itemList);
		else if(style.equals(BOTTOMBOX) || style.equals(MIDDLEBOX))
			createHalfBox(wrapper, m, itemList, parents);
		
		int index = elList.indexOf(wrapper);
		elList.remove(index);
		parent.removeChild(index);
		
		resetSidebars(elList, parent);
	}
	
	private void createFullBoxline(Element wrapper, ArrayList<Element>parents,  Message m, ArrayList<TextMapElement> itemList){
		Element boxline = document.translateElement((Element)wrapper.copy());
		int startPos = createTopLine(wrapper, m, itemList, boxline, styles.get(styles.getKeyFromAttribute(parents.get(0))));
		int endPos = createBottomLine(wrapper, m, itemList, boxline, startPos, styles.get(styles.getKeyFromAttribute(parents.get(parents.size() - 1))));
	
		int treeIndex;
		if(!treeView.getTree().getSelection()[0].equals(treeView.getRoot()))
			treeIndex = treeView.getTree().getSelection()[0].getParentItem().indexOf(treeView.getTree().getSelection()[0]);
		else
			treeIndex = 0;
	
		//remove items from tree
		removeTreeItems(itemList);
	
		ArrayList<TextMapElement> treeItemData = new ArrayList<TextMapElement>();
		treeItemData.add(list.get(startPos));
		treeItemData.add(list.get(endPos));
		//add aside or sidebar to tree
		treeView.newTreeItem(treeItemData, treeIndex, 0);
		
		if(checkSemanticsAttribute((Element)wrapper.getParent(), BOXLINE))
			convertFullBox((Element)wrapper.getParent());
	}
	
	private void convertFullBox(Element e){
		String style = checkSemanticsAttribute(e,BOXLINE) ? FULLBOX : BOXLINE;
		setStyle(e, style);
		Element copy = document.translateElement((Element)e.copy());
		replaceBoxLine((Element)e.getChild(0), (Element)copy.getChild(0));
		replaceBoxLine((Element)e.getChild(e.getChildCount() - 1), (Element)copy.getChild(copy.getChildCount() - 1));
	}
	
	private void createHalfBox(Element wrapper, Message m, ArrayList<TextMapElement>itemList,ArrayList<Element>parents){		
		Element boxline = document.translateElement((Element)wrapper.copy());
		int endPos = createBottomLine(wrapper, m, itemList, boxline, list.indexOf(itemList.get(itemList.size() - 1)), styles.get(styles.getKeyFromAttribute(parents.get(parents.size() - 1))));
		
		int treeIndex;
		if(!treeView.getTree().getSelection()[0].equals(treeView.getRoot()))
			treeIndex = treeView.getTree().getSelection()[0].getParentItem().indexOf(treeView.getTree().getSelection()[0]);
		else
			treeIndex = 0;
		
		//remove items from tree
		removeTreeItems(itemList);
		
		ArrayList<TextMapElement> treeItemData = new ArrayList<TextMapElement>();
		treeItemData.add(list.get(endPos));
		//add aside or sidebar to tree
		treeView.newTreeItem(treeItemData, treeIndex, 0);
	}
	
	/** Private helper method that handles specifics of creating map element and adding the top line in the view
	 * @param wrapper: Aside or Sidebar in the DOM which the top boxline will be enclosed
	 * @param m: The message passed containing offset info
	 * @param itemList: the list containing the map elements to be enclosed in the boxline
	 * @param boxline: The translation passed from createBoxline which contains the top boxline element
	 * @param firstStyle: The style of the first element, used to determine where to place the boxline if a line before is suppposed to occur
	 * @return integer representing the index of the top boxline in the maplist
	 */
	private int createTopLine(Element wrapper, Message m, ArrayList<TextMapElement>itemList, Element boxline, Styles firstStyle){
		int startPos = list.indexOf(itemList.get(0));
		
		//find start position
		int start, brailleStart;
		if(firstStyle.contains(StylesType.linesBefore)){
			start = (Integer)m.getValue("prev");
			brailleStart = (Integer)m.getValue("braillePrev");
		}
		else {
			start = itemList.get(0).start;
			brailleStart = itemList.get(0).brailleList.getFirst().start;
		}
		
		//insert top boxline
		wrapper.insertChild(boxline.removeChild(0), 0);
		BrlOnlyMapElement b1 =  new BrlOnlyMapElement(wrapper.getChild(0), wrapper);
		b1.setOffsets(start, start + b1.textLength());
		b1.setBrailleOffsets(brailleStart, brailleStart + b1.getText().length());
		vi.addElementToSection(list, b1, startPos);
		
		//set text
		text.insertText(start, list.get(startPos).getText() + "\n");
		braille.insertText(brailleStart, list.get(startPos).brailleList.getFirst().value() + "\n");
		list.shiftOffsetsFromIndex(startPos + 1, list.get(startPos).getText().length() + 1, list.get(startPos).brailleList.getFirst().value().length() + 1);
		
		return startPos;
	}
	
	/**Private helper method that handles specifics of creating map element and adding the bottom line in the view
	 * @param wrapper: Aside or Sidebar in the DOM which the top boxline will be enclosed
	 * @param m: The message passed containing offset info
	 * @param itemList: the list containing the map elements to be enclosed in the boxline
	 * @param boxline: The translation passed from createBoxline which contains the bottom boxline element
	 * @param lastStyle: The style of the last element, used to determine where to place the boxline if a line after is supposed to occur following the lst element
	 * @return: int of the index of the boxline
	 */
	private int createBottomLine(Element wrapper, Message m, ArrayList<TextMapElement>itemList, Element boxline, int startPos, Styles lastStyle){
		//find end position
		int endPos = list.indexOf(itemList.get(itemList.size() - 1)) + 1;
		int end, brailleEnd;
		if(lastStyle.contains(StylesType.linesAfter)){
			end = (Integer)m.getValue("next") + list.get(startPos).getText().length() + 1;
			brailleEnd = (Integer)m.getValue("brailleNext") + list.get(startPos).getText().length() + 1;
		}
		else {
			end = list.get(endPos - 1).end;
			brailleEnd = itemList.get(itemList.size() - 1).brailleList.getLast().end;
		}
		
		//insert bottom boxline
		wrapper.appendChild(boxline.removeChild(boxline.getChildCount() - 1));
		BrlOnlyMapElement b2 =  new BrlOnlyMapElement(wrapper.getChild(wrapper.getChildCount() - 1), wrapper);
		b2.setOffsets(end + 1, end + 1 + b2.textLength());
		b2.setBrailleOffsets(brailleEnd + 1, brailleEnd + 1 + b2.getText().length());
		vi.addElementToSection(list, b2, endPos);

		//set text
		text.insertText(end, "\n" + list.get(endPos).getText());
		braille.insertText(brailleEnd, "\n" + list.get(endPos).brailleList.getFirst().value());
		list.shiftOffsetsFromIndex(endPos + 1, list.get(endPos).getText().length() + 1, list.get(endPos).brailleList.getFirst().value().length() + 1);
			
		return endPos;
	}
	
	private void resetSidebars(ArrayList<Element> elList, Element parent){
		while(elList.size() > 0){
			if(getSemanticAttribute(parent.getChildElements().get(0)).equals(TOPBOX) || getSemanticAttribute(parent.getChildElements().get(0)).equals(BOXLINE))
				changeToFullBox(elList.get(0), parent.getChildElements().get(0));
			else if(getSemanticAttribute(parent.getChildElements().get(0)).equals(BOTTOMBOX) || getSemanticAttribute(parent.getChildElements().get(0)).equals(MIDDLEBOX))
				changeToHalfBox(elList.get(0), parent.getChildElements().get(0));
			
			setStyle(elList.get(0), getSemanticAttribute(parent.getChildElements().get(0)));
			parent.removeChild(0);
			elList.remove(0);
		}
	}
	
	/** Modifies full or top box styles which have both top and bottom lines
	 * @param box : Element to be modified
	 * @param replacement : New translation element
	 */
	private void changeToFullBox(Element box, Element replacement){
		//set top
		if(box.getChild(0) instanceof Element && ((Element)box.getChild(0)).getLocalName().equals("brl")){
			replaceBoxLine((Element)box.getChild(0), (Element)replacement.getChild(0));
		}
		else if(box.getChild(0) instanceof Element && !((Element)box.getChild(0)).getLocalName().equals("brl")){
			Text t = findText(box.getChild(0));
			if(t != null)
				insertBoxLine(list.findNodeIndex(t, 0), (Element)box, (Element)replacement.removeChild(0));
		}
		
		//set bottom
		if(box.getChild(box.getChildCount() - 1) instanceof Element &&  ((Element)box.getChild(0)).getLocalName().equals("brl"))
			replaceBoxLine((Element)box.getChild(box.getChildCount() - 1), (Element)replacement.getChild(replacement.getChildCount() - 1));
	}
	
	/** Modifies middle or bottom box styles which have only a lower boxline
	 * @param box : Element to be modified
	 * @param replacement : New translation element
	 */
	private void changeToHalfBox(Element box, Element replacement){
		if(box.getChild(0) instanceof Element && ((Element)box.getChild(0)).getLocalName().equals("brl")){
			int index = list.findNodeIndex(box.getChild(0), 0);
			BrlOnlyMapElement b = (BrlOnlyMapElement)list.get(index);
			removeTopBoxline(b);
			b.parentElement().removeChild(b.n);
		}
		
		if(box.getChild(box.getChildCount() - 1) instanceof Element &&  ((Element)box.getChild(box.getChildCount() - 1)).getLocalName().equals("brl"))
			replaceBoxLine((Element)box.getChild(box.getChildCount() - 1), (Element)replacement.getChild(replacement.getChildCount() - 1));
	}
	
	private void insertBoxLine(int index, Element box, Element brl){
		String style = getSemanticAttribute(list.get(index).parentElement());
		Styles firstStyle = styles.get(style);
		//inserted in DOM
		box.insertChild(brl, 0);
		
		Message m = new Message(null);
		//find start position
		int start, brailleStart;
		if(firstStyle.contains(StylesType.linesBefore)){
			start = (Integer)m.getValue("prev");
			brailleStart = (Integer)m.getValue("braillePrev");
		}
		else {
			start = list.get(index).start;
			brailleStart = list.get(index).brailleList.getFirst().start;
		}
		
		BrlOnlyMapElement b1 =  new BrlOnlyMapElement(box.getChild(0), box);
		b1.setOffsets(start, start + b1.textLength());
		b1.setBrailleOffsets(brailleStart, brailleStart + b1.getText().length());
		vi.addElementToSection(list, b1, index);
		
		//set text
		text.insertText(start, list.get(index).getText() + "\n");
		braille.insertText(brailleStart, list.get(index).brailleList.getFirst().value() + "\n");
		list.shiftOffsetsFromIndex(index + 1, list.get(index).getText().length() + 1, list.get(index).brailleList.getFirst().value().length() + 1);
	}
	
	private void replaceBoxLine(Element brl, Element replacement){		
		int index = list.findNodeIndex(brl, 0);
		Text t = document.findBoxlineTextNode(brl);
		Text newText = document.findBoxlineTextNode(replacement);
		int length = t.getValue().length() - newText.getValue().length();
		t.setValue(replacement.getValue());
		braille.replaceTextRange(list.get(index).brailleList.getFirst().start, list.get(index).brailleList.getLast().end - list.get(index).brailleList.getFirst().start, t.getValue());
		
		if(length > 0)
			list.shiftOffsetsFromIndex(index + 1, length, length);
	}
		
	private ArrayList<Element> findBoxlines(Element e){
		ArrayList<Element> elList = new ArrayList<Element>();
		Element parent = (Element)e.getParent();
		int index = parent.indexOf(e);
			
		for(int i = index - 1; i >= 0; i--){
			if(parent.getChild(i) instanceof Element && isBoxLine((Element)parent.getChild(i)))
				elList.add(0, (Element)parent.getChild(i));
			else 
				break;
		}
			
		elList.add(e);
			
		for(int i = index + 1; i < parent.getChildCount(); i++){
			if(parent.getChild(i) instanceof Element && isBoxLine((Element)parent.getChild(i)))
				elList.add((Element)parent.getChild(i));
			else 
				break;
		}
		return elList; 
	}
	
	/** Removes a boxline from the views and the DOM
	 * @param boxline : Element wrapping content and representing a boxline
	 * @param itemList : List containing opening and closing boxline
	 */
	private void removeSingleBoxline(Element boxline, ArrayList<TextMapElement> itemList){		
		ArrayList<Element>sidebarList = findBoxlines(boxline);
		removeBoxLine(boxline, itemList);
		
		sidebarList.remove(boxline);
		if(sidebarList.size() > 0){
			Element parent = (Element) sidebarList.get(0).getParent();
			parent = buildSegment(parent, parent.indexOf(sidebarList.get(0)), parent.indexOf(sidebarList.get(sidebarList.size() - 1)));
			resetSidebars(sidebarList, parent);
		}
	}
	
	/** Handles deleting a boxline when text selection occurs and one or more boxlines may be selected
	 * @param itemList : ItemList containing text map elements in selection collected via manager's getSelected method
	 */
	private void removeMultiBoxline(ArrayList<TextMapElement> itemList){
		clearNonBrlElements(itemList);
		
		int start = itemList.get(0).parentElement().getParent().indexOf(itemList.get(0).parentElement());
		int end = itemList.get(itemList.size() - 1).parentElement().getParent().indexOf(itemList.get(itemList.size() - 1).parentElement());
		Element parent = (Element) itemList.get(0).parentElement().getParent();
		
		for(int i = 0,j = itemList.size(); i < itemList.size(); i++, j--){
			BrlOnlyMapElement b = list.findJoiningBoxline((BrlOnlyMapElement)itemList.get(i));
			if(!itemList.contains(b) && b != null){
				itemList.add(j, b);
				if(b.parentElement().getParent().indexOf(b.parentElement()) > end && b.parentElement().getParent().equals(parent))
					end = b.parentElement().getParent().indexOf(b.parentElement()); 
			}
		}
		
		while(itemList.size() > 0){
			ArrayList<TextMapElement>boxline = new ArrayList<TextMapElement>();
			if(getSemanticAttribute(itemList.get(0).parentElement()).equals(BOXLINE) || getSemanticAttribute(itemList.get(0).parentElement()).equals(TOPBOX) || getSemanticAttribute(itemList.get(0).parentElement()).equals(FULLBOX)){
				int index = getMatchingParent(itemList, 0);
				boxline.add(itemList.get(0));
				boxline.add(itemList.get(index));
				itemList.remove(0);
				itemList.remove(index - 1);
			}
			else if(getSemanticAttribute(itemList.get(0).parentElement()).equals(MIDDLEBOX) || getSemanticAttribute(itemList.get(0).parentElement()).equals(BOTTOMBOX)){
				boxline.add(itemList.get(0));
				itemList.remove(0);
			}
			treeView.populateItem(boxline.get(0).parentElement());
			removeBoxLine(boxline.get(0).parentElement(), boxline);
		}
		
		if(start > 0 && parent.getChild(start - 1) instanceof Element && isBoxLine((Element)parent.getChild(start - 1)))
			start--;
		if(end < parent.getChildCount() - 1 && parent.getChild(end + 1) instanceof Element && isBoxLine((Element)parent.getChild(end + 1)))
			end++;
		
		ArrayList<Element>elList = new ArrayList<Element>();
		for(int i = start; i <= end; i++){
			if(parent.getChild(i) instanceof Element && isBoxLine((Element)parent.getChild(i)))
				elList.add((Element)parent.getChild(i));
		}
		
		Element newDoc = buildSegment(parent, start, end);
		resetSidebars(elList, newDoc);
	}
	
	private void removeBoxLine(Element boxline, ArrayList<TextMapElement> itemList){
		String style = getSemanticAttribute(boxline);
		if(style.equals(BOXLINE) || style.equals(TOPBOX) ||  style.equals(FULLBOX)){
			Element parent = null;
			removeTopBoxline((BrlOnlyMapElement)itemList.get(0));
			removeBottomBoxline((BrlOnlyMapElement)itemList.get(1));
			if(isBoxLine((Element)boxline.getParent()) && getSemanticAttribute((Element)boxline.getParent()).equals(FULLBOX)){
				if(nestedSidebarCount((Element)boxline.getParent()) == 1)
					parent = (Element)boxline.getParent();
			}
				
			removeBoxLineElement(boxline);
			if(parent != null)
				convertFullBox(parent);
			
		}
		else if(style.equals(MIDDLEBOX) || style.equals(BOTTOMBOX)){
			removeBottomBoxline((BrlOnlyMapElement)itemList.get(0));
			removeBoxLineElement(boxline);
		}
	}
	
	private Element buildSegment(Element parent, int start, int end){
		ArrayList<Element>elList = new ArrayList<Element>();
		for(int i = start; i <= end; i++){
			if(parent.getChild(i) instanceof Element)
				elList.add((Element)parent.getChild(i));
		}
		
		for(int i = 0; i < elList.size(); i++){
			if(i == 0 && isBoxLine(elList.get(i))){
				if(i < elList.size() - 1 && isBoxLine(elList.get(i + 1)))
					setStyle(elList.get(i), TOPBOX);
				else
					setStyle(elList.get(i), BOXLINE);
			}
			else if(i == elList.size() - 1 && isBoxLine(elList.get(i))){
				if(i > 0 && isBoxLine(elList.get(i - 1)))
					setStyle(elList.get(i), BOTTOMBOX);
				else
					setStyle(elList.get(i), BOXLINE);
			}
			else {
				if(isBoxLine(elList.get(i))){
					if(isBoxLine(elList.get(i - 1)) && isBoxLine(elList.get(i + 1)))
						setStyle(elList.get(i), MIDDLEBOX);
					else if(isBoxLine(elList.get(i - 1)) && !isBoxLine(elList.get(i + 1)))
						setStyle(elList.get(i), BOTTOMBOX);
					else if(!isBoxLine(elList.get(i - 1)) && isBoxLine(elList.get(i + 1)))
						setStyle(elList.get(i), TOPBOX);
					else
						setStyle(elList.get(i), BOXLINE);
				}
			}
		}
		
		Document doc = document.translateElements(elList);
		Element root = (Element)doc.getChild(0);
		Elements els = root.getChildElements();
		for(int i = 0; i < els.size(); i++){
			if(!isBoxLine(els.get(i)))
				root.removeChild(els.get(i));
		}
		
		return root;
	}
	
	private void clearNonBrlElements(ArrayList<TextMapElement> itemList){
		for(int i = 0; i < itemList.size(); i++){
			if(!(itemList.get(i) instanceof BrlOnlyMapElement)){
				itemList.remove(i);
				i--;
			}
		}
	}
	
	/** Removes the boxline from the views and maplist
	 * @param b 
	 */
	private void removeTopBoxline(BrlOnlyMapElement b){
		int index = list.indexOf(b);
		manager.getText().replaceTextRange(b.start, (b.end + 1) - b.start, "");
		manager.getBraille().replaceTextRange(b.brailleList.getFirst().start, (b.brailleList.getFirst().end + 1) - b.brailleList.getFirst().start, "");
		list.shiftOffsetsFromIndex(index,  -((b.end + 1) - b.start), -((b.brailleList.getFirst().end + 1) - b.brailleList.getFirst().start));
		list.remove(index);
	}
	
	/** Removes the boxline from the views and maplist
	 * @param b 
	 */
	private void removeBottomBoxline(BrlOnlyMapElement b){
		int index = list.indexOf(b);
		manager.getText().replaceTextRange(b.start - 1, b.end - (b.start - 1), "");
		manager.getBraille().replaceTextRange(b.brailleList.getFirst().start - 1, b.brailleList.getFirst().end - (b.brailleList.getFirst().start - 1), "");
		list.shiftOffsetsFromIndex(index,  -(b.end - (b.start - 1)), -(b.brailleList.getFirst().end - (b.brailleList.getFirst().start - 1)));
		list.remove(index);
	}
	
	/** Removes boxline from DOM and re-inserts contents into the DOM
	 * @param boxline : Element wrapping content and representing a boxline
	 */
	private void removeBoxLineElement(Element boxline){
		int index = boxline.getParent().indexOf(boxline);
		Elements els = boxline.getChildElements();
		
		for(int i = 0; i < els.size(); i++){
			if((i == 0 || i == els.size() - 1) && els.get(i).getLocalName().equals("brl"))
				els.get(i).getParent().removeChild(els.get(i));
			else {
				boxline.getParent().insertChild(boxline.removeChild(els.get(i)), index);
				index++;
			}
		}
		treeView.resetTreeItem(boxline);
		boxline.getParent().removeChild(boxline);
	}
	
	private Text findText(Node n){
		if(n.getChild(0) instanceof Text)
			return (Text)n.getChild(0);
		else if(n instanceof Element)
			return findText(n);
		else 
			return null;
	}
	
	private void removeTreeItems(ArrayList<TextMapElement>itemList){
		if(treeView.getClass().equals(XMLTree.class)){
			for(int i = 0; i < itemList.size(); i++){
				Message treeMessage = new Message(null);
				treeMessage.put("removeAll", true);
				treeView.removeItem(itemList.get(i), treeMessage);
			}
		}
	}
	
	private void setStyle(Element e, String style){
		Message m = new Message(null);
		m.put("element", e);
		m.put("type", "style");
		m.put("action", style);
		manager.getDocument().applyAction(m);
	}
	
	private int getMatchingParent(ArrayList<TextMapElement>elList, int index){
		Element parent = elList.get(index).parentElement();
		for(int i = 0; i < elList.size(); i++)
			if(i != index && elList.get(i).parentElement().equals(parent))
				return i;
		
		return -1;
	}
	
	private int nestedSidebarCount(Element e){
		int sidebarCount = 0;
		Elements els = e.getChildElements();
		for(int i = 0; i < els.size(); i++){
			if(isBoxLine(els.get(i)))
				sidebarCount++;
		}
		return sidebarCount;
	}
	
	/***
     * Get parent style of the current TextMapElement 
     * @param current
     * @param message
     * @return
     */
	private Element parentStyle(TextMapElement current, Message message) {
		Element parent;
		if(current instanceof PageMapElement || current instanceof BrlOnlyMapElement)
			parent = current.parentElement();
		else
			parent = document.getParent(current.n, true);
		
		message.put("previousStyle", styles.get(styles.getKeyFromAttribute(parent)));
		return parent;
	}
	
	/***
	 * get bounds of elements in the list based on previous and next element 
	 * @param itemList : all selected items which we want style to be applied
	 * @param message : passing information regarding styles
	 */
	private void getBounds(ArrayList<TextMapElement> itemList, Message message) {
		int start = list.indexOf(itemList.get(0));
		int end = list.indexOf(itemList.get(itemList.size() - 1));
	
		if (start > 0) {
			message.put("prev", list.get(start - 1).end);
			message.put("braillePrev",
					list.get(start - 1).brailleList.getLast().end);
		} else {
			message.put("prev", -1);
			message.put("braillePrev", -1);
		}

		if (end < list.size() - 1) {
			message.put("next", list.get(end + 1).start);
			message.put("brailleNext",
					list.get(end + 1).brailleList.getFirst().start);
		} else {
			message.put("next", -1);
			message.put("brailleNext", -1);
		}

		text.getBounds(message, itemList);
		braille.getBounds(message, itemList);
	}	
}
