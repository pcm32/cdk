/*
 * Copyright (C) 2009  Arvid Berg <goglepox@users.sourceforge.net>
 *
 * Contact: cdk-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All I ask is that proper credit is given for my work, which includes
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
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.cdk.controller.edit;

import java.util.List;
import java.util.Set;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import org.openscience.cdk.controller.Changed;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.layout.AtomPlacer;

/**
 * Edit representing the adding of an atom and a bond to an existing atom.
 * @author Arvid
 * @cdk.module controlbasic
 */
public class AppendAtom extends AbstractEdit implements IEdit {

    String symbol;
    IAtom source;
    Point2d pos;

    IAtom newSource;
    IAtom newAtom;
    IBond newBond;
    IBond.Stereo newStereo;

    /**
     * Creates and edit where <code>AtomPlacer</code> decides the location of
     * new atom and a bond connection the new atom with source.
     * @param symbol of the new atom.
     * @param source atom to append to.
     * @return edit representing the edit.
     */
    public static AppendAtom appendAtom(String symbol, IAtom source) {
        return new AppendAtom( symbol, source, null ,null);
    }

    /**
     * Creates and edit where <code>AtomPlacer</code> decides the location of
     * new atom and a bond connection the new atom with source with a specific
     * stereo type.
     * @param symbol of the new atom.
     * @param source atom to append to.
     * @param stereo stereo type of the new bond
     * @return edit representing the edit.
     */
    public static AppendAtom appendAtom( String symbol, IAtom source,
                                         IBond.Stereo stereo) {
        return new AppendAtom( symbol, source, null ,stereo);
    }

    /**
     * Creates an edit representing the creation of an atom at a specific
     * position and attaching it to source with a new bond.
     * @param symbol of the new atom.
     * @param source atom to attaching to.
     * @param pos position of new atom.
     * @return edit representing the the creation and connecting.
     */
    public static AppendAtom appendAtom( String symbol, IAtom source, Point2d pos) {
        return new AppendAtom( symbol, source, pos, null);
    }

    /**
     * Creates and edit representing the creating of two atoms and a bond the
     * first atoms is placed at position pos.
     * @param pos position of the first atom.
     * @return edit representing the createion of bond and atoms.
     */
    public static AppendAtom addNewBond(Point2d pos) {
        return new AppendAtom( "C", null, pos ,null);
    }

    private AppendAtom(String symbol, IAtom source, Point2d pos,IBond.Stereo stereo) {
        this.symbol = symbol;
        this.source = source;
        this.pos = pos;
        this.newStereo = stereo;
    }

    private void init() {
        IAtom start;
        if(source==null) {
            start = model.getBuilder().newInstance(IAtom.class,symbol,pos);
            newSource = start;
        }
        else
            start = source;

        if(pos != null && source!=null)
            newAtom = model.getBuilder().newInstance(IAtom.class,symbol, pos );
        else
            newAtom = model.getBuilder().newInstance(IAtom.class,symbol );

        if(newStereo!=null) {
            newBond = model.getBuilder().newInstance(IBond.class, start, newAtom ,
                                                  IBond.Order.SINGLE, newStereo);
        } else {
            newBond = model.getBuilder().newInstance(IBond.class, start, newAtom);
        }

        if(pos==null) {
            double bondLength;
            if (model.getBondCount() >= 1) {
                bondLength = GeometryTools.getBondLengthAverage(model);
            } else {
                bondLength = 1.4;       // XXX Or some sensible default?
            }
            placeNewAtom(model,start,newAtom,bondLength);
        }
    }
    public void redo() {
        if(newAtom == null && newBond == null) {
           init();
        }
        if(newSource != null)
            model.addAtom( newSource );
        model.addAtom( newAtom );
        model.addBond( newBond );

        updateHydrogenCount( newSource!=null?newSource:source,newAtom );
    }
    static void placeNewAtom( IAtomContainer model, IAtom sourceAtom,
                              IAtom atomToPlace, double bondLength) {

        // The AtomPlacer generates coordinates for the new atom


        AtomPlacer atomPlacer = new AtomPlacer();
        atomPlacer.setMolecule(model);
        // determine the atoms which define where the
        // new atom should not be placed
        List<IAtom> connectedAtoms = model.getConnectedAtomsList(sourceAtom);

        if (connectedAtoms.size() == 0) {
            Point2d newAtomPoint = new Point2d(sourceAtom.getPoint2d());
            double angle = Math.toRadians( -30 );
            Vector2d vec1 = new Vector2d(Math.cos(angle), Math.sin(angle));
            vec1.scale( bondLength );
            newAtomPoint.add( vec1 );
            atomToPlace.setPoint2d(newAtomPoint);
        } else if (connectedAtoms.size() == 1) {
            IMolecule ac = model.getBuilder().newInstance(IMolecule.class);
            ac.addAtom(sourceAtom);
            ac.addAtom(atomToPlace);
            Point2d distanceMeasure = new Point2d(0,0); // XXX not sure about this?
            IAtom connectedAtom = connectedAtoms.get(0);
            Vector2d v = atomPlacer.getNextBondVector( sourceAtom,
                                                     connectedAtom,
                                                     distanceMeasure, true);
            atomPlacer.placeLinearChain(ac, v, bondLength);
        } else {
            IMolecule placedAtoms = model.getBuilder().newInstance(IMolecule.class);
            for (IAtom conAtom : connectedAtoms) placedAtoms.addAtom(conAtom);
            Point2d center2D = GeometryTools.get2DCenter(placedAtoms);

            IAtomContainer unplacedAtoms = model.getBuilder().newInstance(IAtomContainer.class);
            unplacedAtoms.addAtom(atomToPlace);

            atomPlacer.distributePartners( sourceAtom, placedAtoms, center2D,
                                           unplacedAtoms, bondLength);
        }
    }

    public void undo() {

        model.removeBond( newBond );
        model.removeAtom( newAtom );
        if(newSource !=null)
            model.removeAtom( newSource );
        else
            updateHydrogenCount( source );

    }

    public Set<Changed> getTypeOfChanges() {

        return changed( Changed.Structure );
    }

}
