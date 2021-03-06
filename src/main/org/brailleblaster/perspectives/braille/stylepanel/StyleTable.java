package org.brailleblaster.perspectives.braille.stylepanel;

import java.text.Collator;
import java.util.Locale;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Element;

import org.brailleblaster.localization.LocaleHandler;
import org.brailleblaster.perspectives.braille.document.BBSemanticsTable.StylesType;
import org.brailleblaster.perspectives.braille.mapping.elements.BrlOnlyMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class StyleTable {
	private final static int LEFT_MARGIN = 0;
	private final static int RIGHT_MARGIN = 15;
	private final static int TOP_MARGIN = 50;
	private final static int BOTTOM_MARGIN = 100;
	
	private Group group;
	private Table t;
	private Font initialFont;
	private boolean traverseFired;
	private boolean removeStyleSet;
	
	private StyleManager sm;
	private Button restoreButton, newButton, editButton, deleteButton, applyButton;
	private SelectionAdapter applyStyle, removeStyle;
	
	public StyleTable(final StyleManager sm, SashForm sash){
		LocaleHandler lh = new LocaleHandler();
		this.sm = sm;
		this.group = new Group(sash, SWT.FILL | SWT.BORDER);
		setLayoutData(this.group, LEFT_MARGIN, RIGHT_MARGIN, TOP_MARGIN, BOTTOM_MARGIN);
		this.group.setLayout(new FormLayout());
		this.group.setVisible(false);
		
		Listener resizeListener = new Listener(){
			@Override
			public void handleEvent(Event e) {
				checkFontSize((Button)e.widget);
			}		
		};
		
		restoreButton = new Button(this.group, SWT.CHECK);
		restoreButton.setText(lh.localValue("restore"));
		setLayoutData(restoreButton, 1, 100, 0, 5);
		
		this.t = new Table(this.group, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		setLayoutData(this.t, 0, 100, 5, 90);
	    
		TableColumn tc1 = new TableColumn(this.t, SWT.CENTER);
		tc1.setWidth(0);
		tc1.setResizable(false);
		
		final TableColumn tc2 = new TableColumn(this.t, SWT.CENTER);   
	    tc2.setText(lh.localValue("styles"));
	   
	    this.t.setLinesVisible(true);
	    this.t.setHeaderVisible(true);	
	   	
	    newButton = new Button(this.group, SWT.NONE);
	    newButton.setText(lh.localValue("new"));
	    setLayoutData(newButton, 0, 25, 90, 100);
	    initialFont = newButton.getFont();
	    newButton.addListener(SWT.Resize, resizeListener);
	    
	    editButton = new Button(this.group, SWT.NONE);
	   	editButton.setText(lh.localValue("edit"));
	   	setLayoutData(editButton, 25, 50, 90, 100);
	   	editButton.addListener(SWT.Resize, resizeListener);
	   	
	   	deleteButton = new Button(this.group, SWT.NONE);
	   	deleteButton.setText(lh.localValue("delete"));
	   	setLayoutData(deleteButton, 50, 75, 90, 100);
	   	deleteButton.addListener(SWT.Resize, resizeListener);
	   	
	    applyButton = new Button(this.group, SWT.NONE);
	    applyButton.setText(lh.localValue("apply"));
	    setLayoutData(applyButton, 75, 100, 90, 100);
	    applyButton.addListener(SWT.Resize, resizeListener);
	    
	    group.pack();
	    Control [] tabList = {t, newButton, editButton, deleteButton, applyButton, restoreButton};
	    group.setTabList(tabList);
	    
	    tc2.setWidth(group.getClientArea().width);
	   
	    t.getHorizontalBar().dispose();
		
		this.group.addListener(SWT.Resize, new Listener(){
			@Override
			public void handleEvent(Event e) {
				 tc2.setWidth(group.getClientArea().width);
			}
			
		});
		
		this.t.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e) {
				if(!traverseFired && Character.isLetter(e.character)){
					int loc = searchTree(e.character);
					if(loc != -1)
						setSelection(loc);
				}
				else if(e.keyCode == SWT.CR){
					sm.apply((String) t.getSelection()[0].getData());
					traverseFired = false;
				}
				else
					traverseFired = false;
			}
		});
		
		t.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkToggle();
			}		
		});
		
		applyStyle = new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				sm.apply((String) t.getSelection()[0].getData());
				checkToggle();
			}
		};
		
		removeStyle = new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				sm.remove((String) t.getSelection()[0].getData());
				checkToggle();
			}
		};
	
	   	populateTable(sm.getKeySet());
	   	initializeListeners();
	   	traverseFired = false;
	    sort();
	}
	
	private void sort(){
		 TableItem[] items = t.getItems();
         Collator collator = Collator.getInstance(Locale.getDefault());
         for (int i = 1; i < items.length; i++) {
           String value1 = items[i].getText(1);
           for (int j = 0; j < i; j++) {
             String value2 = items[j].getText(1);
             if (collator.compare(value1, value2) < 0) {
               String[] values = { items[i].getText(0), items[i].getText(1) };
               String data = (String)items[i].getData();
               items[i].dispose();
               TableItem item = new TableItem(t, SWT.NONE, j);
               item.setData(data);
               item.setText(values);
               items = t.getItems();
               break;
             }
           }
         }
	}
	
	private void initializeListeners(){	
		restoreButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				LocaleHandler lh = new LocaleHandler();
				MessageBox mb = new MessageBox(group.getShell(), SWT.OK | SWT.CANCEL);
				mb.setText(lh.localValue("restoreMB"));
				mb.setMessage(lh.localValue("restoreMBMessage"));
				int choice = mb.open();
				
				if(choice == SWT.OK){
					sm.restoreDefaults();
					restoreButton.setSelection(false);
				}
				else
					restoreButton.setSelection(false);
			}	
		});
		
		newButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				sm.openNewStyleTable();
			}
		});
		
		editButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {	
				sm.openEditStyle();
			}		
		});
		
		deleteButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteStyle();
			}			
		});
		

		applyButton.addSelectionListener(applyStyle);
		removeStyleSet = false;

		
		t.addTraverseListener(new TraverseListener(){
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.stateMask == SWT.MOD1 & e.keyCode == SWT.TAB){
					sm.dm.getText().view.setFocus();
					e.doit = false;
				}
				else if(e.stateMask == SWT.MOD2 && e.keyCode == SWT.TAB){
					sm.dm.getTreeView().getTree().setFocus();
					e.doit = false;
				}
			}		
		});
		
		group.addTraverseListener(new TraverseListener(){
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(groupHasFocus()){
					if(e.stateMask == SWT.MOD3 && e.keyCode == 'n')
						sm.openNewStyleTable();
					else if(e.stateMask == SWT.MOD3 && e.keyCode == 'e')
						sm.openEditStyle();
					else if(e.stateMask == SWT.MOD3 && e.keyCode == 'd'){
						int index = t.getSelectionIndex();
						deleteStyle();
						//selection must be reset or if user hits delete again an error occurs
						if(index < t.getItemCount())
							t.setSelection(index);
						else if(t.getItemCount() < 0)
							t.setSelection(t.getItemCount() -  1);
						else
							deleteButton.setFocus();
					}
					else if(e.stateMask == SWT.MOD3 && e.keyCode == 'y')
						sm.apply((String) t.getSelection()[0].getData());
					else if(e.keyCode == SWT.ESC)
						sm.closeTable();
					
					
					e.doit = false;
					traverseFired = true;
				}
			}		
		});
	}
	
	private void setLayoutData(Control c, int left, int right, int top, int bottom){
		FormData location = new FormData();
		
		location.left = new FormAttachment(left);
		location.right = new FormAttachment(right);
		location.top = new FormAttachment(top);
		location.bottom = new FormAttachment(bottom);
		
		c.setLayoutData(location);
	}
	
	private boolean groupHasFocus(){
		if(t.isFocusControl() || newButton.isFocusControl() || editButton.isFocusControl() || deleteButton.isFocusControl() || applyButton.isFocusControl() || restoreButton.isFocusControl())
			return true;
		else
			return false;
	}
	
	public void showTable(TextMapElement item){
		group.setVisible(true);
		t.setFocus();
		if(item != null){
			setSelection(item);
		}
		else
			t.setSelection(0);
	}
	
	public void hideTable(){
		group.setVisible(false);
	}
	
	private int searchTree(String text){
		for(int i = 0; i < t.getItemCount(); i++){
			if(t.getItem(i).getData().equals(text)){
				return i;
			}
		}
		return 0;
	}
	
	private int searchTree(char c){
		for(int i = 0; i < t.getItemCount(); i++){
			if(t.getItem(i).getText(1).charAt(0) == c)
				return i;
		}
		return -1;
	}
	
	public void setSelection(TextMapElement item){
		Element parent = (Element)item.n.getParent();
		String text;
		if(item instanceof BrlOnlyMapElement && isBoxLine(item.parentElement())){
			text = "boxline";
		}
		else {
			while(sm.getSemanticsTable().getSemanticTypeFromAttribute(parent) == null || sm.getSemanticsTable().getSemanticTypeFromAttribute(parent).equals("action")){
				parent = (Element)parent.getParent();
			}
			text = sm.getSemanticsTable().getKeyFromAttribute(parent);
			
			if(text.contains("local_")){
				String [] tokens = text.split("_");
				text = tokens[1];
			}
		}
		
		setSelection(searchTree(text));
	}
	
	public void setSelection(String style){
		for(int i = 0; i < t.getItemCount(); i++){
			if(t.getItem(i).getData().equals(style)){
				setSelection(i);
				break;
			}
		}
	}
	
	private void setSelection(int index){
		t.setSelection(index);
		checkToggle();
	}
	

    private void populateTable(Set<String> list){ 
    	StylesType perefferedStyle=StylesType.valueOf("name");
    	String perefferedName;
    	
    	for(String s : list){
    		if(!s.contains("local_") && !s.equals("document") && !s.equals("italicx") && !s.equals("boldx") &&
    				!s.equals("topBox") && !s.equals("middleBox") & !s.equals("bottomBox") && !s.equals("fullBox") 
    				&&  !s.equals("underlinex") && !s.equals("none"))
    		{
    			if(sm.getSemanticsTable().get(s).contains(perefferedStyle)){
    				perefferedName=(String) sm.getSemanticsTable().get(s).get(perefferedStyle);
    				addTableItem(perefferedName,s);
    			}
    			else{
    				addTableItem(s,s);
    			}
    		}
    	}  	
    }
    
    private boolean deleteStyle(){
    	LocaleHandler lh = new LocaleHandler();
    	MessageBox mb = new MessageBox(group.getShell(), SWT.OK | SWT.CANCEL);
		mb.setText(lh.localValue("deleteMB"));
		mb.setMessage(lh.localValue("deleteMBMessage"));
		
		int open = mb.open();
		
		if(open == SWT.OK){
			sm.deleteStyle((String)t.getSelection()[0].getData());
			t.remove(t.getSelectionIndex());
			return true;
		}
		else
			return false;
    }
    
    public void resetTable(String configFile){
    	t.removeAll();
    	populateTable(sm.getKeySet());
    	sort();
    }
    
    private void checkFontSize(Button b){
    	Font newFont = (Font)b.getData();
    	
    	b.setFont(initialFont);
    	int charWidth = getFontWidth(b);
    	int stringWidth = b.getText().length() * charWidth;
    	FontData[] fontData = b.getFont().getFontData();
    	 
    	int Ssize = fontData[0].getHeight();
    	if(stringWidth > b.getBounds().width){
    		while(stringWidth > b.getBounds().width && Ssize > 0){
    			Ssize = fontData[0].getHeight() - 1;
    			fontData[0].setHeight(Ssize);
    			if(newFont != null && !newFont.isDisposed())
    				newFont.dispose();
    			newFont = new Font(Display.getCurrent(), fontData[0]);
    			b.setFont(newFont);
    			b.setData(newFont);
    			charWidth = getFontWidth(b);
    			stringWidth = b.getText().length() * charWidth;
    		}
    	}
    }
    
    protected int getFontWidth(Button b){
		GC gc = new GC(b);
		FontMetrics fm = gc.getFontMetrics();
		gc.dispose();
		return fm.getAverageCharWidth();
	}
    
    private void addTableItem(String item,String originalName){
    	TableItem tItem = new TableItem(t, SWT.CENTER);
    	tItem.setText(new String[]{"", item});
    	tItem.setData(originalName);
    }
    
    public boolean isVisible(){
        if(!t.isDisposed() && t.isVisible())
            return true;
        else
            return false;
    }
    
    public Table getTable(){
    	return t;
    }
    
    public Group getGroup(){
    	return group;
    }
    
    protected void dispose(){
    	t.dispose();
    	disposeFont(newButton);
    	disposeFont(editButton);
    	disposeFont(deleteButton);
    	disposeFont(applyButton);
    	group.dispose();  	
    }
    
    private void disposeFont(Button b){
    	Font f = (Font)b.getData();
    	if(f != null && !f.isDisposed())
    		f.dispose();
    }
    
    private Element getParent(TextMapElement item){
    	Element parent = (Element)item.n.getParent();
		while(sm.getSemanticsTable().getSemanticTypeFromAttribute(parent) == null || sm.getSemanticsTable().getSemanticTypeFromAttribute(parent).equals("action")){
			parent = (Element)parent.getParent();
		}
		
		return parent;
    }
    
    private void checkToggle(){
    	TextMapElement textElement = sm.getCurrentItem();
    	if(textElement != null && t.getSelection().length > 0){
    		Element parent = getParent(textElement);
    	
    		if(t.getSelection()[0].getData().equals(sm.getSemanticsTable().getKeyFromAttribute(parent)) || (t.getSelection()[0].getData().equals("boxline") && isBoxLine(parent)))
    			toggleApplyButton(true);
    		else
    			toggleApplyButton(false);
    	}
    	else
    		toggleApplyButton(false);
    }
    
    private void toggleApplyButton(boolean match){
    	LocaleHandler lh = new LocaleHandler();
    	if(match && !removeStyleSet){
    		applyButton.setText(lh.localValue("remove"));
    		applyButton.removeSelectionListener(applyStyle);
    		applyButton.addSelectionListener(removeStyle);
    		removeStyleSet = true;
    	}	
    	else if(!match && removeStyleSet) {
    		applyButton.setText(lh.localValue("apply"));
    		applyButton.removeSelectionListener(removeStyle);
    		applyButton.addSelectionListener(applyStyle);
    		removeStyleSet = false;
    	}
    }
    
    private boolean isBoxLine(Element e){
		if(checkSemanticsAttribute(e, "boxline") || checkSemanticsAttribute(e, "topBox") || checkSemanticsAttribute(e, "middleBox") 
				|| checkSemanticsAttribute(e, "bottomBox") || checkSemanticsAttribute(e, "fullBox")) 
			return true;
		else
			return false;
	}
	
	private boolean checkSemanticsAttribute(Element e, String value){
		Attribute atr = e.getAttribute("semantics");
		
		if(atr == null || !atr.getValue().contains(value))
			return false;
		
		return true;
	}
}
