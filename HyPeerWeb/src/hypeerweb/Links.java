package hypeerweb;

import java.util.ArrayList;
import java.util.TreeSet;

/**
 * Maintains all node connections
 * @author isaac
 */
public class Links{
	public static enum Type {
		FOLD, SFOLD, ISFOLD, NEIGHBOR, SNEIGHBOR, ISNEIGHBOR
	}
	private Node fold;
	private Node surrogateFold;
	private Node inverseSurrogateFold;
	private TreeSet<Node> neighbors;
	private TreeSet<Node> surrogateNeighbors;
	private TreeSet<Node> inverseSurrogateNeighbors;
	public TreeSet<Node> highest;
	
	public Links(){
		neighbors = new TreeSet<>();
		surrogateNeighbors = new TreeSet<>();
		inverseSurrogateNeighbors = new TreeSet<>();
		highest = new TreeSet<>();
	}
	public Links(Node f, Node sf, Node isf, ArrayList<Node> n, ArrayList<Node> sn, ArrayList<Node> isn){
		this();
		//Add everything to the highest set as well
		//Add folds
		fold = f;
		surrogateFold = sf;
		inverseSurrogateFold = isf;
		if (f != null) highest.add(f);
		if (sf != null) highest.add(sf);
		if (isf != null) highest.add(isf);
		//Add neighbors
		if (n != null){
			neighbors.addAll(n);
			highest.addAll(n);
		}
		if (sn != null){
			surrogateNeighbors.addAll(sn);
			highest.addAll(sn);
		}
		if (isn != null){
			inverseSurrogateNeighbors.addAll(isn);
			highest.addAll(isn);
		}
	}
	
	/**
	 * Updates the connection
	 * @param oldNode the old Node reference (if there was one)
	 * @param newNode the new Node reference
	 * @param type the type of connection (Links.Type)
	 * @param keyChange if true, newNode will be treated as a key change,
	 * rather than a replacement of oldNode
	 */
	public void update(Node oldNode, Node newNode, Type type, boolean keyChange){
		//First remove the old node reference
		//In the case of folds, we can just set them right now
		//If we're updating the key, temporarily change the height/webID
		//So that the node gets added into the correct place
		if (keyChange){
			oldNode = newNode;
			newNode.changingKey = true;
		}
		switch (type){
			case FOLD:
				oldNode = fold;
				fold = newNode;
				break;
			case SFOLD:
				oldNode = surrogateFold;
				surrogateFold = newNode;
				break;
			case ISFOLD:
				oldNode = inverseSurrogateFold;
				inverseSurrogateFold = newNode;
				break;
			case NEIGHBOR:
				if (oldNode != null)
					neighbors.remove(oldNode);
				break;
			case SNEIGHBOR:
				if (oldNode != null)
					surrogateNeighbors.remove(oldNode);
				break;
			case ISNEIGHBOR:
				if (oldNode != null)
					inverseSurrogateNeighbors.remove(oldNode);
				break;
		}
		//Update the highest connection list
		//Make sure this node isn't another link type
		if (oldNode != null && (keyChange || !(fold == oldNode || surrogateFold == oldNode ||
			inverseSurrogateFold == fold || neighbors.contains(oldNode) ||
			surrogateNeighbors.contains(oldNode) || inverseSurrogateNeighbors.contains(oldNode))))
		{
			highest.remove(oldNode);
		}
		//Add it to the appropriate structure
		//Change the key back to the changed value
		if (keyChange)
			newNode.changingKey = false;
		if (newNode != null){
			switch (type){
				case NEIGHBOR:
					neighbors.add(newNode);
					break;
				case SNEIGHBOR:
					surrogateNeighbors.add(newNode);
					break;
				case ISNEIGHBOR:
					inverseSurrogateNeighbors.add(newNode);
					break;
			}
			//Update the highest connection list
			highest.add(newNode);
		}
	}
	
	//BROADCAST AND NOTIFICATION
	/**
	 * Notifies all incoming pointers that the current node has
	 * changed and the references need to be updated
	 * @param oldPointer the old node pointer
	 * @param newPointer the new node pointer
	 * @param keyChange if true, new node will be treated as a new key, rather
	 * than a replacement of oldPointer
	 */
	public void broadcastUpdate(Node oldPointer, Node newPointer, boolean keyChange){
		//NOTE: we reverse surrogate/inverse-surrogate connections
		//In the case of folds, we do not have to search for an oldPointer
		if (fold != null)
			fold.L.update(null, newPointer, Type.FOLD, keyChange);
		if (surrogateFold != null)
			surrogateFold.L.update(null, newPointer, Type.ISFOLD, keyChange);
		if (inverseSurrogateFold != null)
			inverseSurrogateFold.L.update(null, newPointer, Type.SFOLD, keyChange);
		for (Node n: neighbors)
			n.L.update(oldPointer, newPointer, Type.NEIGHBOR, keyChange);
		for (Node n: surrogateNeighbors)
			n.L.update(oldPointer, newPointer, Type.ISNEIGHBOR, keyChange);
		for (Node n: inverseSurrogateNeighbors)
			n.L.update(oldPointer, newPointer, Type.SNEIGHBOR, keyChange);
	}
	
	//SETTERS
	/**
	 * Adds a Neighbor to the set of Neighbors
	 * @param n the neighbor node
	 */
	public void addNeighbor(Node n) {
		update(null, n, Type.NEIGHBOR, false);
	}
	/**
	 * Removes a neighbor node
	 * @param n the node to remove
	 */
	public void removeNeighbor(Node n){
		update(n, null, Type.NEIGHBOR, false);
	}
	/**
	 * Adds a Surrogate Neighbor
	 * @param sn the new node
	 */
	public void addSurrogateNeighbor(Node sn) {
		update(null, sn, Type.SNEIGHBOR, false);
	}
	/**
	 * Removes a surrogate neighbor
	 * @param sn the node to remove
	 */
	public void removeSurrogateNeighbor(Node sn){
		update(sn, null, Type.SNEIGHBOR, false);
	}
	/**
	 * Adds an Inverse Surrogate Neighbor
	 * @param isn the new node
	 */
	public void addInverseSurrogateNeighbor(Node isn) {
		update(null, isn, Type.ISNEIGHBOR, false);
	}
	/**
	 * Removes the given node as an inverse surrogate neighbor
	 * @param isn Node to remove from inverse surrogate neighbor set
	 */
	public void removeInverseSurrogateNeighbor(Node isn){
		update(isn, null, Type.ISNEIGHBOR, false);
	}
	/**
	 * Removes all the IS neighbors from the node
	 */
	public void removeAllInverseSurrogateNeighbors(){
		highest.removeAll(inverseSurrogateNeighbors);
		inverseSurrogateNeighbors.clear();
	}
	/**
	 * Sets the the fold connection
	 * @param f the new fold node
	 */
	public void setFold(Node f) {
		update(null, f, Type.FOLD, false);
	}
	/**
	 * Sets the surrogate fold of the node
	 * @param sf the new surrogate fold node
	 */
	public void setSurrogateFold(Node sf) {
		update(null, sf, Type.SFOLD, false);
	}
	/**
	 * Sets the Inverse Surrogate Fold of the Node
	 * @param isf the new Inverse Surrogate Fold of the Node
	 */
	public void setInverseSurrogateFold(Node isf) {
		update(null, isf, Type.ISFOLD, false);
	}
	
	//GETTERS
	/**
	 * Gets the highest node out of all the connections
	 * @return a node
	 */
	public Node getHighestLink(){
		return highest.last();
	}
	/**
	 * Gets the lowest node out of all the connections
	 * @return a node
	 */
	public Node getLowestLink(){
		return highest.first();
	}
	/**
	 * Gets the node's fold
	 * @return
	 */
	public Node getFold() {
		return fold;
	}
	/**
	 * Gets the Surrogate Fold of the Node
	 * @return
	 */
	public Node getSurrogateFold() {
		return surrogateFold;
	}
	/**
	 * Gets the Inverse Surrogate Fold of the Node
	 * @return
	 */
	public Node getInverseSurrogateFold() {
		return inverseSurrogateFold;
	}
	/**
	 * Gets an ordered list (ascending) containing the Neighbors of the Node
	 * @return
	 */
	public Node[] getNeighbors() {
		return neighbors.toArray(new Node[neighbors.size()]);
	}
	/**
	 * Gets the neighbor of greatest height
	 * @return a neighbor
	 */
	public Node getHighestNeighbor(){
		if (neighbors.isEmpty())
			return null;
		return neighbors.last();
	}
	/**
	 * Gets the neighbor of smallest height
	 * @return a neighbor
	 */
	public Node getLowestNeighbor(){
		if (neighbors.isEmpty())
			return null;
		return neighbors.first();
	}
	/**
	 * Gets an ordered (ascending) list containing the Surrogate Neighbors of the Node
	 * @return a list of nodes
	 */
	public Node[] getSurrogateNeighbors() {
		return surrogateNeighbors.toArray(new Node[surrogateNeighbors.size()]);
	}		
	/**
	 * Gets the last surrogate neighbor of the node
	 * @return the last surrogate neighbor
	 */
	public Node getHighestSurrogateNeighbor(){
		if (surrogateNeighbors.isEmpty())
			return null;
		return surrogateNeighbors.last();
	}
	/**
	 * Gets the first surrogate neighbor of the node
	 * @return the first surrogate neighbor
	 */
	public Node getLowestSurrogateNeighbor(){
		if (surrogateNeighbors.isEmpty())
			return null;
		return surrogateNeighbors.first();
	}
	/**
	 * Gets an ordered (ascending) list of Inverse Surrogate Neighbors of the Node
	 * @return a list of nodes
	 */
	public Node[] getInverseSurrogateNeighbors() {
		return inverseSurrogateNeighbors.toArray(new Node[inverseSurrogateNeighbors.size()]);
	}
	/**
	 * Gets the first inverse surrogate neighbor of the node
	 * @return the first inverse surrogate neighbor
	 */
	public Node getHighestInverseSurrogateNeighbor(){
		if (inverseSurrogateNeighbors.isEmpty())
			return null;
		return inverseSurrogateNeighbors.last();
	}
	/**
	 * Gets the smallest isneighbor
	 * @return 
	 */
	public Node getLowestInverseSurrogateNeighbor(){
		if (inverseSurrogateNeighbors.isEmpty())
			return null;
		return inverseSurrogateNeighbors.first();
	}
	
	//PROTECTED GETTERS (WARNING, DO NOT MODIFY RETURNED VALUES)
	/**
	 * Gets neighbors as a collection
	 * Implementor must not modify the values
	 * @return a set of neighbors
	 */
	protected TreeSet<Node> getNeighborsSet(){
		return neighbors;
	}
	/**
	 * Gets surrogate neighbors as a collection
	 * Implementor must not modify the values
	 * @return a set of surrogate neighbors
	 */
	protected TreeSet<Node> getSurrogateNeighborsSet(){
		return surrogateNeighbors;
	}
	/**
	 * Gets inverse surrogate neighbors as a collection
	 * Implementor must not modify the values
	 * @return a set of inverse surrogate neighbors
	 */
	protected TreeSet<Node> getInverseSurrogateNeighborsSet(){
		return inverseSurrogateNeighbors;
	}
}