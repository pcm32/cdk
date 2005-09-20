/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  The Chemistry Development Kit (CDK) project
 * 
 * Contact: cdk-devel@lists.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.openscience.cdk;

/**
 * A AminoAcid is Monomer which stores additional amino acid specific 
 * informations, like the N-terminus atom.
 *
 * @cdk.module data
 *
 * @author      Egon Willighagen <e.willighagen@science.ru.nl>
 * @cdk.created 2005-08-11
 * @cdk.keyword amino acid
 */
public class AminoAcid extends Monomer implements java.io.Serializable
{

    /**
     * Determines if a de-serialized object is compatible with this class.
     *
     * This value must only be changed if and only if the new version
     * of this class is imcompatible with the old version. See Sun docs
     * for <a href=http://java.sun.com/products/jdk/1.1/docs/guide
     * /serialization/spec/version.doc.html>details</a>.
	 */
	private static final long serialVersionUID = -5032283549467862509L;
	
	/** The atom that constitutes the N-terminus. */
    private org.openscience.cdk.interfaces.Atom nTerminus;
    /** The atom that constitutes the C-terminus. */
    private org.openscience.cdk.interfaces.Atom cTerminus;

    /**
     * Contructs a new AminoAcid.
     */
    public AminoAcid() {
        super();
    }
    
    /**
     * Retrieves the N-terminus atom.
     *
     * @return The Atom that is the N-terminus
     *
     * @see    #addNTerminus(Atom)
     */
    public org.openscience.cdk.interfaces.Atom getNTerminus() {
        return nTerminus;
    }

    /**
     * Add an Atom and makes it the N-terminus atom.
     *
     * @param atom  The Atom that is the N-terminus
     *
     * @see    #getNTerminus
     */
    public void addNTerminus(org.openscience.cdk.interfaces.Atom atom) {
        super.addAtom(atom);
        nTerminus = atom;
    }
    
    /**
     * Marks an Atom as being the N-terminus atom. It assumes that the Atom
     * is already added to the AminoAcid.
     *
     * @param atom  The Atom that is the N-terminus
     *
     * @see    #addNTerminus
     */
    private void setNTerminus(org.openscience.cdk.interfaces.Atom atom) {
        nTerminus = atom;
    }

    /**
     * Retrieves the C-terminus atom.
     *
     * @return The Atom that is the C-terminus
     *
     * @see    #addCTerminus(Atom)
     */
    public org.openscience.cdk.interfaces.Atom getCTerminus() {
        return cTerminus;
    }

    /**
     * Add an Atom and makes it the C-terminus atom.
     *
     * @param atom  The Atom that is the C-terminus
     *
     * @see    #getCTerminus
     */
    public void addCTerminus(org.openscience.cdk.interfaces.Atom atom) {
        super.addAtom(atom);
        setCTerminus(atom);
    }

    /**
     * Marks an Atom as being the C-terminus atom. It assumes that the Atom
     * is already added to the AminoAcid.
     *
     * @param atom  The Atom that is the C-terminus
     *
     * @see    #addCTerminus
     */
    private void setCTerminus(org.openscience.cdk.interfaces.Atom atom) {
        cTerminus = atom;
    }

    /**
     * Clones this AminoAcid object.
     *
     * @return    The cloned object
     */
    public Object clone() {
        AminoAcid clone = null;
        try {
            clone = (AminoAcid) super.clone();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        // copying the new N-terminus and C-terminus pointers
        if (getNTerminus() != null)
        	clone.setNTerminus(clone.getAtomAt(getAtomNumber(getNTerminus())));
        if (getCTerminus() != null)
        	clone.setCTerminus(clone.getAtomAt(getAtomNumber(getCTerminus())));
        return clone;
    }
    
    public String toString() {
        StringBuffer stringContent = new StringBuffer();
        stringContent.append("AminoAcid(");
        stringContent.append(this.hashCode()).append(", ");
        if (nTerminus == null) {
        	stringContent.append("N: null, ");
        } else {
        	stringContent.append("N: ").append(nTerminus.hashCode()).append(", ");
        }
        if (cTerminus == null) {
        	stringContent.append("C: null, ");
        } else {
        	stringContent.append("C: ").append(cTerminus.hashCode()).append(", ");
        }
        stringContent.append(super.toString());
        stringContent.append(")");
        return stringContent.toString();
    }

}
