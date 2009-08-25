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

import java.util.HashSet;
import java.util.Set;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import org.openscience.cdk.controller.undoredo.IUndoRedoFactory;
import org.openscience.cdk.controller.undoredo.IUndoRedoable;
import org.openscience.cdk.controller.undoredo.UndoRedoHandler;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.tools.LoggingTool;

/**
 * Module to move around a selection of atoms and bonds
 *
 * @author Niels Out
 * @cdk.svnrev $Revision: 9162 $
 * @cdk.module controlbasic
 */
public class MoveModule extends ControllerModuleAdapter {

    private LoggingTool logger = new LoggingTool(MoveModule.class);
    
    private Vector2d offset;
    
    private IAtomContainer undoRedoContainer;
    
    private Point2d start2DCenter;

    public MoveModule(IChemModelRelay chemObjectRelay) {
        super(chemObjectRelay);
    }

    public void mouseClickedDown(Point2d worldCoord) {

        undoRedoContainer = 
            chemModelRelay.getIChemModel().getBuilder().newAtomContainer();
        
        IAtomContainer selectedAC = getSelectedAtomContainer(worldCoord );
        if (selectedAC != null) {

            // It could be that only a  selected bond is going to be moved. 
            // So make sure that the attached atoms are included, otherwise
            // the undo will fail to place the atoms back where they were
            for (IBond selectedBond : selectedAC.bonds() ) {
                for (IAtom atom : selectedBond.atoms()) {
                    if (!selectedAC.contains(atom)) {
                        selectedAC.addAtom(atom);
                    }
                }
            }
            
            undoRedoContainer.add(selectedAC);

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
            Point2d end2DCenter = GeometryTools.get2DCenter(undoRedoContainer);
            end.sub(end2DCenter, start2DCenter);

            IUndoRedoFactory factory = chemModelRelay.getUndoRedoFactory();
            UndoRedoHandler handler = chemModelRelay.getUndoRedoHandler();
            if (factory != null && handler != null) {
                IUndoRedoable undoredo = factory.getMoveAtomEdit(
                        undoRedoContainer, end, this.getDrawModeString());
                handler.postEdit(undoredo);
            }

            
            // Do the merge of atoms
            if (!chemModelRelay.getRenderer().getRenderer2DModel().getMerge()
                    .isEmpty()) {
                chemModelRelay.mergeMolecules(end);
            } else {
                for(IAtom atom:selection.getConnectedAtomContainer().atoms()) {
                   //use move without undo to prevent all individual 
                   //atoms contributing to the undo stack
                   chemModelRelay.moveToWithoutUndo( atom, atom.getPoint2d());
                }
            }

    	}
    	endMove();
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

            Point2d d = new Point2d();
            d.sub(worldCoordTo, worldCoordFrom);
            IAtomContainer selectedAC = selection.getConnectedAtomContainer();
            Set<IAtom> moveAtoms = new HashSet<IAtom>();
            for (IAtom atom : selectedAC.atoms()) {
                moveAtoms.add(atom);
            }
            for (IBond bond : selectedAC.bonds()) {
                for (IAtom atom : bond.atoms())
                    moveAtoms.add(atom);
            }
            
            for (IAtom atom : moveAtoms)
                atom.getPoint2d().add(d);
            
            // check for possible merges
            RendererModel model = 
                chemModelRelay.getRenderer().getRenderer2DModel(); 
            model.getMerge().clear();
            
            for (IAtom moveAtom : moveAtoms) {
                IAtom inRange = chemModelRelay.getAtomInRange(moveAtoms, moveAtom);
                if (inRange != null) {
                    model.getMerge().put( moveAtom, inRange );
                }
            }
            chemModelRelay.updateView();

        } else {
            if (chemModelRelay == null) {
                logger.debug("chemObjectRelay is NULL!");
            }
        }
    }

    public void setChemModelRelay(IChemModelRelay relay) {
        this.chemModelRelay = relay;
    }

	public String getDrawModeString() {
		return "Move";
	}

}
