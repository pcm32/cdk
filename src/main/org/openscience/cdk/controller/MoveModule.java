/*
 * Copyright (C) 2007  Niels Out <nielsout@users.sf.net>
 * Copyright (C) 2008-2009  Arvid Berg <goglepox@users.sf.net>
 * Copyright (C) 2008  Stefan Kuhn (undo redo)
 * Copyright (C) 2009  Mark Rijnbeek (markr@ebi.ac.uk)
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
package org.openscience.cdk.controller;

import static org.openscience.cdk.controller.edit.CompositEdit.compose;
import static org.openscience.cdk.controller.edit.Merge.merge;
import static org.openscience.cdk.controller.edit.Move.move;
import static org.openscience.cdk.controller.edit.OptionalUndoEdit.wrapFinal;
import static org.openscience.cdk.controller.edit.OptionalUndoEdit.wrapNonFinal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import org.openscience.cdk.controller.edit.IEdit;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.tools.ILoggingTool;
import org.openscience.cdk.tools.LoggingToolFactory;

/**
 * Module to move around a selection of atoms and bonds
 *
 * @author Niels Out
 * @cdk.svnrev $Revision: 9162 $
 * @cdk.module controlbasic
 */
public class MoveModule extends ControllerModuleAdapter {

    private ILoggingTool logger =
        LoggingToolFactory.createLoggingTool(MoveModule.class);

    private Vector2d offset;

    private Set<IAtom> atomsToMove;

    private Point2d start2DCenter;

    public MoveModule(IChemModelRelay chemObjectRelay) {
        super(chemObjectRelay);
    }

    public void mouseClickedDown(Point2d worldCoord) {

        IAtomContainer selectedAC = getSelectedAtomContainer(worldCoord );
        if (selectedAC != null) {

            // It could be that only a  selected bond is going to be moved.
            // So make sure that the attached atoms are included, otherwise
            // the undo will fail to place the atoms back where they were
           atomsToMove = new HashSet<IAtom>();

            for (IAtom atom : selectedAC.atoms()) {
                atomsToMove.add(atom);
            }
            for (IBond bond : selectedAC.bonds()) {
                for (IAtom atom : bond.atoms())
                    atomsToMove.add(atom);
            }

            Point2d current = GeometryTools.get2DCenter(selectedAC);
            start2DCenter = current;
            offset = new Vector2d();
            offset.sub(current, worldCoord);

        } else {
            endMove();
        }
    }

    public void mouseClickedUp(Point2d worldCoord) {
    	if (start2DCenter != null) {
            Vector2d end = new Vector2d();

            // take 2d center of end point to ensure correct positional undo
            Point2d end2DCenter = GeometryTools.get2DCenter(atomsToMove);
            end.sub(end2DCenter, start2DCenter);

            Double diff = end2DCenter.distanceSquared(start2DCenter);
            if(Double.isNaN(diff) || diff.equals(0)) { endMove();return;}
            Map<IAtom, IAtom> mergeMap = chemModelRelay.getRenderer()
                                          .getRenderer2DModel().getMerge();
            // Do the merge of atoms
            if (!mergeMap.isEmpty()) {

                // First try to shift the selection to be exactly on top of
                // the target of the merge. This makes the end results visually
                // more attractive and avoid tilted rings
                Vector2d shift = calcualteShift( mergeMap );

                IEdit smallMove = wrapNonFinal( move( shift, atomsToMove ));
                shift.add( end );
                IEdit shiftEdit = wrapFinal( move( shift, atomsToMove));

                IEdit mergeEdit = merge(mergeMap);
                mergeMap.clear();
                chemModelRelay.execute( compose(smallMove, shiftEdit, mergeEdit) );
            }else {
                IEdit edit = wrapFinal( move( end, atomsToMove ));
                chemModelRelay.execute(edit);
            }
    	}
    	endMove();
    }

    public static Vector2d calcualteShift( Map<IAtom, IAtom> mergeMap ) {
        Iterator<IAtom> it = mergeMap.keySet().iterator();
        if(!it.hasNext()) return new Vector2d();
        IAtom atomA = (IAtom) it.next();
        IAtom atomB = mergeMap.get( atomA );
        Vector2d shift = new Vector2d();
        shift.sub( atomB.getPoint2d(), atomA.getPoint2d() );
        return shift;
    }

    private void endMove() {
        start2DCenter = null;
        selection = null;
        offset = null;
    }

    public void mouseDrag(Point2d worldCoordFrom, Point2d worldCoordTo) {
        if (chemModelRelay != null && offset != null) {

            Point2d atomCoord = new Point2d();
            atomCoord.add(worldCoordTo, offset);

            Vector2d d = new Vector2d();
            d.sub(worldCoordTo, worldCoordFrom);

            IEdit edit  = wrapNonFinal( move( d, atomsToMove ));
            // check for possible merges
            RendererModel model =
                chemModelRelay.getRenderer().getRenderer2DModel();
            model.getMerge().clear();

            model.getMerge().putAll( calculateMerge( atomsToMove,
                                     getModel() ,
              getHighlightDistance()));

            chemModelRelay.execute( edit );

        } else {
            if (chemModelRelay == null) {
                logger.debug("chemObjectRelay is NULL!");
            }
        }
    }

    public static class DistAtom implements Comparable<DistAtom>{
        IAtom atom;
        double distSquared;

        public DistAtom(IAtom atom, double distSquared) {
            this.atom = atom;
            this.distSquared = distSquared;
        }

        public int compareTo( DistAtom o ) {
            if(this.distSquared < o.distSquared) return -1;
            if(this.distSquared > o.distSquared) return 1;
            return 0;
        }
    }

    public static Map<IAtom, IAtom> calculateMerge( IAtomContainer ac1,
                                                    IAtomContainer ac2,
                                                    double maxDistance) {
        Set<IAtom> atoms = new HashSet<IAtom>();
        for(IAtom atom:ac1.atoms()) {
            atoms.add( atom );
        }
        return calculateMerge( atoms, ac2, maxDistance );
    }
    public static Map<IAtom, IAtom> calculateMerge( Collection<IAtom> mergeAtoms ,
                                                    IAtomContainer ac,
                                                    double maxDistance) {
        double maxDistance2 = maxDistance *= maxDistance; // maxDistance squared
        Map<IAtom,IAtom> mergers = new HashMap<IAtom, IAtom>();
        for(IAtom atom:mergeAtoms) {
            List<DistAtom> candidates = findMergeCandidates(ac,atom);
            Collections.sort( candidates);
            for(DistAtom candiate:candidates) {
                if(candiate.distSquared>maxDistance2)
                    break;
                if(mergeAtoms.contains( candiate.atom ))
                    continue;
                mergers.put( atom, candiate.atom );
            }
        }
        return mergers;
    }

    private static List<DistAtom> findMergeCandidates(IAtomContainer set, IAtom atom ) {
        List<DistAtom> candidates = new ArrayList<DistAtom>();
        for(IAtom candidate:set.atoms()) {
            double disSquare = candidate.getPoint2d().distanceSquared( atom.getPoint2d() );
            candidates.add(new DistAtom(candidate,disSquare));
        }
        return candidates;
    }

    public String getDrawModeString() {
		return "Move";
	}

}
