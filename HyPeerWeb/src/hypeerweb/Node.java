package hypeerweb;

import java.util.ArrayList;
import validator.NodeInterface;

/**
 * The Node class
 * @author Guy
 */
public class Node implements NodeInterface{
	//NODE ATTRIBUTES	
	private boolean hasChild;
	protected int webID;
	protected int height;
	protected Node fold = null;
	protected Node surrogateFold = null;
	protected Node inverseSurrogateFold = null;
	protected ArrayList<Node> neighbors = new ArrayList();
	protected ArrayList<Node> surrogateNeighbors = new ArrayList();
	protected ArrayList<Node> inverseSurrogateNeighbors = new ArrayList();
	//Hash code prime
	private static long prime = Long.parseLong("2654435761");

	//CONSTRUCTORS
	/**
	 * Create a Node with only a WebID
	 *
	 * @param id The WebID of the Node
	 */
	public Node(int id, int height) {
		webID = id;
		this.height = height;
	}

	/**
	 * Create a Node with all of its data
	 *
	 * @param id The WebID of the Node
	 * @param Height The Height of the Node
	 * @param Fold The Fold of the Node
	 * @param sFold The Surrogate Fold of the Node
	 * @param isFold The Inverse Surrogate Fold of the Node
	 * @param Neighbors An ArrayList containing the Neighbors of the Node
	 * @param sNeighbors An ArrayList containing the Surrogate Neighbors of the
	 * Node
	 * @param isNeighbors An ArrayList containing the Inverse Surrogate
	 * Neighbors of the Node
	 */
	public Node(int id, int Height, Node Fold, Node sFold, Node isFold,
			ArrayList<Node> Neighbors, ArrayList<Node> sNeighbors,
			ArrayList<Node> isNeighbors) {
		webID = id;
		height = Height;
		fold = Fold;
		surrogateFold = sFold;
		inverseSurrogateFold = isFold;

		if (Neighbors != null) {
			neighbors = Neighbors;
		}
		if (sNeighbors != null) {
			surrogateNeighbors = sNeighbors;
		}
		if (isNeighbors != null) {
			inverseSurrogateNeighbors = isNeighbors;
		}
	}

	/**
	 * Adds a child node to the current one
	 * @param db the Database associated with the HyPeerWeb
	 * @return the new child node; null if the node couldn't be added
	 */
	public Node addChild(Database db){
		//Get new height and child's WebID
		int childHeight = height+1,
			childWebID = 1;
		for (int i=1; i<childHeight; i++)
			childWebID <<= 1;
		childWebID |= webID;
		Node child = new Node(childWebID, childHeight);
		
		//Set neighbours (Guy)
		NeighborDatabaseChanges ndc = new NeighborDatabaseChanges();
		//child neighbors
		for (Node n: inverseSurrogateNeighbors)
			ndc.updateDirect(child, n);
		//adds a neighbor of parent as a surrogate neighbor of child if nieghbor is childless
		//and makes child an isn of neighbor
		for(Node n: neighbors){
			if (!n.hasChild){ 
				ndc.updateSurrogate(child, n);
				ndc.updateInverse(n, child);
			}
		}
		ndc.updateDirect(this, child);
		ndc.updateDirect(child, this);
		
		//Set folds (Brian/Isaac)
		FoldDatabaseChanges fdc = new FoldDatabaseChanges();
		if (inverseSurrogateFold != null){
			//Stable-state fold references
			fdc.updateDirect(child, inverseSurrogateFold);
			fdc.updateDirect(inverseSurrogateFold, child);
			//Remove surrogate references
			fdc.updateSurrogate(inverseSurrogateFold, null);
			fdc.updateInverse(this, null);
		}
		else{
			//Update reflexive folds
			fdc.updateDirect(child, fold);
			fdc.updateDirect(fold, child);
			//Insert surrogates for non-existant node
			fdc.updateSurrogate(this, fold);
			fdc.updateInverse(fold, this);
			//Remove stable state reference
			fdc.updateDirect(this, null);
		}
		
		//Attempt to add the node to the database
		//If it fails, we cannot proceed
		{
			db.beginCommit();
			//Create the child node
			db.addNode(child);
			//Set neighbors and folds
			ndc.commitToDatabase(db);
			fdc.commitToDatabase(db);
			//Commit changes to database
			if (!db.endCommit())
				return null;
		}
		
		//Add the node to the Java structure
		{
			//Update parent
			this.height = childHeight;
			this.hasChild = true;
			//Update neighbors and folds
			ndc.commitToHyPeerWeb();
			fdc.commitToHyPeerWeb();
			return child;
		}
		
	}
	
	/**
	 * Finds and returns the node whose WebID is closest to the given long
	 * Assumed to always start with the node with WebID of zero
	 * @param index The value to get as close as possible to
	 * @author John
	 */
	public Node searchForNode(long index){
		long closeness = index & webID, c;
		for (int i=0; i < neighbors.size(); i++){
			c = index & neighbors.get(i).webID;
			if (c > closeness)
				return neighbors.get(i).searchForNode(index);
		}
		return this;
	}
	
	/**
	 * Finds the closest valid insertion point (the parent
	 * of the child to add) from a starting node
	 * @return the parent of the child to add
	 * @author josh
	 */
	public Node findInsertionNode() {

		Node result = findInsertionNode(this, 2);

		if (result == null) {
			return this;
		}

		return result;
	}
	private Node findInsertionNode(Node original, int times) {
		if (surrogateFold != null) {
			return surrogateFold;
		}

		if (surrogateNeighbors != null && !surrogateNeighbors.isEmpty()) {
			return surrogateNeighbors.get(0);
		}

		for (Node n : neighbors) {
			if (n != original && n.height < height) {
				return n;
			}
		}

		if (times == 0) {
			return null;
		}

		for (Node n : neighbors) {

			if (n == original) {
				continue;
			}

			Node result = n.findInsertionNode(this, times - 1);

			if (result != null) {
				return result;
			}
		}

		return null;
	}
	
	//EN-MASSE DATABASE CHANGE HANDLING
	/**
	 * Sub-Class to keep track of Fold updates
	 * @author isaac
	 */
	private static class DatabaseChanges{
		//Valid types of changes
		protected enum NodeUpdateType{
			DIRECT, SURROGATE, INVERSE
		}
		//List of changes
		protected ArrayList<NodeUpdate> updates;
		//Holds all change information
		protected class NodeUpdate{
			public NodeUpdateType type;
			public Node node;
			public Node value;
			public NodeUpdate(NodeUpdateType type, Node node, Node value){
				this.type = type;
				this.node = node;
				this.value = value;
			}
		}
		//constructor
		public DatabaseChanges(){
			updates = new ArrayList<>();
		}
		//add updates
		public void updateDirect(Node node, Node value){
			updates.add(new NodeUpdate(NodeUpdateType.DIRECT, node, value));
		}
		public void updateSurrogate(Node node, Node value){
			updates.add(new NodeUpdate(NodeUpdateType.SURROGATE, node, value));
		}
		public void updateInverse(Node node, Node value){
			updates.add(new NodeUpdate(NodeUpdateType.INVERSE, node, value));
		}
	}
	/**
	 * Interface for implementing node-specific commit actions
	 * @author isaac
	 */
	private interface DatabaseChangesInterface{
		public void commitToDatabase(Database db);
		public void commitToHyPeerWeb();
	}
	/**
	 * Extension of DatabaseChanges class to handle folds
	 * @author isaac
	 */
	private static class FoldDatabaseChanges extends DatabaseChanges implements DatabaseChangesInterface{
		@Override
		public void commitToDatabase(Database db) {
			for (NodeUpdate nu: updates){
				int value = nu.value == null ? -1 : nu.value.webID;
				switch (nu.type){
					case DIRECT:
						db.setFold(nu.node.webID, value);
						break;
					case SURROGATE:
						db.setSurrogateFold(nu.node.webID, value);
						break;
					//Surrogate/Inverse are reflexive; DB will handle the rest
					case INVERSE: break;
				}
			}
		}
		@Override
		public void commitToHyPeerWeb() {
			for (NodeUpdate nu: updates){
				switch (nu.type){
					case DIRECT:
						nu.node.fold = nu.value;
						break;
					case SURROGATE:
						nu.node.surrogateFold = nu.value;
						break;
					case INVERSE:
						nu.node.inverseSurrogateFold = nu.value;
						break;
				}
			}
		}
	}
	/**
	 * Extension of DatabaseChanges to handle neighbors
	 * @author guy
	 */
	private static class NeighborDatabaseChanges extends DatabaseChanges implements DatabaseChangesInterface{
		@Override
		public void commitToDatabase(Database db) {
			for (NodeUpdate nu: updates){
				switch (nu.type){
					case DIRECT:
						db.addNeighbor(nu.node.webID, nu.value.webID);
						break;
					case SURROGATE:
						db.addSurrogateNeighbor(nu.node.webID, nu.value.webID);
						break;
					//Surrogate/Inverse are reflexive; DB will handle the rest
					case INVERSE: break;
				}
			}
		}
		@Override
		public void commitToHyPeerWeb() {
			for (NodeUpdate nu: updates){
				switch (nu.type){
					case DIRECT:
						nu.node.neighbors.add(nu.value);
						break;
					case SURROGATE:
						nu.node.surrogateNeighbors.add(nu.value);
						break;
					case INVERSE:
						nu.node.inverseSurrogateNeighbors.add(nu.node);
						break;
				}
			}
		}
	
	}
	
	//ACCESS METHODS FOR 
	/**
	 * Gets the WebID of the Node
	 *
	 * @return The WebID of the Node
	 */
	@Override
	public int getWebId() {
		return webID;
	}
	/**
	 * Gets the Height of the Node
	 *
	 * @return The Height of the Node
	 */
	@Override
	public int getHeight() {
		return height;
	}
	/**
	 * Gets the WebId of the Node's Fold
	 *
	 * @return The WebID of the Node's Fold
	 */
	@Override
	public Node getFold() {
		return fold;
	}
	/**
	 * Gets the WebID of the Surrogate Fold of the Node
	 *
	 * @return The WebID of the Surrogate Fold of the Node
	 */
	@Override
	public Node getSurrogateFold() {
		return surrogateFold;
	}
	/**
	 * Gets the WebID of the Inverse Surrogate Fold of the Node
	 *
	 * @return The WebID of the Inverse Surrogate Fold of the Node
	 */
	@Override
	public Node getInverseSurrogateFold() {
		return inverseSurrogateFold;
	}
	/**
	 * Gets an ArrayList containing the Neighbors of the Node
	 *
	 * @return An ArrayList containing the Neighbors of the Node
	 */
	@Override
	public Node[] getNeighbors() {
		return neighbors.toArray(new Node[0]);
	}
	/**
	 * Gets an ArrayList containing the Surrogate Neighbors of the Node
	 *
	 * @return An ArrayList containing the Surrogate Neighbors of the Node
	 */
	@Override
	public Node[] getSurrogateNeighbors() {
		return surrogateNeighbors.toArray(new Node[0]);
	}
	/**
	 * Gets an ArrayList containing the Inverse Surrogate Neighbors of the Node
	 *
	 * @return An ArrayList containing the Inverse Surrogate Neighbors of the
	 * Node
	 */
	@Override
	public Node[] getInverseSurrogateNeighbors() {
		return inverseSurrogateNeighbors.toArray(new Node[0]);
	}
	@Override
	public Node getParent() {
		Node lowest = this;
		for (Node n : neighbors) {
			if (n.webID < lowest.webID)
				lowest = n;
		}
		return lowest == this ? null : lowest;
	}
		
	//Setters
	/**
	 * Adds a Neighbor WebID to the list of Neighbors if it is not already in
	 * the list
	 *
	 * @param n The WebID of the Neighbor
	 */
	public void addNeighbor(Node n) {
		if (!isNeighbor(n))
			neighbors.add(n);
	}
	/**
	 * Checks to see if a WebID is in the list of Neighbors
	 *
	 * @param n The WebID to check
	 * @return True if found, false otherwise
	 */
	private boolean isNeighbor(Node n) {
		return neighbors.contains(n);
	}
	/**
	 * Adds a Surrogate Neighbor WebID to the list of Surrogate Neighbors if it
	 * is not already in the list
	 *
	 * @param sn The WebID of the Surrogate Neighbor
	 */
	public void addSurrogateNeighbor(Node sn) {
		if (!isSurrogateNeighbor(sn))
			surrogateNeighbors.add(sn);
	}
	/**
	 * Checks to see if a WebID is in the list of Surrogate Neighbors
	 *
	 * @param sn The WebID to check
	 * @return True if found, false otherwise
	 */
	private boolean isSurrogateNeighbor(Node sn) {
		return surrogateNeighbors.contains(sn);
	}
	/**
	 * Adds an Inverse Surrogate Neighbor WebID to the list of Inverse Surrogate
	 * Neighbors if it is not already in the list
	 *
	 * @param isn The WebID of the Inverse Surrogate Neighbor
	 */
	public void addInverseSurrogateNeighbor(Node isn) {
		if (!isInverseSurrogateNeighbor(isn))
			inverseSurrogateNeighbors.add(isn);
	}
	/**
	 * Checks to see if a WebID is in the list of Inverse Surrogate Neighbors
	 *
	 * @param isn The WebID to check
	 * @return True if found, false otherwise
	 */
	private boolean isInverseSurrogateNeighbor(Node isn) {
		return inverseSurrogateNeighbors.contains(isn);
	}
		
	@Override
	public int compareTo(NodeInterface node) {
		if (webID < node.getWebId())
			return -1;
		else if (webID == node.getWebId())
			return 0;
		return 1;
	}
	@Override
	public int hashCode(){
		return (int) ((this.webID * prime) % Integer.MAX_VALUE);
	}
	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass())
			return false;
		return this.webID != ((Node) obj).webID;
	}
}
