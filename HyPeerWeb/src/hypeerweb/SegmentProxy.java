package hypeerweb;

import communicator.Command;
import communicator.Communicator;
import communicator.NodeListener;
import communicator.RemoteAddress;
import hypeerweb.visitors.AbstractVisitor;
import java.io.ObjectStreamException;
import java.util.ArrayList;

/**
 * Oh goodness. I can't believe I'm doing this
 * @author isaac
 */
public class SegmentProxy<T> extends Segment{
	private final RemoteAddress raddr;
	
	public SegmentProxy(Segment seg){
		super(seg.dbname, -1, seg.webID, seg.height);
		L = new LinksProxy(seg.UID);
		raddr = new RemoteAddress(seg.UID);
	}

	//NODE OPERATIONS
	@Override
	protected void addChild(Node child, NodeListener listener) {
		request("addChild", new String[]{Node.className, NodeListener.className}, new Object[]{child, listener}, false);
	}
	@Override
	protected void replaceNode(Node toReplace, int newHeight, NodeListener listener) {
		System.err.println("Node.replaceNode should not be called from a Proxy");
	}
	@Override
	protected void disconnectNode(int newHeight, NodeListener listener) {
		request("disconnectNode", new String[] {"int",NodeListener.className}, new Object[]{newHeight, listener}, true);
	}
	@Override
	protected Node findValidNode(Criteria.Type x, int levels, boolean recursive) {
		return (Node) request("findValidNode", new String[] {"hypeerweb.Criteria$Type","int","boolean"}, new Object[]{x, levels, recursive}, true);
	}
	@Override
	protected Node findInsertionNode() {
		return (Node) request("findInsertionNode");
	}
	@Override
	protected Node findDisconnectNode() {
		return (Node) request("findDisconnectNode");
	}
	@Override
    public void accept(AbstractVisitor p0){
		request("accept", new String[]{"hypeerweb.visitors.AbstractVisitor"}, new Object[]{p0}, false);
    }
	
	//GETTERS
	@Override
    public ArrayList getTreeChildren(){
		return (ArrayList<Node>) request("getTreeChildren");
    }
	@Override
    public Node getTreeParent(){
		return (Node) request("getTreeParent");
    }
	@Override
    public Node getCloserNode(int p0, boolean p1){
		return (Node) request("getCloserNode", new String[]{"int", "boolean"}, new Object[]{p0, p1}, true);
    }
	@Override
	public Object getData(String key) {
		return request("getData", new String[] {"java.lang.String"}, new Object[] {key}, true);
	}
	@Override
	public Attributes getAllData(){
		return (Attributes) request("getAllData");
	}
	@Override
	protected FoldState getFoldState() {
		return (FoldState) request("getFoldState");
	}
	@Override
	public Node getParent() {
		return (Node) request("getParent");
	}
	
	//SETTERS
	@Override
	public void setWebID(int id) {
		webID = id;
		request("setWebID", new String[] {"int"}, new Object[] {id}, false);
	}
	@Override
	protected void setHeight(int h) {
		height = h;
		request("setHeight", new String[] {"int"}, new Object[] {h}, false);
	}
	@Override
	protected void setFoldState(FoldState state) {
		request("setFoldState", new String[] {"hypeerweb.Node$FoldState"}, new Object[] {state}, false);
	}
	@Override
	public void setData(String key, Object val) {
		request("setData", new String[] {"java.lang.String", "java.lang.Object"}, new Object[] {key, val}, false);
	}
	
	//NETWORKING
	@Override
	public Segment getHostSegment(){
		return (Segment) request("getHostSegment");
	}
	@Override
	public NodeCache convertToCached(){
		return (NodeCache) request("convertToCached");
	}
	@Override
	public void executeRemotely(NodeListener listener) {
		request("executeRemotely", new String[] {NodeListener.className}, new Object[] {listener}, false);
	}
	@Override
	public RemoteAddress getAddress(){
		return raddr;
	}
	
	private Object request(String name){
		return request(name, null, null, true);
	}
	private Object request(String name, String[] paramTypes, Object[] paramVals, boolean sync){
		Command command = new Command(Node.className, name, paramTypes, paramVals);
		return Communicator.request(raddr, command, sync);
	}
	
	//HYPEERWEB SPECIFIC
	@Override
	public int getSegmentSize(){
		return (int) requestSeg("getSegmentSize");
	}
	@Override
	public boolean isSegmentEmpty(){
		return (boolean) requestSeg("isSegmentEmpty");
	}
	@Override
	public void getNode(int p1, boolean p2, NodeListener p3){
		requestSeg("getNode", new String[] {"int", "boolean", NodeListener.className}, new Object[] {p1, p2, p3}, false);
	}
	
	private Object requestSeg(String name){
		return requestSeg(name, null, null, true);
	}
	private Object requestSeg(String name, String[] paramTypes, Object[] paramVals, boolean sync){
		Command command = new Command(className, name, paramTypes, paramVals);
		return Communicator.request(raddr, command, sync);
	}
	
	
	@Override
	public Object writeReplace() throws ObjectStreamException {
		return this;
	}
	@Override
	public Object readResolve() throws ObjectStreamException {
		if (raddr.onSameMachineAs(Communicator.getAddress()))
			return Communicator.resolveId(Segment.class, raddr.UID);
		return this;
	}
}
