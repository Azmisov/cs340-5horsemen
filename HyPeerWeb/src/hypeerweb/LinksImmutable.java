/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hypeerweb;

import java.io.ObjectStreamException;

/**
 *
 * @author ljnutal6
 */
public class LinksImmutable extends Links {
	public LinksImmutable(Links links){
		super(links);
	}
	@Override
	public Object writeReplace() throws ObjectStreamException {
		return this;
	}	
}