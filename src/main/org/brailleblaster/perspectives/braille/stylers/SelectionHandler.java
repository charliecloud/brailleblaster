package org.brailleblaster.perspectives.braille.stylers;

import java.util.ArrayList;
import java.util.LinkedList;

import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ParentNode;
import nu.xom.Text;

import org.brailleblaster.perspectives.braille.Manager;
import org.brailleblaster.perspectives.braille.document.BBSemanticsTable.StylesType;
import org.brailleblaster.perspectives.braille.eventQueue.EventFrame;
import org.brailleblaster.perspectives.braille.eventQueue.EventTypes;
import org.brailleblaster.perspectives.braille.eventQueue.SelectionEvent;
import org.brailleblaster.perspectives.braille.mapping.elements.BrailleMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.BrlOnlyMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.PageMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement;
import org.brailleblaster.perspectives.braille.mapping.maps.MapList;
import org.brailleblaster.perspectives.braille.messages.Message;
import org.brailleblaster.perspectives.braille.viewInitializer.ViewInitializer;

public class SelectionHandler extends Handler {
	boolean emptyNode = false;
    LinkedList<Integer>nodes = null;
    EventFrame evFrame;
    
    public SelectionHandler(Manager manager, ViewInitializer vi, MapList list) {
        super(manager, vi, list);
    }
    
    @SuppressWarnings("unchecked")
	public void removeSelection(Message m){
    	evFrame = new EventFrame();
    	int startPos = (Integer)m.getValue("start");
        int endPos = (Integer)m.getValue("end");
        String replacedText = (String)m.getValue("replacedText");
        String replacementText = (String)m.getValue("replacementText");
        int startIndex = getIndex(startPos);
        int endIndex = getIndex(endPos);
        Element firstEl = getBlockElement(startIndex);
        Element lastEl = getBlockElement(endIndex);    
        ArrayList<TextMapElement> firstList = getBlockMapElements(startIndex, firstEl);
     	TextMapElement first = list.get(startIndex);
     	TextMapElement last = list.get(endIndex);
     	int textStart = first.start;
     	int brailleStart = first.brailleList.getFirst().start;
    	ArrayList<Integer> indexes = tree.getItemPath();
    	boolean removeLast = false;
    	
        if(firstEl.equals(lastEl)){ 
        	if(textStart > startPos){
         		brailleStart -= textStart - startPos;
         		textStart = startPos;
         	}
        	boolean format = false; 
        	if(beforeStart(startPos, firstList) || afterEnd(endPos, firstList))
        		format = true;
        	
        	addEvent(firstEl, list.indexOf(firstList.get(0)), textStart, brailleStart, (ArrayList<Integer>)indexes.clone(), false, format, startPos, endPos);
        	updateFirstNode(firstList, firstEl, first, startPos, endPos, replacementText);
        	int index = endIndex;
        	if(!first.equals(last) && endPos < last.end){
         		list.setCurrent(endIndex);
         		updateSecondNode(last, startPos, endPos, replacedText);
         	}
         	else if(endPos >= last.end){
         		removeLast = true;
         		index++;
         	}      	
         	
         	for(int i = startIndex + 1; i < index; i++)
         		removeElement(i);
           	
         	clearViewRanges(textStart, brailleStart, firstList.get(firstList.size() - 1), startIndex, endPos - startPos, endPos, replacementText);    	
         	clearListItems(firstList);
         	
         	ArrayList<TextMapElement>mapList = new ArrayList<TextMapElement>(); 
         	if(readOnly(first)){
         		mapList.add(first);
         		repopulateReadOnly(mapList, startIndex);
         	}
         	else {
         		if(emptyNode)
             		mapList = recreateEmptyElement(firstEl, startIndex, nodes);
             	else
             		mapList.addAll(recreateElement(firstEl, startIndex));
         	}
         	
         	rebuildViews(mapList, startIndex, textStart, brailleStart, indexes, false);
         }
         else {
        	boolean clearAll = false;	 
        	boolean removeFirst = false;  
        	
            ArrayList<TextMapElement> lastList = getBlockMapElements(endIndex, lastEl);
         	
         	if(list.indexOf(first) == 0 && list.indexOf(last) == list.size() - 1){
         		if(endPos >= last.end)
         			clearAll = true;
         	}    		
         
         	if(!clearAll && !readOnly(list.get(list.indexOf(firstList.get(firstList.size()  - 1)) + 1)) && (startPos <= first.start && endPos > first.end) && endPos < last.end && replacementText.length() == 0)
         		removeFirst = true;
         	else if(!readOnly(list.get(endIndex - 1)) && endPos > first.end && endPos >= last.end )
         		removeLast = true;
         	
         	
         	boolean format = beforeStart(startPos, firstList);
         	if(readOnly(first))
         		addEvent(list.get(list.indexOf(firstList.get(0))).parentElement(), list.indexOf(firstList.get(0)), textStart, brailleStart, (ArrayList<Integer>)indexes.clone(), removeFirst, format, startPos, endPos);
         	else
         		addEvent(firstEl, list.indexOf(firstList.get(0)), textStart, brailleStart, (ArrayList<Integer>)indexes.clone(), removeFirst, format, startPos, endPos);
         
         	addEvents(list.indexOf(firstList.get(firstList.size() - 1)) + 1, list.indexOf(lastList.get(0)));
         	
         	format = !readOnly(last) && hasLinesBefore(lastEl) || afterEnd(endPos, lastList);
         	if(readOnly(last))
         		addEvent(list.get(list.indexOf(lastList.get(0))).parentElement(), list.indexOf(lastList.get(0)), lastList.get(0).start, lastList.get(0).brailleList.getFirst().start, (ArrayList<Integer>)indexes.clone(), removeLast, format, startPos, endPos);
         	else
         		addEvent(lastEl, list.indexOf(lastList.get(0)), lastList.get(0).start, lastList.get(0).brailleList.getFirst().start, (ArrayList<Integer>)indexes.clone(), removeLast, format, startPos, endPos);
         	
         	updateFirstNode(firstList, firstEl, first, startPos, endPos, replacementText);	
         	
         	if(!readOnly(first)){
         		int index; 
         		if(removeFirst)
         			index = list.indexOf(first);
         		else
         			index = list.indexOf(first) + 1;
         		
         		int removed = clearElement(index, list.indexOf(firstList.get(firstList.size() - 1)) + 1);
     			endIndex -= removed;
         	}
     		
         	 if(startIndex > list.indexOf(firstList.get(0))){
             		startIndex = list.indexOf(firstList.get(0));
             		textStart = list.get(startIndex).start;
             		brailleStart = list.get(startIndex).brailleList.getFirst().start;
         	 }
                 
         	ArrayList<TextMapElement> readOnly = new ArrayList<TextMapElement>();
         	if(list.indexOf(firstList.get(firstList.size() - 1)) != list.indexOf(lastList.get(0)) - 1){
         		readOnly = removeElements(list.indexOf(firstList.get(firstList.size() - 1)) + 1, list.indexOf(lastList.get(0))); 
         		endIndex = endIndex - readOnly.size();
         		for(int j = 0; j < readOnly.size(); j++){
         			if(!(readOnly.get(j) instanceof BrlOnlyMapElement) && !(readOnly.get(j) instanceof PageMapElement)){
         				readOnly.remove(j);
         				j--;
         			}      				
         		}
         	}
         	
         	if(!readOnly(last) && !clearAll){
         		list.setCurrent(endIndex);
         		updateSecondNode(last, startPos, endPos, replacedText);	
         		int removed = clearElement(list.indexOf(lastList.get(0)), list.indexOf(last));
         		endIndex -= removed;
         	}
         	else if(clearAll){
         		removeElement(list.indexOf(last));
         	}
         	
         	 if(endIndex < list.indexOf(lastList.get(lastList.size() - 1)))
          		endIndex = list.indexOf(lastList.get(lastList.size() - 1));
         	 
         	 if(textStart > startPos){
         		brailleStart -= textStart - startPos;
          		textStart = startPos;
          	}
         	
         	list.setCurrent(startIndex);
         	clearViewRanges(textStart, brailleStart, list.get(endIndex), startIndex, endPos - startPos, endPos, replacementText);
         	clearListItems(firstList);         	
         	clearListItems(lastList);
         	
         	ArrayList<TextMapElement>mapList = new ArrayList<TextMapElement>(); 
         	if(readOnly(first)){
         		mapList.add(first);
         		repopulateReadOnly(mapList, startIndex);
         	}
         	else {
         		if(emptyNode && !removeFirst)
             		mapList = recreateEmptyElement(firstEl, startIndex, nodes);
             	else if(!removeFirst)
             		mapList.addAll(recreateElement(firstEl, startIndex));
         	}
         	
         	if(readOnly.size() > 0){
         		repopulateReadOnly(readOnly, startIndex + mapList.size());
         		mapList.addAll(readOnly);
         	}
         	
         	int pos = startIndex + mapList.size();
         	if(readOnly(last)){
         		mapList.add(last);
         		repopulateReadOnly(last, pos);
         	}
         	else if(!clearAll && !removeLast)
         		mapList.addAll(recreateElement(lastEl, pos));
         	
         	if(!list.empty())
         		list.setCurrent(startIndex);
         	
         	rebuildViews(mapList, startIndex, textStart, brailleStart, indexes, false);         	
         	text.refreshStyle(mapList.get(0));
         	braille.refreshStyle(mapList.get(0));
         }
    	
     	text.setCurrentElement(startPos);
     	if(!evFrame.empty())
     		manager.addUndoEvent(evFrame);
    }
    
    private void updateFirstNode(ArrayList<TextMapElement> elList, Element e, TextMapElement t, int start, int end, String replacementText){
     	if(!readOnly(t)){
     		if(start <= t.start && end >= t.end && replacementText.length() == 0){
     			clearText(t);
     			if(elList.indexOf(t) == 0){
     				emptyNode = true;
					nodes = nodeIndexes(t.n, e);
     			}
     		}
     		else if(start < t.start && end < t.end){
     			int offset = end - t.start;
     			String newText = t.getText().substring(offset);
     			Text node = (Text)t.n;
     			node.setValue(newText);
     		}
     		else {
     			if(start <= t.start)
     				start = t.start;
     			
     			int offset = start - t.start;
     			
     			String newText = t.getText().substring(0, offset) + replacementText;
     			Text node = (Text)t.n;
     			node.setValue(newText);
     		}
     	}
    }
    
    private void updateSecondNode(TextMapElement t, int start, int end, String replacedText){
    	String newText;
    	if(end >= t.end)
    		newText = "";
    	else if(end <= t.start){
    		newText = t.getText();
    	}
    	else {
    		int offset = end - t.start;
    		int startOffset = t.start - start;
    		int lineBreaks = replacedText.substring(startOffset).length() - replacedText.substring(startOffset).replaceAll("\n", "").length();
    		newText = t.getText().substring(offset - lineBreaks);
    	}
    	
		Text node = (Text)t.n;
		node.setValue(newText);
    }
    
    private ArrayList<TextMapElement> recreateEmptyElement(Element first, int startIndex, LinkedList<Integer>nodes){
    	Element e = first;
 		while(nodes.size() > 0)
     		e = (Element)e.getChild(nodes.remove());
 		
 		return recreateElement(e, startIndex);
    }
    
    private ArrayList<TextMapElement> recreateElement(Element e, int startIndex){  
    	e = replaceElement(e);
    	ArrayList<TextMapElement>textList;
    	if(e.getChildCount() == 0){
    		textList = new ArrayList<TextMapElement>();
    		Text node = new Text("");
    		e.insertChild(node, 0);
    		TextMapElement t = new TextMapElement(0, 0, node);
    		Element brl = new Element("brl");
    		Text textNode = new Text("");
    		brl.appendChild(textNode);
    		e.appendChild(brl);
    		t.brailleList.add(new BrailleMapElement(0,0, textNode));
    		textList.add(t);
    		vi.addElementToSection(list, t, startIndex);
    	}
    	else {
    		int size = repopulateRange(e, startIndex);
    		textList = getListRange(startIndex, size);
    	}
    	return textList;
    }
    
    private void clearText(TextMapElement t){
    	Text textNode = (Text)t.n;
    	textNode.setValue(""); 
    }
    
    private int clearElement(int startIndex, int endIndex){
    	int removed = 0;
    	for(int i = startIndex; i < endIndex; i++){
    		removeElement(i);
    		removed++;
    	}
    	
    	return removed;
    }
    
    private Element replaceElement(Element e){
    	Element newEl = manager.getDocument().translateElement(e);
    	ParentNode p = e.getParent();
    	p.replaceChild(e, newEl);
    	return newEl;
    }
    
    private void removeElement(int listIndex){
    	TextMapElement t = list.get(listIndex);
    	if(!readOnly(t)){
    		Element e = t.parentElement();
   			Message m = Message.createRemoveNodeMessage(listIndex, t.end - t.start);
   			manager.getDocument().updateDOM(list, m);
    		if(e.getChildCount() == 0){
    			if(e.getAttributeValue("semantics").contains("action")){
    				if(e.getParent().getChildCount() == 1)
    					e = (Element)e.getParent();
    			}
   				e.getParent().removeChild(e);
    		}
    	}
    }
    
    private void addEvents(int start, int end){
    	int index = start;
    	ArrayList<TextMapElement> elList = new ArrayList<TextMapElement>();
    	while(index < end){
    		if(!readOnly(list.get(index))){
    			Element e = getBlockElement(index);
    			ArrayList<TextMapElement>local = getBlockMapElements(index, e);
    			elList.addAll(local); 		
    			index = list.indexOf(elList.get(elList.size() - 1)) + 1;
    			addEvent(e, list.indexOf(local.get(0)), local.get(0).start, local.get(0).brailleList.getFirst().start, tree.getItemPath(), false, true, start, end);
    		}
    		else {
    			addEvent(list.get(index).parentElement(), index, list.get(index).start, list.get(index).brailleList.getFirst().start, tree.getItemPath(), true, true, start, end);
    			elList.add(list.get(index));
    			index++;
    		}		
    	}
    }
    
    private ArrayList<TextMapElement> removeElements(int start, int end){
    	int index = start;
    	ArrayList<TextMapElement> elList = new ArrayList<TextMapElement>();
    	while(index < end){
    		if(!readOnly(list.get(index))){
    			Element e = getBlockElement(index);
    			ArrayList<TextMapElement>local = getBlockMapElements(index, e);
    			elList.addAll(local); 		
    			index = list.indexOf(elList.get(elList.size() - 1)) + 1;
    		}
    		else {
    			elList.add(list.get(index));
    			index++;
    		}
    			
    	}
    	
    	for(int i = 0; i < elList.size(); i++)
    		removeElement(list.indexOf(elList.get(i)));
    	
    	clearListItems(elList);
    	return elList;
    }
    
    private void clearListItems(ArrayList<TextMapElement> elList){   
    	for(int i = 0; i < elList.size(); i++)
    		vi.remove(list, list.indexOf(elList.get(i)));
    }
    
	private void clearViewRanges(int start, int brailleStart, TextMapElement last, int index, int length, int endPos, String replacementText){
		int end = last.end;
		int brailleEnd = last.brailleList.getLast().end;
		if(endPos > end){
			brailleEnd += endPos - end;
			end = endPos;
		}
	
		text.replaceTextRange(start, (end - length) - start + replacementText.length(), "");
		braille.replaceTextRange(brailleStart, brailleEnd - brailleStart, "");
		list.shiftOffsetsFromIndex(index, -(end - start), -(brailleEnd - brailleStart));
	}
	
	private void rebuildViews(ArrayList<TextMapElement>mapList, int startIndex, int textStart, int brailleStart, ArrayList<Integer> indexes, boolean format){
		list.setCurrent(startIndex);
     	setViews(mapList,startIndex, textStart, brailleStart, format);
     	tree.rebuildTree(indexes);
	}
	private void setViews(ArrayList<TextMapElement> elList, int index, int textOffset, int brailleOffset, boolean format){
		Message m = new Message(null);
		int count = elList.size();
		
		for(int i = 0; i < count; i++){
			int brailleLength = 0;
			
			text.resetSelectionElement(m, vi, list, index, textOffset, elList.get(i), format);
			textOffset = elList.get(i).end;
			
			for(int j = 0; j < elList.get(i).brailleList.size(); j++){
				braille.resetSelectionElement(m, list, list.get(index), elList.get(i).brailleList.get(j), brailleOffset, format);
				brailleOffset = (Integer)m.getValue("brailleOffset");
				brailleLength += (Integer)m.getValue("brailleLength");
			}
				
			int textLength = (Integer)m.getValue("textLength");
			textOffset = (Integer)m.getValue("textOffset");
			list.shiftOffsetsFromIndex(index + 1, textLength, brailleLength);
			index++;
		}
	}
    
    private int repopulateRange(Element e, int index){
		ArrayList<TextMapElement> elList = 	constructMapElement(e);
		for(int i = 0; i < elList.size(); i++, index++)
			vi.addElementToSection(list, elList.get(i), index);
		
		return elList.size();
	}
    
    private void repopulateReadOnly(ArrayList<TextMapElement>elList, int index){
    	for(int i = 0; i < elList.size(); i++, index++)
    		repopulateReadOnly(elList.get(i), index);
    }
	
    private void repopulateReadOnly(TextMapElement t, int index){
    	t.setOffsets(0, 0);
    	t.brailleList.get(0).setOffsets(0, 0);
    	vi.addElementToSection(list, t, index);
    }
    
	private ArrayList<TextMapElement> constructMapElement(Element e){
		ArrayList<TextMapElement> elList = new ArrayList<TextMapElement>();
		
		for(int i = 0; i < e.getChildCount(); i++){
			if(e.getChild(i) instanceof Text){
				elList.add(new TextMapElement(e.getChild(i)));
			}
			else if(e.getChild(i) instanceof Element && ((Element)e.getChild(i)).getLocalName().equals("brl") && !isBoxLine(e)){
				for(int j = 0; j < e.getChild(i).getChildCount(); j++){
					if(e.getChild(i).getChild(j) instanceof Text){
						elList.get(elList.size() - 1).brailleList.add(new BrailleMapElement(e.getChild(i).getChild(j)));
					}
				}
			}
			else if(e.getChild(i) instanceof Element && ((Element)e.getChild(i)).getLocalName().equals("brl") && isBoxLine(e))
				elList.add(new BrlOnlyMapElement(e.getChild(i), e));
			else if(e.getChild(i) instanceof Element)
				elList.addAll(constructMapElement((Element)e.getChild(i)));
		}
		
		return elList;
	}
	
	private ArrayList<TextMapElement>getListRange(int start, int length){
		ArrayList<TextMapElement>textList = new ArrayList<TextMapElement>();
		for(int i = start; i < start + length; i++)
			textList.add(list.get(i));
		
		return textList;
	}
    
    private int getIndex(int pos){
        Message m = new Message(null);
        m.put("offset", pos);
        m.put("selection", manager.getTreeView().getSelection(list.getCurrent()));
        return list.findClosest(m, 0, list.size() - 1);
    }
    
    private Element getBlockElement(int index){
        return manager.getDocument().getParent(list.get(index).n, true);
    }
    
    private ArrayList<TextMapElement> getBlockMapElements(int index, Element el){
        return list.findTextMapElements(index, el, true);
    }
    
    private LinkedList<Integer> nodeIndexes(Node n, Element parent){
    	LinkedList<Integer> nodes = new LinkedList<Integer>();
    	ParentNode  e = n.getParent();
    	
    	while(!e.equals(parent)){
    		n = e;
    		e = e.getParent();
    		nodes.push(e.indexOf(n));
    	}
    	
    	return nodes;
    }
    
    private void addEvent(Element blockElement, int listIndex, int textStart, int brailleStart, ArrayList<Integer> treeIndexes, boolean insert, boolean format, int startPos, int endPos){
    	SelectionEvent ev = new SelectionEvent(EventTypes.Selection, blockElement, vi.getStartIndex(), listIndex, textStart, brailleStart, treeIndexes, insert, format, startPos, endPos);
    	evFrame.addEvent(ev);
    }
    
    private void pushEvent(Element blockElement, int listIndex, int textStart, int brailleStart, ArrayList<Integer> treeIndexes, boolean insert, boolean format, int startPos, int endPos){
    	SelectionEvent ev = new SelectionEvent(EventTypes.Selection, blockElement, vi.getStartIndex(), listIndex, textStart, brailleStart, treeIndexes, insert, format, startPos, endPos);
    	evFrame.addEvent(0, ev);
    }
    
    public void undoSelection(EventFrame frame){
    	evFrame = new EventFrame();
    	boolean firstBlock = true;
    	int pos = text.view.getCaretOffset();
    	TextMapElement t = null;
    	while(!frame.empty() && frame.peek().getEventType().equals(EventTypes.Selection)){
    		SelectionEvent ev = (SelectionEvent)frame.push();		
    		if(firstBlock)
    			pos = ev.getTextOffset();
    		if(!readOnly((Element)ev.getNode())){
    			if((firstBlock  && !ev.insert()) || (frame.size() == 0 && !ev.insert())){
    				Element e = getBlockElement(ev.getListIndex());
    				ArrayList<TextMapElement> maplist = getBlockMapElements(ev.getListIndex(), e);
    				TextMapElement first = maplist.get(0);
    				TextMapElement last = maplist.get(maplist.size() - 1);
    				int index = list.indexOf(first);
    				int textStart = first.start;
    				int brailleStart = first.brailleList.getFirst().start;
    				addEvent(e, index, textStart, brailleStart, tree.getItemPath(), false, ev.format(), ev.getSelectionStart(), ev.getSelectionEnd());
    				e.getParent().replaceChild(e, ev.getNode());
    				clearViewRanges(textStart, first.brailleList.getFirst().start, last, list.indexOf(first), 0, last.end, "");
    				clearListItems(maplist);
        	
    				int size = repopulateRange((Element)ev.getNode(), index);
    				maplist = getListRange(index, size);
    			if(!firstBlock && ev.getListIndex() > 0 && !readOnly(list.get(ev.getListIndex() - 1)) && list.get(ev.getListIndex() - 1).end == textStart){
    				text.insertText(textStart, "\n");
    				textStart++;
    				braille.insertText(brailleStart, "\n");
    				brailleStart++;
    				list.shiftOffsetsFromIndex(index, 1, 1);
    			}
    				rebuildViews(maplist, index, textStart, brailleStart, ev.getTreeIndex(), ev.format());
    				if(firstBlock)
    					t = maplist.get(0);
    			}
    			else
    				insertEvent(ev);
    		}
    		else
    			readOnlyEvent(ev, false);
    		
    		firstBlock = false;
    	}	
    	
    	if(evFrame.size() > 0){
    		manager.addRedoEvent(evFrame);
    		text.setCurrentElement(pos);
    		if(t != null){
    			text.refreshStyle(t);
    			braille.refreshStyle(t);
    		}
    	}
    }
    
    public void redoSelection(EventFrame frame){
    	evFrame = new EventFrame();
    	boolean firstBlock = true;
    	int pos = text.view.getCaretOffset();
    	TextMapElement t = null;
    	
    	while(!frame.empty() && frame.peek().getEventType().equals(EventTypes.Selection)){
    		SelectionEvent ev = (SelectionEvent)frame.pop();
    		if(frame.empty())
    			pos = ev.getTextOffset();
    		if(!readOnly((Element)ev.getNode())){
    			Element e = getBlockElement(ev.getListIndex());
        		ArrayList<TextMapElement> maplist = getBlockMapElements(ev.getListIndex(), e);
        		TextMapElement first = maplist.get(0);
        		TextMapElement last = maplist.get(maplist.size() - 1);
        
        		int index = list.indexOf(first);
        		int textStart = first.start;
        		int brailleStart = first.brailleList.getFirst().start;
        		pushEvent(e, index, textStart, brailleStart, tree.getItemPath(), ev.insert(), ev.format(), ev.getSelectionStart(), ev.getSelectionEnd());
        		if(hasLinesBefore((Element)ev.getNode()) && ev.format()){
        			textStart -= getLinesBefore(e);
        			brailleStart -= getLinesBefore(e);
        		}
        		
        		int linesAfter = 0;
        		if(hasLinesAfter((Element)ev.getNode()) && ev.format() && ev.getSelectionEnd() > last.end){
        			linesAfter = getLinesAfter(e);
        		}
        		
        		if(!(firstBlock || frame.empty()) || (!firstBlock && list.indexOf(last) < list.size() - 1 && !readOnly(list.get(list.indexOf(last) + 1))))
        			clearViewRanges(textStart, brailleStart, last, list.indexOf(first), 0, last.end + 1 + linesAfter, "");
        		else
        			clearViewRanges(textStart, brailleStart, last, list.indexOf(first), 0, last.end + linesAfter, "");
        		
        		
        		clearListItems(maplist);
        		if((firstBlock && !ev.insert()) || (frame.empty() && !ev.insert())){
        			e.getParent().replaceChild(e, ev.getNode());
        			int size = repopulateRange((Element)ev.getNode(), index);
        			maplist = getListRange(index, size);
            		rebuildViews(maplist, index, textStart, brailleStart, ev.getTreeIndex(), false);

                	if(frame.size() == 0)
                		t = maplist.get(0);
        		}
        		else {
        			e.getParent().removeChild(e);
        		}
    		}
    		else
    			readOnlyEvent(ev, true);
    		
        	firstBlock = false;
    	}	   	
    	if(evFrame.size() > 0){
    		manager.addUndoEvent(evFrame);
    		text.setCurrentElement(pos);
    		if(t != null){
    			text.refreshStyle(t);
    			braille.refreshStyle(t);
    		}
    	}
    }
    
    private void insertEvent(SelectionEvent ev){
    	Element e = (Element)ev.getNode();
    	int index = ev.getListIndex();
    	int textStart = ev.getTextOffset();
    	int brailleStart = ev.getBrailleOffset();
    	ev.getParent().insertChild(ev.getNode(), ev.getParentIndex());
    	addEvent(e, index, textStart, brailleStart, tree.getItemPath(), true, ev.format(), ev.getSelectionStart(), ev.getSelectionEnd());
    	int size = repopulateRange(e, index);
		ArrayList<TextMapElement>maplist = getListRange(index, size);
		
		if(ev.getListIndex() > 0 && !readOnly(list.get(ev.getListIndex() - 1)) ){
			text.insertText(list.get(ev.getListIndex() - 1).end, "\n");
		    braille.insertText(list.get(ev.getListIndex() - 1).brailleList.getLast().end, "\n");
		    list.shiftOffsetsFromIndex(index + maplist.size(), 1, 1);
		}
		
		if(ev.format() && hasLinesBefore(e)){
			textStart -= getLinesBefore(e);
			brailleStart -= getLinesBefore(e);
		}
		rebuildViews(maplist, index, textStart, brailleStart, ev.getTreeIndex(), ev.format());
	
    }
    
    private void readOnlyEvent(SelectionEvent ev, boolean push){
    	ArrayList<TextMapElement> maplist = new ArrayList<TextMapElement>();
    	TextMapElement t = list.get(ev.getListIndex());
    	maplist.add(t);
    	if(push)
    		pushEvent(t.parentElement(), ev.getListIndex(), ev.getTextOffset(), ev.getBrailleOffset(), tree.getItemPath(), false, ev.format(), ev.getSelectionStart(), ev.getSelectionEnd());
    	else
    		addEvent(t.parentElement(), ev.getListIndex(), ev.getTextOffset(), ev.getBrailleOffset(), tree.getItemPath(), false, ev.format(), ev.getSelectionStart(), ev.getSelectionEnd());
    	
    	int start = ev.getTextOffset();
    	int brailleStart = ev.getBrailleOffset();
    	clearViewRanges(t.start, t.brailleList.getFirst().start, t, ev.getListIndex(), 0, t.end, "");
    	t.setOffsets(0, 0);
    	t.brailleList.getFirst().setOffsets(0, 0);
    	rebuildViews(maplist, ev.getListIndex(), start, brailleStart, ev.getTreeIndex(), false);
    }
    
    private boolean beforeStart(int start, ArrayList<TextMapElement>elList){
    	if(start < elList.get(0).start)
    		return true;
    	
    	return false;
    }
    
    private boolean afterEnd(int end, ArrayList<TextMapElement>elList){
    	if(end > elList.get(elList.size() - 1).end)
    		return true;
    	
    	return false;
    }
    
    private boolean hasLinesBefore(Element e){
    	String key = manager.getStyleTable().getKeyFromAttribute(e);
    	return manager.getStyleTable().get(key).contains(StylesType.linesBefore);
    }
    
    private boolean hasLinesAfter(Element e){
    	String key = manager.getStyleTable().getKeyFromAttribute(e);
    	return manager.getStyleTable().get(key).contains(StylesType.linesAfter);
    }
    
    private int getLinesBefore(Element e){
    	String key = manager.getStyleTable().getKeyFromAttribute(e);
    	return Integer.valueOf((String)manager.getStyleTable().get(key).get(StylesType.linesBefore));
    }
    
    private int getLinesAfter(Element e){
    	String key = manager.getStyleTable().getKeyFromAttribute(e);
    	return Integer.valueOf((String)manager.getStyleTable().get(key).get(StylesType.linesAfter));
    }
}