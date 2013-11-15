package chat;

import hypeerweb.Node;
import java.awt.BorderLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * List all nodes in HyPeerWeb, categorized by
 * their InceptionSegment
 * @author isaac
 */
public class ListTab extends JPanel{
	private static ChatClient container;
	private JTable table;
	private JComboBox segmentBox;
	
	public ListTab(ChatClient container) {
		super(new BorderLayout());
		
		JPanel segmentPanel = new JPanel();
		JLabel label = new JLabel("Segment:");
		segmentBox = new JComboBox(new segmentModel());
		segmentBox.setSize(50, 100);
		segmentPanel.add(label);
		segmentPanel.add(segmentBox);
		this.add(segmentPanel, BorderLayout.NORTH);
		
		ListTab.container = container;
        table = new JTable(new MyTableModel());
        table.setFillsViewportHeight(true);
		
		ListSelectionModel lsm = table.getSelectionModel();
		lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lsm.addListSelectionListener(new selectionHandler());
		
		TableColumn col; 
        DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer();    
        dtcr.setHorizontalAlignment(SwingConstants.CENTER);  
		for(int i = 0; i < table.getModel().getColumnCount(); i++){
			col = table.getColumnModel().getColumn(i); 
			col.setCellRenderer(dtcr);
		}
		
		table.getColumnModel().getColumn(0).setPreferredWidth(58);
		table.getColumnModel().getColumn(1).setPreferredWidth(44);
		table.getColumnModel().getColumn(2).setPreferredWidth(45);
		table.getColumnModel().getColumn(3).setPreferredWidth(85);
		table.getColumnModel().getColumn(4).setPreferredWidth(50);
		table.getColumnModel().getColumn(5).setPreferredWidth(50);
		table.getColumnModel().getColumn(6).setPreferredWidth(25);
		table.getColumnModel().getColumn(7).setPreferredWidth(25);
		table.getColumnModel().getColumn(8).setPreferredWidth(25);
		
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
	}
	
	public void draw(){
		table.repaint();
		segmentBox.repaint();
	}

	private static class MyTableModel implements TableModel {
		private String[] columnNames = {"Segment",
										"WebID",
                                        "Height",
                                        "Ns",
                                        "SNs",
                                        "ISNs",
										"F",
										"SF",
										"ISF"};
		
		public MyTableModel() {}
		
		@Override
		public int getRowCount() {
			return container.nodeList.list.size();
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public String getColumnName(int columnIndex) {
			return columnNames[columnIndex];
		}

		@Override
		public Class getColumnClass(int columnIndex) {
			return String.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			String result = "";
			Node node = container.nodeList.getNodes().get(rowIndex);
			switch(columnIndex){
				case 0:
					result = "1";
					break;
				case 1:
					result += node.getWebId();
					break;
				case 2:
					result += node.getHeight();
					break;
				case 3:
					for(Node n : node.getNeighbors())
						result += n.getWebId() + " ";
					break;
				case 4:
					for(Node n : node.getSurrogateNeighbors())
						result += n.getWebId() + " ";
					break;
				case 5:
					for(Node n : node.getInverseSurrogateNeighbors())
						result += n.getWebId() + " ";
					break;
				case 6:
					if(node.getFold() != null)
						result += node.getFold().getWebId();
					break;
				case 7:
					if(node.getSurrogateFold() != null)
						result += node.getSurrogateFold().getWebId();
					break;
				case 8:
					if(node.getInverseSurrogateFold() != null)
						result += node.getInverseSurrogateFold().getWebId();
					break;
			}
			return result;
		}
		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {	
		}
		@Override
		public void addTableModelListener(TableModelListener l) {
		}
		@Override
		public void removeTableModelListener(TableModelListener l) {
		}
	}
	
	private static class segmentModel implements ComboBoxModel{
		//temporary
		private String[] segments = {"All", "1", "2", "3"};
		
		int selection = -1;//-1 for all segments
		
		@Override
		public void setSelectedItem(Object anItem) {
			if(anItem == "All")
				selection = -1;
			else
				selection = Integer.parseInt((String) anItem);
		}

		@Override
		public Object getSelectedItem() {
			if(selection == -1)
				return "All";
			else
				return selection;
		}

		@Override
		public int getSize() {
			//get number of segments
			return segments.length;
		}

		@Override
		public Object getElementAt(int index) {
			return segments[index];
		}

		@Override
		public void addListDataListener(ListDataListener l) {
		}

		@Override
		public void removeListDataListener(ListDataListener l) {
		}
		
	}
	
	private static class selectionHandler implements ListSelectionListener{

		@Override
		public void valueChanged(ListSelectionEvent e) {
			ListSelectionModel lsm = (ListSelectionModel)e.getSource();
			int index = lsm.getMinSelectionIndex();
			Node n = container.nodeList.getNodes().get(index);
			container.setSelectedNode(n);	
		}
	}
}
