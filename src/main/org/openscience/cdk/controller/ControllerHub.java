/* $Revision: 7636 $ $Author: egonw $ $Date: 2007-01-04 18:46:10 +0100 (Thu, 04 Jan 2007) $
 *
 * Copyright (C) 2007-2008  Egon Willighagen <egonw@users.sf.net>
 *               2005-2007  Christoph Steinbeck <steinbeck@users.sf.net>
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

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.atomtype.CDKAtomTypeMatcher;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.controller.edit.IEdit;
import org.openscience.cdk.controller.edit.OptionalUndoEdit;
import org.openscience.cdk.controller.undoredo.IUndoRedoFactory;
import org.openscience.cdk.controller.undoredo.IUndoRedoable;
import org.openscience.cdk.controller.undoredo.UndoRedoHandler;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomType;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.interfaces.IPseudoAtom;
import org.openscience.cdk.interfaces.IReaction;
import org.openscience.cdk.interfaces.IReactionSet;
import org.openscience.cdk.interfaces.IRing;
import org.openscience.cdk.interfaces.ISingleElectron;
import org.openscience.cdk.interfaces.IBond.Order;
import org.openscience.cdk.layout.AtomPlacer;
import org.openscience.cdk.layout.RingPlacer;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.layout.TemplateHandler;
import org.openscience.cdk.nonotify.NoNotificationChemObjectBuilder;
import org.openscience.cdk.renderer.BoundsCalculator;
import org.openscience.cdk.renderer.IRenderer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator.Scale;
import org.openscience.cdk.renderer.generators.HighlightAtomGenerator.HighlightAtomDistance;
import org.openscience.cdk.renderer.selection.IChemObjectSelection;
import org.openscience.cdk.renderer.selection.IncrementalSelection;
import org.openscience.cdk.tools.SaturationChecker;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.AtomContainerSetManipulator;
import org.openscience.cdk.tools.manipulator.BondManipulator;
import org.openscience.cdk.tools.manipulator.ChemModelManipulator;
import org.openscience.cdk.tools.manipulator.ReactionManipulator;
import org.openscience.cdk.tools.manipulator.ReactionSetManipulator;
import org.openscience.cdk.validate.ProblemMarker;

/**
 * Class that will central interaction point between a mouse event throwing
 * widget (SWT or Swing) and the Controller2D modules.
 *
 * <p>FIXME: will replace the old Controller2D class.
 *
 * @cdk.svnrev  $Revision: 9162 $
 * @cdk.module  controlbasic
 * @author      Niels Out
 * @author      egonw
 */
public class ControllerHub implements IMouseEventRelay, IChemModelRelay {

	private IChemModel chemModel;

	private IControllerModel controllerModel;
	
	private IRenderer renderer;
	
	private IViewEventRelay eventRelay;

	private List<IControllerModule> generalModules;

	private static StructureDiagramGenerator diagramGenerator;

	private IControllerModule activeDrawModule;
	
	private final static RingPlacer ringPlacer = new RingPlacer();

	private IAtomContainer phantoms;

    private IChemModelEventRelayHandler changeHandler;

    private IUndoRedoFactory undoredofactory;

    private UndoRedoHandler undoredohandler;

	private CDKAtomTypeMatcher matcher;

	public ControllerHub(IControllerModel controllerModel,
		                   IRenderer renderer,
		                   IChemModel chemModel,
		                   IViewEventRelay eventRelay,
		                   UndoRedoHandler undoredohandler,
		                   IUndoRedoFactory undoredofactory) {
		this.controllerModel = controllerModel;
		this.renderer = renderer;
		this.chemModel = chemModel;
		this.eventRelay = eventRelay;
		this.phantoms = chemModel.getBuilder().newInstance(IAtomContainer.class);
		this.undoredofactory=undoredofactory;
		this.undoredohandler=undoredohandler;

		generalModules = new ArrayList<IControllerModule>();

		registerGeneralControllerModule(new HighlightModule(this));
		registerGeneralControllerModule(new ZoomModule(this));
		matcher = CDKAtomTypeMatcher.getInstance(chemModel.getBuilder());
	}

	public IControllerModel getController2DModel() {
		return controllerModel;
	}

	public IControllerModel getControlModel() {
	     return controllerModel;
	}

	public IRenderer getRenderer() {
		return renderer;
	}

	public RendererModel getRenderModel() {
	    return renderer.getRenderer2DModel();
	}

	public IChemModel getIChemModel() {
	    return chemModel;
	}

  public void setChemModel(IChemModel model) {
      this.chemModel = model;
      //updateAtoms(ring, ring.atoms());
      //structureChanged();
    }

	/**
	 * Unregister all general IController2DModules.
	 */
	public void unRegisterAllControllerModule() {
		generalModules.clear();
	}

	/**
	 * Adds a general IController2DModule which will catch all mouse events.
	 */
	public void registerGeneralControllerModule(IControllerModule module) {
		module.setChemModelRelay(this);
		generalModules.add(module);
	}

	public void mouseWheelMovedBackward(int clicks) {
	    for (IControllerModule module : generalModules) {
            module.mouseWheelMovedBackward(clicks);
        }
	    IControllerModule activeModule = getActiveDrawModule();
        if (activeModule != null) activeModule.mouseWheelMovedBackward(clicks);

    }

    public void mouseWheelMovedForward(int clicks) {
        for (IControllerModule module : generalModules) {
            module.mouseWheelMovedForward(clicks);
        }
        IControllerModule activeModule = getActiveDrawModule();
        if (activeModule != null) activeModule.mouseWheelMovedForward(clicks);

    }

    public void mouseClickedDouble(int screenCoordX, int screenCoordY) {
		Point2d worldCoord =
		    renderer.toModelCoordinates(screenCoordX, screenCoordY);

		// Relay the mouse event to the general handlers
		for (IControllerModule module : generalModules) {
			module.mouseClickedDouble(worldCoord);
		}

		// Relay the mouse event to the active
		IControllerModule activeModule = getActiveDrawModule();
		if (activeModule != null) activeModule.mouseClickedDouble(worldCoord);
	}


	public void mouseClickedDownRight(int screenX, int screenY) {
		Point2d modelCoord = renderer.toModelCoordinates(screenX, screenY);

		// Relay the mouse event to the general handlers
		for (IControllerModule module : generalModules) {
			module.mouseClickedDownRight(modelCoord);
		}

		// Relay the mouse event to the active
		IControllerModule activeModule = getActiveDrawModule();
		if (activeModule != null) activeModule.mouseClickedDownRight(modelCoord);
	}

	public void mouseClickedUpRight(int screenX, int screenY) {
		Point2d modelCoord = renderer.toModelCoordinates(screenX, screenY);

		// Relay the mouse event to the general handlers
		for (IControllerModule module : generalModules) {
			module.mouseClickedUpRight(modelCoord);
		}

		// Relay the mouse event to the active
		IControllerModule activeModule = getActiveDrawModule();
		if (activeModule != null) activeModule.mouseClickedUpRight(modelCoord);
	}

	public void mouseClickedDown(int screenX, int screenY) {
		Point2d modelCoord = renderer.toModelCoordinates(screenX, screenY);

		// Relay the mouse event to the general handlers
		for (IControllerModule module : generalModules) {
			module.mouseClickedDown(modelCoord);
		}

		// Relay the mouse event to the active
		IControllerModule activeModule = getActiveDrawModule();
		if (activeModule != null) activeModule.mouseClickedDown(modelCoord);
	}

	public void mouseClickedUp(int screenX, int screenY) {
		Point2d modelCoord = renderer.toModelCoordinates(screenX, screenY);

		// Relay the mouse event to the general handlers
		for (IControllerModule module : generalModules) {
			module.mouseClickedUp(modelCoord);
		}

		// Relay the mouse event to the active
		IControllerModule activeModule = getActiveDrawModule();
		if (activeModule != null) activeModule.mouseClickedUp(modelCoord);
	}

	public void mouseDrag(
	        int screenXFrom, int screenYFrom, int screenXTo, int screenYTo) {
		Point2d modelCoordFrom =
		    renderer.toModelCoordinates(screenXFrom, screenYFrom);
		Point2d modelCoordTo =
		    renderer.toModelCoordinates(screenXTo, screenYTo);

		// Relay the mouse event to the general handlers
		for (IControllerModule module : generalModules) {
			module.mouseDrag(modelCoordFrom, modelCoordTo);
		}

		// Relay the mouse event to the active
		IControllerModule activeModule = getActiveDrawModule();
		if (activeModule != null) {
		    activeModule.mouseDrag(modelCoordFrom, modelCoordTo);
		}
	}

	public void mouseEnter(int screenX, int screenY) {
		Point2d worldCoord = renderer.toModelCoordinates(screenX, screenY);

		// Relay the mouse event to the general handlers
		for (IControllerModule module : generalModules) {
			module.mouseEnter(worldCoord);
		}

		// Relay the mouse event to the active
		IControllerModule activeModule = getActiveDrawModule();
		if (activeModule != null) activeModule.mouseEnter(worldCoord);
	}

	public void mouseExit(int screenX, int screenY) {
		Point2d worldCoord = renderer.toModelCoordinates(screenX, screenY);

		// Relay the mouse event to the general handlers
		for (IControllerModule module : generalModules) {
			module.mouseExit(worldCoord);
		}

		// Relay the mouse event to the active
		IControllerModule activeModule = getActiveDrawModule();
		if (activeModule != null) activeModule.mouseExit(worldCoord);
	}

	public void mouseMove(int screenX, int screenY) {
		Point2d worldCoord = renderer.toModelCoordinates(screenX, screenY);

		// Relay the mouse event to the general handlers
		for (IControllerModule module : generalModules) {
			module.mouseMove(worldCoord);
		}

		// Relay the mouse event to the active
		IControllerModule activeModule = getActiveDrawModule();
		if (activeModule != null) activeModule.mouseMove(worldCoord);
	}

	public void updateView() {
		//call the eventRelay method here to update the view..
		eventRelay.updateView();
	}

	public IControllerModule getActiveDrawModule() {
		return activeDrawModule;
	}

	public void setActiveDrawModule(IControllerModule activeDrawModule){
		this.activeDrawModule=activeDrawModule;
	}

	public IAtom getClosestAtom(Point2d worldCoord) {
        IAtom closestAtom = null;
        double closestDistanceSQ = Double.MAX_VALUE;

        for (IAtomContainer atomContainer :
            ChemModelManipulator.getAllAtomContainers(chemModel)) {

            for (IAtom atom : atomContainer.atoms()) {
                if (atom.getPoint2d() != null) {
                    double distanceSQ =
                        atom.getPoint2d().distanceSquared(worldCoord);
                    if (distanceSQ < closestDistanceSQ) {
                        closestAtom = atom;
                        closestDistanceSQ = distanceSQ;
                    }
                }
            }
        }

        return closestAtom;
    }

	public IBond getClosestBond(Point2d worldCoord) {
        IBond closestBond = null;
        double closestDistanceSQ = Double.MAX_VALUE;

        for (IAtomContainer atomContainer :
            ChemModelManipulator.getAllAtomContainers(chemModel)) {

            for (IBond bond : atomContainer.bonds()) {
                boolean hasCenter = true;
                for (IAtom atom : bond.atoms())
                    hasCenter = hasCenter && (atom.getPoint2d() != null);
                if (hasCenter) {
                    double distanceSQ =
                        bond.get2DCenter().distanceSquared(worldCoord);
                    if (distanceSQ < closestDistanceSQ) {
                        closestBond = bond;
                        closestDistanceSQ = distanceSQ;
                    }
                }
            }
        }
        return closestBond;
    }


	public IAtomContainer removeAtomWithoutUndo(IAtom atom) {
		IAtomContainer ac = atom.getBuilder().newInstance(IAtomContainer.class);
		ac.addAtom(atom);
		Iterator<IBond> connbonds = ChemModelManipulator.getRelevantAtomContainer(chemModel, atom).getConnectedBondsList(atom).iterator();
		while(connbonds.hasNext())
			ac.addBond(connbonds.next());
		ChemModelManipulator.removeAtomAndConnectedElectronContainers(chemModel, atom);
		for(IBond bond : ac.bonds()){
			if(bond.getAtom(0)==atom)
				updateAtom(bond.getAtom(1));
			else
				updateAtom(bond.getAtom(0));
		}
		structureChanged();
		return ac;
	}

	public IAtomContainer removeAtom(IAtom atom) {
		IAtomContainer ac = removeAtomWithoutUndo(atom);
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
		    IUndoRedoable undoredo = getUndoRedoFactory().getRemoveAtomsAndBondsEdit(getIChemModel(), ac, "Remove Atom",this);
		    getUndoRedoHandler().postEdit(undoredo);
	    }
		return ac;
	}

	public IAtom addAtom(String atomType, Point2d worldCoord) {
		IAtomContainer undoRedoContainer = chemModel.getBuilder().newInstance(IAtomContainer.class);
		undoRedoContainer.addAtom(addAtomWithoutUndo(atomType, worldCoord));
		if (getUndoRedoFactory() != null && getUndoRedoHandler() != null) {
            IUndoRedoable undoredo = getUndoRedoFactory().getAddAtomsAndBondsEdit(chemModel, undoRedoContainer, "Add Atom", this);
            getUndoRedoHandler().postEdit(undoredo);
        }
		return undoRedoContainer.getAtom(0);
	}

	public IAtom addAtomWithoutUndo(String atomType, Point2d worldCoord) {
		IAtom newAtom = chemModel.getBuilder().newInstance(IAtom.class,atomType, worldCoord);
		//FIXME : there should be an initial hierarchy?
		IMoleculeSet molSet = chemModel.getMoleculeSet();
		if (molSet == null) {
		    molSet = chemModel.getBuilder().newInstance(IMoleculeSet.class);
		    IMolecule ac = chemModel.getBuilder().newInstance(IMolecule.class);
		    ac.addAtom(newAtom);
		    molSet.addMolecule(ac);
		    chemModel.setMoleculeSet(molSet);
		} else {
		    // FIXME : always add to the first container?
		    molSet.getMolecule(0).addAtom(newAtom);
		}
		updateAtom(newAtom);
		RendererModel model = this.getRenderer().getRenderer2DModel();
		double nudgeDistance = model.getParameter(
                HighlightAtomDistance.class).getValue() /
		    model.getParameter(Scale.class).getValue();
		if (getClosestAtom(newAtom) != null)
		    newAtom.getPoint2d().x += nudgeDistance;
        structureChanged();
		return newAtom;
	}

	public IAtom addAtom(String atomType, IAtom atom) {
		IAtomContainer undoRedoContainer = atom.getBuilder().newInstance(IAtomContainer.class);
		undoRedoContainer.addAtom(addAtomWithoutUndo(atomType, atom));
	    IAtomContainer atomContainer =
	        ChemModelManipulator.getRelevantAtomContainer(
	                    getIChemModel(), undoRedoContainer.getAtom(0));
	        IBond newBond = atomContainer.getBond(atom, undoRedoContainer.getAtom(0));
	        undoRedoContainer.addBond(newBond);
		if (getUndoRedoFactory() != null && getUndoRedoHandler() != null) {
            IUndoRedoable undoredo = getUndoRedoFactory().getAddAtomsAndBondsEdit(chemModel, undoRedoContainer, "Add Atom", this);
            getUndoRedoHandler().postEdit(undoredo);
        }
		return undoRedoContainer.getAtom(0);
	}

    public IAtom addAtomWithoutUndo(String atomType, IAtom atom) {
        IAtom newAtom = chemModel.getBuilder().newInstance(IAtom.class,atomType);
        IBond newBond = chemModel.getBuilder().newInstance(IBond.class,atom, newAtom);
        IAtomContainer atomCon =
            ChemModelManipulator.getRelevantAtomContainer(chemModel, atom);
        if (atomCon == null) {
            atomCon = chemModel.getBuilder().newInstance(IMolecule.class);
            IMoleculeSet moleculeSet = chemModel.getMoleculeSet();
            if (moleculeSet == null) {
                moleculeSet = chemModel.getBuilder().newInstance(IMoleculeSet.class);
                chemModel.setMoleculeSet(moleculeSet);
            }
            moleculeSet.addAtomContainer(atomCon);
        }

        // The AtomPlacer generates coordinates for the new atom
        AtomPlacer atomPlacer = new AtomPlacer();
        atomPlacer.setMolecule(chemModel.getBuilder().newInstance(
        	IMolecule.class, atomCon
        ));
        double bondLength;
        if (atomCon.getBondCount() >= 1) {
            bondLength = GeometryTools.getBondLengthAverage(atomCon);
        } else {
            bondLength = 1.4;       // XXX Or some sensible default?
        }

        // determine the atoms which define where the
        // new atom should not be placed
        List<IAtom> connectedAtoms = atomCon.getConnectedAtomsList(atom);

        if (connectedAtoms.size() == 0) {
            Point2d newAtomPoint = new Point2d(atom.getPoint2d());
            double angle = Math.toRadians( -30 );
            Vector2d vec1 = new Vector2d(Math.cos(angle), Math.sin(angle));
            vec1.scale( bondLength );
            newAtomPoint.add( vec1 );
            newAtom.setPoint2d(newAtomPoint);
        } else if (connectedAtoms.size() == 1) {
            IMolecule ac = atomCon.getBuilder().newInstance(IMolecule.class);
            ac.addAtom(atom);
            ac.addAtom(newAtom);
            Point2d distanceMeasure = new Point2d(0,0); // XXX not sure about this?
            IAtom connectedAtom = connectedAtoms.get(0);
            Vector2d v = atomPlacer.getNextBondVector(
                    atom, connectedAtom, distanceMeasure, true);
            atomPlacer.placeLinearChain(ac, v, bondLength);
        } else {
            IMolecule placedAtoms = atomCon.getBuilder().newInstance(IMolecule.class);
            for (IAtom conAtom : connectedAtoms) placedAtoms.addAtom(conAtom);
            Point2d center2D = GeometryTools.get2DCenter(placedAtoms);

            IAtomContainer unplacedAtoms = atomCon.getBuilder().newInstance(IAtomContainer.class);
            unplacedAtoms.addAtom(newAtom);

            atomPlacer.distributePartners(
                    atom, placedAtoms, center2D, unplacedAtoms, bondLength);
        }

        atomCon.addAtom(newAtom);
        atomCon.addBond(newBond);
        updateAtom(newBond.getAtom(0));
        updateAtom(newBond.getAtom(1));
        
        // shift the new atom a bit if it is in range of another atom
        RendererModel model = this.getRenderer().getRenderer2DModel();
        double nudgeDistance = model.getParameter(
                HighlightAtomDistance.class).getValue() /
            model.getParameter(Scale.class).getValue();
        if (getClosestAtom(newAtom) != null)
        	newAtom.getPoint2d().x += nudgeDistance;
        
        structureChanged();
        return newAtom;
    }

	public void addNewBond(Point2d worldCoordinate) {
		IAtomContainer undoRedoContainer = 
		    getIChemModel().getBuilder().newInstance(IAtomContainer.class);
		
		// add the first atom in the new bond
		String atomType = getController2DModel().getDrawElement();
	    IAtom atom = addAtomWithoutUndo(atomType, worldCoordinate);
	    undoRedoContainer.addAtom(atom);
	    
	    // add the second atom to this
	    IAtom newAtom = addAtomWithoutUndo(atomType, atom);
	    undoRedoContainer.addAtom(newAtom);
	    
	    IAtomContainer atomContainer =
	        ChemModelManipulator.getRelevantAtomContainer(
                getIChemModel(), newAtom);
	    
        IBond newBond = atomContainer.getBond(atom, newAtom);
        undoRedoContainer.addBond(newBond);
        updateAtom(newBond.getAtom(0));
        updateAtom(newBond.getAtom(1));
        
	    structureChanged();
	    if(undoredofactory!=null && undoredohandler!=null){
		    IUndoRedoable undoredo = undoredofactory.getAddAtomsAndBondsEdit(getIChemModel(), undoRedoContainer, "Add Bond",this);
		    undoredohandler.postEdit(undoredo);
	    }
	}

	public void cycleBondValence(IBond bond) {
		IBond.Order[] orders=new IBond.Order[2];
		IBond.Stereo[] stereos=new IBond.Stereo[2];
		orders[1]=bond.getOrder();
		stereos[1]=bond.getStereo();
	    // special case : reset stereo bonds
	    if (bond.getStereo() != IBond.Stereo.NONE) {
	        bond.setStereo(IBond.Stereo.NONE);
	    }else{
	        // cycle the bond order up to maxOrder
		    IBond.Order maxOrder = getController2DModel().getMaxOrder();
	        if (BondManipulator.isLowerOrder(bond.getOrder(), maxOrder)) {
	            BondManipulator.increaseBondOrder(bond);
	        } else {
	            bond.setOrder(IBond.Order.SINGLE);
	        }
	    }
        orders[0]=bond.getOrder();
        stereos[0]=bond.getStereo();
		Map<IBond, IBond.Order[]> changedBonds = new HashMap<IBond, IBond.Order[]>();
		Map<IBond, IBond.Stereo[]> changedBondsStereo =
			new HashMap<IBond, IBond.Stereo[]>();
		changedBonds.put(bond,orders);
		changedBondsStereo.put(bond, stereos);
        updateAtom(bond.getAtom(0));
        updateAtom(bond.getAtom(1));
		structureChanged();
	    if(undoredofactory!=null && undoredohandler!=null){
	    	IUndoRedoable undoredo = undoredofactory.getAdjustBondOrdersEdit(
	    		changedBonds, changedBondsStereo, "Adjust Bond Order",this
	        );
		    undoredohandler.postEdit(undoredo);
	    }
	}

    public IBond makeNewStereoBond(IAtom atom, Direction desiredDirection) {
        String atomType = getController2DModel().getDrawElement();
        IAtom newAtom = addAtomWithoutUndo(atomType, atom);
        IAtomContainer undoRedoContainer=getIChemModel().getBuilder().newInstance(IAtomContainer.class);

        // XXX these calls would not be necessary if addAtom returned a bond
        IAtomContainer atomContainer =
            ChemModelManipulator.getRelevantAtomContainer(
                    getIChemModel(), newAtom);
        IBond newBond = atomContainer.getBond(atom, newAtom);

        if (desiredDirection == Direction.UP) {
            newBond.setStereo(IBond.Stereo.UP);
        } else {
            newBond.setStereo(IBond.Stereo.DOWN);
        }
        undoRedoContainer.addAtom(newAtom);
        undoRedoContainer.addBond(newBond);
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
		    IUndoRedoable undoredo = getUndoRedoFactory().getAddAtomsAndBondsEdit(getIChemModel(), undoRedoContainer, "Add Stereo Bond",this);
		    getUndoRedoHandler().postEdit(undoredo);
	    }
	    return newBond;
	}

    public void moveToWithoutUndo( IAtom atom, Point2d worldCoords ) {
        if ( atom != null ) {
            Point2d atomCoord = new Point2d( worldCoords );
            atom.setPoint2d( atomCoord );
        }
        coordinatesChanged();
    }

    public void moveTo( IAtom atom, Point2d worldCoords ) {
        if ( atom != null ) {
    		if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
    			IAtomContainer undoRedoContainer = chemModel.getBuilder().newInstance(IAtomContainer.class);
    			undoRedoContainer.addAtom(atom);
        		Vector2d end=new Vector2d();
        		end.sub(worldCoords,atom.getPoint2d());
    			IUndoRedoable undoredo = getUndoRedoFactory().getMoveAtomEdit(undoRedoContainer, end, "Move atom");
    			getUndoRedoHandler().postEdit(undoredo);
    		}
    		moveToWithoutUndo(atom, worldCoords);
        }
	}

    public void moveToWithoutUndo( IBond bond, Point2d point ) {
        if (bond != null) {
			Point2d center = bond.get2DCenter();
			for (IAtom atom : bond.atoms()) {
				Vector2d offset = new Vector2d();
				offset.sub(atom.getPoint2d(), center);
				Point2d result = new Point2d();
				result.add(point, offset);

				atom.setPoint2d(result);
			}
        }
        coordinatesChanged();
    }

    public void moveTo( IBond bond, Point2d point ) {
    	if (bond != null) {
    		if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
    			IAtomContainer undoRedoContainer = chemModel.getBuilder().newInstance(IAtomContainer.class);
            	undoRedoContainer.addAtom(bond.getAtom(0));
            	undoRedoContainer.addAtom(bond.getAtom(1));
        		Vector2d end=new Vector2d();
        		end.sub(point,bond.getAtom(0).getPoint2d());
    			IUndoRedoable undoredo = getUndoRedoFactory().getMoveAtomEdit(undoRedoContainer, end, "Move atom");
    			getUndoRedoHandler().postEdit(undoredo);
    		}
    		moveToWithoutUndo(bond, point);
    	}
    }

    public IBond addBond(IAtom fromAtom, IAtom toAtom) {
        IBond newBond = chemModel.getBuilder().newInstance(IBond.class,fromAtom, toAtom);
        chemModel.getMoleculeSet().getAtomContainer(0).addBond(newBond);
        updateAtom(newBond.getAtom(0));
        updateAtom(newBond.getAtom(1));
        structureChanged();
        return newBond;
    }

    public void setCharge(IAtom atom, int charge) {
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
	    	IUndoRedoable undoredo = getUndoRedoFactory().getChangeChargeEdit(atom,atom.getFormalCharge(),charge, "Change charge to "+charge,this);
		    getUndoRedoHandler().postEdit(undoredo);
	    }
        atom.setFormalCharge(charge);
        updateAtom(atom);
        structurePropertiesChanged();
    }

    public void setMassNumber(IAtom atom, int massNumber) {
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
		    IUndoRedoable undoredo = getUndoRedoFactory().getChangeIsotopeEdit(atom, atom.getMassNumber(), massNumber, "Change Atomic Mass to "+massNumber);
		    getUndoRedoHandler().postEdit(undoredo);
	    }
        atom.setMassNumber(massNumber);
        structurePropertiesChanged();
    }

    public void setOrder(IBond bond, Order order) {
    	Map<IBond, IBond.Order[]> changedBonds= new HashMap<IBond, IBond.Order[]>();
    	changedBonds.put(bond,new Order[]{order,bond.getOrder()});
        bond.setOrder(order);
        updateAtom(bond.getAtom(0));
        updateAtom(bond.getAtom(1));
        structurePropertiesChanged();
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
		    IUndoRedoable undoredo =
		    	getUndoRedoFactory().getAdjustBondOrdersEdit(changedBonds, 
		        new HashMap<IBond, IBond.Stereo[]>(),
		        "Changed Bond Order to "+order,
		        this
		    );
		    getUndoRedoHandler().postEdit(undoredo);
	    }
    }

    public void setSymbol(IAtom atom, String symbol) {
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
		    IUndoRedoable undoredo = getUndoRedoFactory().getChangeAtomSymbolEdit(atom,atom.getSymbol(),symbol,"Change Atom Symbol to "+symbol,this);
		    getUndoRedoHandler().postEdit(undoredo);
	    }
        atom.setSymbol(symbol);
        // configure the atom, so that the atomic number matches the symbol
        try {
            IsotopeFactory.getInstance(
                    atom.getBuilder()).configure(atom);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        updateAtom(atom);
        structurePropertiesChanged();
    }

    public void setWedgeType(IBond bond, IBond.Stereo type) {
        bond.setStereo(type);
        structurePropertiesChanged();
    }

    public void updateImplicitHydrogenCounts() {
    	Map<IAtom, Integer[]> atomHydrogenCountsMap= new HashMap<IAtom, Integer[]>();
        for (IAtomContainer container :
             ChemModelManipulator.getAllAtomContainers(chemModel)) {
            for (IAtom atom : container.atoms()) {
                if (!(atom instanceof IPseudoAtom)) {
                    try {
                        IAtomType type = matcher.findMatchingAtomType(
                            container, atom
                        );
                        if (type != null &&
                            type.getFormalNeighbourCount() != null) {
                            int connectedAtomCount = container.getConnectedAtomsCount(atom);
                        	atomHydrogenCountsMap.put(atom, new Integer[]{type.getFormalNeighbourCount() -
                                connectedAtomCount, atom.getImplicitHydrogenCount()});
                            atom.setImplicitHydrogenCount(
                                type.getFormalNeighbourCount() - connectedAtomCount
                            );
                        }
                    } catch ( CDKException e ) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
		    IUndoRedoable undoredo = getUndoRedoFactory().getChangeHydrogenCountEdit(atomHydrogenCountsMap, "Update implicit hydrogen count");
		    getUndoRedoHandler().postEdit(undoredo);
	    }
        structurePropertiesChanged();
    }

    public void zap() {
	    if (getUndoRedoFactory() != null && getUndoRedoHandler() != null) {
		    IUndoRedoable undoredo = getUndoRedoFactory().getClearAllEdit(chemModel, chemModel.getMoleculeSet(), chemModel.getReactionSet(), "Clear Panel");
		    getUndoRedoHandler().postEdit(undoredo);
	    }
	    if (chemModel.getMoleculeSet() != null){
		    IMoleculeSet molSet = chemModel.getBuilder().newInstance(IMoleculeSet.class);
		    IMolecule ac = chemModel.getBuilder().newInstance(IMolecule.class);
		    molSet.addMolecule(ac);
		    chemModel.setMoleculeSet(molSet);

	    }
	    if (chemModel.getReactionSet() != null)
	    	chemModel.setReactionSet(chemModel.getBuilder().newInstance(
	    		IReactionSet.class
	    	));
        structureChanged();
    }

    public void makeBondStereo(IBond bond, Direction desiredDirection) {
        IBond.Stereo stereo = bond.getStereo();
        boolean isUp = isUp(stereo);
        boolean isDown = isDown(stereo);
        boolean noStereo = noStereo(stereo);
        if (isUp && desiredDirection == Direction.UP) {
            flipDirection(bond, stereo);
        } else if (isDown && desiredDirection == Direction.UP) {
            flipOrientation(bond, stereo);
        } else if (isUp && desiredDirection == Direction.DOWN) {
            flipOrientation(bond, stereo);
        } else if (isDown && desiredDirection == Direction.DOWN) {
            flipDirection(bond, stereo);
        } else if (noStereo && desiredDirection == Direction.UP) {
            bond.setStereo(IBond.Stereo.UP);
        } else if (noStereo && desiredDirection == Direction.DOWN) {
            bond.setStereo(IBond.Stereo.DOWN);
        }
        IBond.Stereo[] stereos = new IBond.Stereo[2];
        stereos[1]=stereo;
        stereos[0]=bond.getStereo();
		Map<IBond, IBond.Order[]> changedBonds = new HashMap<IBond, IBond.Order[]>();
		Map<IBond, IBond.Stereo[]> changedBondsStereo =
			new HashMap<IBond, IBond.Stereo[]>();
		changedBondsStereo.put(bond, stereos);
		updateAtom(bond.getAtom(0));
		updateAtom(bond.getAtom(1));
		structureChanged();
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
	    	IUndoRedoable undoredo =
	    		getUndoRedoFactory().getAdjustBondOrdersEdit(
	    			changedBonds,
	    			changedBondsStereo,
	    			"Adjust Bond Stereo",
	    			this
	    	    );
		    getUndoRedoHandler().postEdit(undoredo);
	    }
    }

	/**
	 * Change the stereo bond from start->end to start<-end.
	 *
	 * @param bond the bond to change
	 * @param stereo the current stereo of that bond
	 */
	private void flipDirection(IBond bond, IBond.Stereo stereo) {
	    if (stereo == IBond.Stereo.UP)
	    	bond.setStereo(IBond.Stereo.UP_INVERTED);
	    else if (stereo == IBond.Stereo.UP_INVERTED)
	    	bond.setStereo(IBond.Stereo.UP);
	    else if (stereo == IBond.Stereo.DOWN_INVERTED)
	    	bond.setStereo(IBond.Stereo.DOWN);
	    else if (stereo == IBond.Stereo.DOWN)
	    	bond.setStereo(IBond.Stereo.DOWN_INVERTED);
	}


	/**
	 * Change the stereo of the bond from UP<->DOWN.
	 * @param bond the bond to change
	 * @param stereo the current stereo of the bond
	 */
	private void flipOrientation(IBond bond, IBond.Stereo stereo) {
	    if (stereo == IBond.Stereo.UP)
	    	bond.setStereo(IBond.Stereo.DOWN_INVERTED);
        else if (stereo == IBond.Stereo.UP_INVERTED)
        	bond.setStereo(IBond.Stereo.DOWN);
        else if (stereo == IBond.Stereo.DOWN_INVERTED) 
        	bond.setStereo(IBond.Stereo.UP);
        else if (stereo == IBond.Stereo.DOWN) 
        	bond.setStereo(IBond.Stereo.UP_INVERTED);
	}

	private boolean isUp(IBond.Stereo stereo) {
	    return stereo == IBond.Stereo.UP || stereo == IBond.Stereo.UP_INVERTED;
	}

    private boolean isDown(IBond.Stereo stereo) {
        return stereo == IBond.Stereo.DOWN ||
               stereo == IBond.Stereo.DOWN_INVERTED;
    }

    private boolean noStereo(IBond.Stereo stereo) {
        return stereo == IBond.Stereo.NONE ||
               stereo == CDKConstants.UNSET;
    }
    
    public static void avoidOverlap(IChemModel chemModel){
		//we avoid overlaps
		//first we we shift down the reactions
		Rectangle2D usedReactionbounds=null;
		if(chemModel.getReactionSet()!=null){
			for(IReaction reaction : chemModel.getReactionSet().reactions()){
	            // now move it so that they don't overlap
	            Rectangle2D reactionbounds = BoundsCalculator.calculateBounds(reaction);
				if(usedReactionbounds!=null){
	                double bondLength = GeometryTools.getBondLengthAverage(reaction);
	                Rectangle2D shiftedBounds =
	                    GeometryTools.shiftReactionVertical(
	                            reaction, reactionbounds, usedReactionbounds, bondLength);
	                usedReactionbounds = usedReactionbounds.createUnion(shiftedBounds);
	            } else {
	            	usedReactionbounds = reactionbounds;
	            }
			}
		}
		//then we shift the molecules not to overlap
		Rectangle2D usedBounds = null;
		if(chemModel.getMoleculeSet()!=null){
	        for (IAtomContainer container :
	        	AtomContainerSetManipulator.getAllAtomContainers(chemModel.getMoleculeSet())) {
	            // now move it so that they don't overlap
	            Rectangle2D bounds = BoundsCalculator.calculateBounds(container);
	            if (usedBounds != null) {
	                double bondLength = GeometryTools.getBondLengthAverage(container);
	                Rectangle2D shiftedBounds =
	                    GeometryTools.shiftContainer(
	                            container, bounds, usedBounds, bondLength);
	                usedBounds = usedBounds.createUnion(shiftedBounds);
	            } else {
	                usedBounds = bounds;
	            }
	        } 
		}
		//and the products/reactants in every reaction
		if(chemModel.getReactionSet()!=null){
			for(IReaction reaction : chemModel.getReactionSet().reactions()){
				usedBounds = null;
		        for (IAtomContainer container :
		        	ReactionManipulator.getAllAtomContainers(reaction)) {
		            // now move it so that they don't overlap
		            Rectangle2D bounds = BoundsCalculator.calculateBounds(container);
		            if (usedBounds != null) {
		                double bondLength = GeometryTools.getBondLengthAverage(container);
		                Rectangle2D shiftedBounds =
		                    GeometryTools.shiftContainer(
		                            container, bounds, usedBounds, bondLength);
		                usedBounds = usedBounds.createUnion(shiftedBounds);
		            } else {
		                usedBounds = bounds;
		            }
		        } 
			}
		}
		//TODO overlaps of molecules in molecule set and reactions (ok, not too common, but still...)
    }

    public void cleanup() {
        Map<IAtom, Point2d[]> coords = new HashMap<IAtom, Point2d[]>();
        if(chemModel.getMoleculeSet()==null || chemModel.getMoleculeSet().getAtomContainerCount()==0)
            return;
        //THIS CODE ASSUMES THERE IS ONLY ONE ATOMCONTAINER IN CHEMMODEL. This was
        //the policy agreed on, so I stick to it.
        IAtomContainer container = chemModel.getMoleculeSet().getAtomContainer(0);
        for (IAtom atom : container.atoms()){
        	Point2d[] coordsforatom = new Point2d[2];
            coordsforatom[1] = atom.getPoint2d();
            coords.put(atom, coordsforatom);
            atom.setPoint2d(null);
        }

        if (ConnectivityChecker.isConnected(container)) {
            generateNewCoordinates(container);
        } else {
            // deal with disconnected atom containers
            IMoleculeSet molecules =
                ConnectivityChecker.partitionIntoMolecules(container);
            for (IAtomContainer subContainer : molecules.molecules()) {
                generateNewCoordinates(subContainer);
            }
            Rectangle2D usedBounds = null;
            for (IAtomContainer subContainer : molecules.molecules()) {
                // now move it so that they don't overlap
                Rectangle2D bounds = BoundsCalculator.calculateBounds(subContainer);
                if (usedBounds != null) {
                    double bondLength = GeometryTools.getBondLengthAverage(subContainer);
                    Rectangle2D shiftedBounds =
                        GeometryTools.shiftContainer(
                                subContainer, bounds, usedBounds, bondLength);
                    usedBounds = usedBounds.createUnion(shiftedBounds);
                } else {
                    usedBounds = bounds;
                }
            } 


        }

        for (IAtom atom : container.atoms()) {
            Point2d[] coordsforatom = coords.get(atom);
            coordsforatom[0] = atom.getPoint2d();
        }
        coordinatesChanged();
	    if (getUndoRedoFactory() != null && getUndoRedoHandler() != null) {
            IUndoRedoable undoredo =
                getUndoRedoFactory().getChangeCoordsEdit(coords, "Clean Up");
            getUndoRedoHandler().postEdit(undoredo);
        }
    }

    public static void generateNewCoordinates(IAtomContainer container) {
        IChemObjectBuilder builder =
            NoNotificationChemObjectBuilder.getInstance();

        if (diagramGenerator == null) {
            diagramGenerator = new StructureDiagramGenerator();
            diagramGenerator.setTemplateHandler(
                new TemplateHandler(builder)
            );
        }
        if (container instanceof IMolecule) {
            diagramGenerator.setMolecule((IMolecule)container);
        } else {
            diagramGenerator.setMolecule(builder.newInstance(
            	IMolecule.class, container
            ));
        }

        try {
            diagramGenerator.generateExperimentalCoordinates();
            IMolecule cleanedMol = diagramGenerator.getMolecule();
            // now copy/paste coordinates
            for (int i = 0; i < cleanedMol.getAtomCount(); i++) {
                container.getAtom(i).setPoint2d(
                     cleanedMol.getAtom(i).getPoint2d()
                );
            }
        } catch ( Exception e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public IRing addRing(int ringSize, Point2d worldcoord) {
        IRing ring = chemModel.getBuilder().newInstance(
        	IRing.class, ringSize, "C"
        );
        double bondLength = 1.4;
        ringPlacer.placeRing(ring, worldcoord, bondLength);
        IMoleculeSet set = chemModel.getMoleculeSet();

        // the molecule set should not be null, but just in case...
        if (set == null) {
            set = chemModel.getBuilder().newInstance(IMoleculeSet.class);
            chemModel.setMoleculeSet(set);
        }
        IMolecule container = set.getMolecule(0);
        if (container == null) {
            container = set.getBuilder().newInstance(IMolecule.class);
            set.addAtomContainer(container);
        }
        container.add(ring);
	    updateAtoms(ring, ring.atoms());
        structureChanged();
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
		    IUndoRedoable undoredo = getUndoRedoFactory().getAddAtomsAndBondsEdit(
		    	getIChemModel(),
		    	ring.getBuilder().newInstance(IAtomContainer.class,ring),
		    	"Ring" + " " + ringSize,this
		    );
		    getUndoRedoHandler().postEdit(undoredo);
	    }
	    return ring;
    }

    public IRing addPhenyl(Point2d worldcoord) {
        IRing ring = chemModel.getBuilder().newInstance(IRing.class,6, "C");
        ring.getBond(0).setOrder(IBond.Order.DOUBLE);
        ring.getBond(2).setOrder(IBond.Order.DOUBLE);
        ring.getBond(4).setOrder(IBond.Order.DOUBLE);
        makeRingAromatic(ring);

        double bondLength = 1.4;
        ringPlacer.placeRing(ring, worldcoord, bondLength);
        IMoleculeSet set = chemModel.getMoleculeSet();

        // the molecule set should not be null, but just in case...
        if (set == null) {
            set = chemModel.getBuilder().newInstance(IMoleculeSet.class);
            chemModel.setMoleculeSet(set);
        }
        IMolecule container = set.getMolecule(0);
        if (container == null) {
            container = set.getBuilder().newInstance(IMolecule.class);
            set.addAtomContainer(container);
        }
        container.add(ring);
        updateAtoms(ring, ring.atoms());
        structureChanged();
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
		    IUndoRedoable undoredo = getUndoRedoFactory().getAddAtomsAndBondsEdit(
		    	getIChemModel(),
		    	ring.getBuilder().newInstance(IAtomContainer.class, ring),
		    	"Benzene",this
		    );
		    getUndoRedoHandler().postEdit(undoredo);
	    }
	    return ring;
    }

    public IRing addRing(IAtom atom, int ringSize) {
        IAtomContainer sourceContainer
            = ChemModelManipulator.getRelevantAtomContainer(chemModel, atom);
        IAtomContainer sharedAtoms = atom.getBuilder().newInstance(IAtomContainer.class);
        sharedAtoms.addAtom(atom);

        IRing newRing = createAttachRing(sharedAtoms, ringSize, "C");
        double bondLength = GeometryTools.getBondLengthAverage(sourceContainer);
        Point2d conAtomsCenter = getConnectedAtomsCenter(sharedAtoms, chemModel);

        Point2d sharedAtomsCenter = atom.getPoint2d();
        Vector2d ringCenterVector = new Vector2d(sharedAtomsCenter);
        ringCenterVector.sub(conAtomsCenter);
        ringPlacer.placeSpiroRing(
                newRing, sharedAtoms, sharedAtomsCenter, ringCenterVector, bondLength);

        for (IAtom ringAtom : newRing.atoms()) {
            if (ringAtom != atom) sourceContainer.addAtom(ringAtom);
        }

        for (IBond ringBond : newRing.bonds()) {
            sourceContainer.addBond(ringBond);
        }
        updateAtoms(newRing, newRing.atoms());
        
        RendererModel rModel = this.getRenderer().getRenderer2DModel();
        double d = rModel.getParameter(
                HighlightAtomDistance.class).getValue() /
            rModel.getParameter(Scale.class).getValue();
        for (IAtom newatom : newRing.atoms()) {
            if (atom != newatom && getClosestAtom(atom) != null) {
                atom.getPoint2d().x += d;
            }
        }
        structureChanged();
        return newRing;
    }

    public IRing addPhenyl(IAtom atom) {
        IAtomContainer sourceContainer
            = ChemModelManipulator.getRelevantAtomContainer(chemModel, atom);
        IAtomContainer sharedAtoms = atom.getBuilder().newInstance(IAtomContainer.class);
        sharedAtoms.addAtom(atom);

        // make a benzene ring
        IRing newRing = createAttachRing(sharedAtoms, 6, "C");
        newRing.getBond(0).setOrder(IBond.Order.DOUBLE);
        newRing.getBond(2).setOrder(IBond.Order.DOUBLE);
        newRing.getBond(4).setOrder(IBond.Order.DOUBLE);
        makeRingAromatic(newRing);

        double bondLength;
        if (sourceContainer.getBondCount() == 0) {
            /*
             * Special case of adding a ring to a single, unconnected atom
             * - places the ring centered on the place where the atom was.
             */
            bondLength = 1.4;
            Point2d ringCenter = new Point2d(atom.getPoint2d());
            ringPlacer.placeRing(newRing, ringCenter, bondLength);
        } else {
            bondLength = GeometryTools.getBondLengthAverage(sourceContainer);
            Point2d conAtomsCenter = getConnectedAtomsCenter(sharedAtoms, chemModel);

            Point2d sharedAtomsCenter = atom.getPoint2d();
            Vector2d ringCenterVector = new Vector2d(sharedAtomsCenter);
            ringCenterVector.sub(conAtomsCenter);
            ringPlacer.placeSpiroRing(newRing,
                                      sharedAtoms,
                                      sharedAtomsCenter,
                                      ringCenterVector,
                                      bondLength);
        }

        // add the ring to the source container
        for (IAtom ringAtom : newRing.atoms()) {
            if (ringAtom != atom) sourceContainer.addAtom(ringAtom);
        }

        for (IBond ringBond : newRing.bonds()) {
            sourceContainer.addBond(ringBond);
        }
        updateAtoms(newRing, newRing.atoms());
        for (IAtom newatom : newRing.atoms()) {
            if (atom != newatom && getClosestAtom(atom) != null) {
                RendererModel rModel = this.getRenderer().getRenderer2DModel();
                double d =  rModel.getParameter(
                        HighlightAtomDistance.class).getValue() /
                    rModel.getParameter(Scale.class).getValue();
                atom.getPoint2d().x += d;
            }
        }
        structureChanged();
        return newRing;
    }

    private void makeRingAromatic(IRing newRing) {
        for (IAtom atom : newRing.atoms())
            atom.setFlag(CDKConstants.ISAROMATIC, true);
        for (IBond bond : newRing.bonds())
            bond.setFlag(CDKConstants.ISAROMATIC, true);
    }

    /**
     * Constructs a new Ring of a certain size that contains all the atoms and
     * bonds of the given AtomContainer and is filled up with new Atoms and Bonds.
     *
     * @param  sharedAtoms  The AtomContainer containing the Atoms and bonds for the
     *                      new Ring
     * @param  ringSize     The size (number of Atoms) the Ring will have
     * @param  symbol       The element symbol the new atoms will have
     * @return              The constructed Ring
     */
    private IRing createAttachRing(IAtomContainer sharedAtoms, int ringSize, String symbol) {
        IRing newRing = sharedAtoms.getBuilder().newInstance(IRing.class,ringSize);
        IAtom[] ringAtoms = new IAtom[ringSize];
        for (int i = 0; i < sharedAtoms.getAtomCount(); i++) {
            ringAtoms[i] = sharedAtoms.getAtom(i);
        }
        for (int i = sharedAtoms.getAtomCount(); i < ringSize; i++) {
            ringAtoms[i] = sharedAtoms.getBuilder().newInstance(IAtom.class,symbol);
        }
        for (IBond bond : sharedAtoms.bonds()) newRing.addBond(bond);
        for (int i = sharedAtoms.getBondCount(); i < ringSize - 1; i++) {
            newRing.addBond(sharedAtoms.getBuilder().newInstance(IBond.class,
                ringAtoms[i], ringAtoms[i + 1], IBond.Order.SINGLE)
            );
        }
        newRing.addBond(sharedAtoms.getBuilder().newInstance(IBond.class,
            ringAtoms[ringSize - 1], ringAtoms[0], IBond.Order.SINGLE)
        );
        newRing.setAtoms(ringAtoms);
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
	    	IAtomContainer undoRedoContainer = newRing.getBuilder().newInstance(
	    		IAtomContainer.class, newRing
	    	);
	    	for(IAtom atom : sharedAtoms.atoms())
	    		undoRedoContainer.removeAtom(atom);
	    	for(IBond bond : sharedAtoms.bonds())
	    		undoRedoContainer.removeBond(bond);
		    IUndoRedoable undoredo = getUndoRedoFactory().getAddAtomsAndBondsEdit(getIChemModel(), undoRedoContainer, "Ring" + " " + ringSize,this);
		    getUndoRedoHandler().postEdit(undoredo);
	    }
        return newRing;
    }

    /**
     * Searches all the atoms attached to the Atoms in the given AtomContainer and
     * calculates the center point of them.
     *
     * @param  sharedAtoms  The Atoms the attached partners are searched of
     * @return              The Center Point of all the atoms found
     */
    private Point2d getConnectedAtomsCenter(IAtomContainer sharedAtoms,
                                            IChemModel chemModel) {
        IAtomContainer conAtoms = sharedAtoms.getBuilder().newInstance(IAtomContainer.class);
        for (IAtom sharedAtom : sharedAtoms.atoms()) {
            conAtoms.addAtom(sharedAtom);
            IAtomContainer atomCon =
                ChemModelManipulator.getRelevantAtomContainer(chemModel,
                                                              sharedAtom);
            for (IAtom atom : atomCon.getConnectedAtomsList(sharedAtom)) {
                conAtoms.addAtom(atom);
            }
        }
        return GeometryTools.get2DCenter(conAtoms);
    }

    public IRing addRing(IBond bond, int size) {
        IAtomContainer sharedAtoms = bond.getBuilder().newInstance(IAtomContainer.class);
        IAtom firstAtom = bond.getAtom(0); // Assumes two-atom bonds only
        IAtom secondAtom = bond.getAtom(1);
        sharedAtoms.addAtom(firstAtom);
        sharedAtoms.addAtom(secondAtom);
        sharedAtoms.addBond(bond);
        IAtomContainer sourceContainer = ChemModelManipulator
            .getRelevantAtomContainer(chemModel, firstAtom);

        Point2d sharedAtomsCenter = GeometryTools.get2DCenter(sharedAtoms);

        // calculate two points that are perpendicular to the highlighted bond
        // and have a certain distance from the bond center
        Point2d firstPoint = firstAtom.getPoint2d();
        Point2d secondPoint = secondAtom.getPoint2d();
        Vector2d diff = new Vector2d(secondPoint);
        diff.sub(firstPoint);
        double bondLength = firstPoint.distance(secondPoint);
        double angle = GeometryTools.getAngle(diff.x, diff.y);
        Point2d newPoint1 = new Point2d( // FIXME: what is this point??
            (Math.cos(angle + (Math.PI/2))*bondLength/4) + sharedAtomsCenter.x,
            (Math.sin(angle + (Math.PI/2))*bondLength/4) + sharedAtomsCenter.y
        );
        Point2d newPoint2 = new Point2d( // FIXME: what is this point??
            (Math.cos(angle - (Math.PI/2))*bondLength/4) + sharedAtomsCenter.x,
            (Math.sin(angle - (Math.PI/2))*bondLength/4) + sharedAtomsCenter.y
        );

        // decide on which side to draw the ring??
        IAtomContainer connectedAtoms = bond.getBuilder().newInstance(IAtomContainer.class);
        for (IAtom atom : sourceContainer.getConnectedAtomsList(firstAtom)) {
            if (atom != secondAtom) connectedAtoms.addAtom(atom);
        }
        for (IAtom atom : sourceContainer.getConnectedAtomsList(secondAtom)) {
            if (atom != firstAtom) connectedAtoms.addAtom(atom);
        }
        Point2d conAtomsCenter = GeometryTools.get2DCenter(connectedAtoms);
        double distance1 = newPoint1.distance(conAtomsCenter);
        double distance2 = newPoint2.distance(conAtomsCenter);
        Vector2d ringCenterVector = new Vector2d(sharedAtomsCenter);
        if (distance1 < distance2) {
            ringCenterVector.sub(newPoint1);
        } else { // distance2 <= distance1
            ringCenterVector.sub(newPoint2);
        }

        // construct a new Ring that contains the highlighted bond an its two atoms
        IRing newRing = createAttachRing(sharedAtoms, size, "C");
        ringPlacer.placeFusedRing(newRing, sharedAtoms, sharedAtomsCenter,
                                  ringCenterVector, bondLength);
        // add the new atoms and bonds
        for (IAtom ringAtom : newRing.atoms()) {
            if (ringAtom != firstAtom && ringAtom != secondAtom) {
                sourceContainer.addAtom(ringAtom);
            }
        }
        for (IBond ringBond : newRing.bonds()) {
            if (ringBond != bond) {
                sourceContainer.addBond(ringBond);
            }
        }
        updateAtoms(newRing, newRing.atoms());
        
        RendererModel rModel = this.getRenderer().getRenderer2DModel();
        double d =  rModel.getParameter(
                HighlightAtomDistance.class).getValue() /
            rModel.getParameter(Scale.class).getValue();
        for(IAtom atom : newRing.atoms()){
        	if (atom != firstAtom && atom != secondAtom
                    && getClosestAtom(atom) != null) {
        		atom.getPoint2d().x += d;
        	}
        }
        structureChanged();
        return newRing;
    }
    
    public IAtom getClosestAtom(IAtom atom) {
        return getAtomInRange(null, atom);
    }

    public IAtom getAtomInRange(Collection<IAtom> toIgnore, IAtom atom) {
        Point2d atomPosition = atom.getPoint2d();
        RendererModel rModel = this.getRenderer().getRenderer2DModel();
        double highlight = rModel.getParameter(
                HighlightAtomDistance.class).getValue() /
            rModel.getParameter(Scale.class).getValue();
        
        IAtom bestClosestAtom = null;
        double bestDistance = -1;
        for (IAtomContainer atomContainer : 
            ChemModelManipulator.getAllAtomContainers(getIChemModel())) {

            IAtom closestAtom = GeometryTools.getClosestAtom(atomContainer, atom);
            
            if (closestAtom != null) {
                double distance = closestAtom.getPoint2d().distance(atomPosition); 
                if ((distance > highlight) || 
                        (toIgnore != null && toIgnore.contains(closestAtom))) {
                    continue;
                } else {
                    if (bestClosestAtom == null || distance < bestDistance) {
                        bestClosestAtom = closestAtom;
                        bestDistance = distance;
                    }
                }
            }
        }
        return bestClosestAtom;
    }

    public IRing addPhenyl(IBond bond) {
        IAtomContainer sharedAtoms = bond.getBuilder().newInstance(IAtomContainer.class);
        IAtom firstAtom = bond.getAtom(0); // Assumes two-atom bonds only
        IAtom secondAtom = bond.getAtom(1);
        sharedAtoms.addAtom(firstAtom);
        sharedAtoms.addAtom(secondAtom);
        sharedAtoms.addBond(bond);
        IAtomContainer sourceContainer = ChemModelManipulator
            .getRelevantAtomContainer(chemModel, firstAtom);

        Point2d sharedAtomsCenter = GeometryTools.get2DCenter(sharedAtoms);

        // calculate two points that are perpendicular to the highlighted bond
        // and have a certain distance from the bond center
        Point2d firstPoint = firstAtom.getPoint2d();
        Point2d secondPoint = secondAtom.getPoint2d();
        Vector2d diff = new Vector2d(secondPoint);
        diff.sub(firstPoint);
        double bondLength = firstPoint.distance(secondPoint);
        double angle = GeometryTools.getAngle(diff.x, diff.y);
        Point2d newPoint1 = new Point2d( // FIXME: what is this point??
            (Math.cos(angle + (Math.PI/2))*bondLength/4) + sharedAtomsCenter.x,
            (Math.sin(angle + (Math.PI/2))*bondLength/4) + sharedAtomsCenter.y
        );
        Point2d newPoint2 = new Point2d( // FIXME: what is this point??
            (Math.cos(angle - (Math.PI/2))*bondLength/4) + sharedAtomsCenter.x,
            (Math.sin(angle - (Math.PI/2))*bondLength/4) + sharedAtomsCenter.y
        );

        // decide on which side to draw the ring??
        IAtomContainer connectedAtoms = bond.getBuilder().newInstance(IAtomContainer.class);
        for (IAtom atom : sourceContainer.getConnectedAtomsList(firstAtom)) {
            if (atom != secondAtom) connectedAtoms.addAtom(atom);
        }
        for (IAtom atom : sourceContainer.getConnectedAtomsList(secondAtom)) {
            if (atom != firstAtom) connectedAtoms.addAtom(atom);
        }
        Point2d conAtomsCenter = GeometryTools.get2DCenter(connectedAtoms);
        double distance1 = newPoint1.distance(conAtomsCenter);
        double distance2 = newPoint2.distance(conAtomsCenter);
        Vector2d ringCenterVector = new Vector2d(sharedAtomsCenter);
        if (distance1 < distance2) {
            ringCenterVector.sub(newPoint1);
        } else { // distance2 <= distance1
            ringCenterVector.sub(newPoint2);
        }

        // construct a new Ring that contains the highlighted bond an its two atoms
        IRing newRing = createAttachRing(sharedAtoms, 6, "C");
        makeRingAromatic(newRing);
        ringPlacer.placeFusedRing(newRing, sharedAtoms, sharedAtomsCenter,
                                  ringCenterVector, bondLength);
        if (sourceContainer.getMaximumBondOrder(bond.getAtom(0)) == IBond.Order.SINGLE &&
            sourceContainer.getMaximumBondOrder(bond.getAtom(1)) == IBond.Order.SINGLE) {
            newRing.getBond(1).setOrder(IBond.Order.DOUBLE);
            newRing.getBond(3).setOrder(IBond.Order.DOUBLE);
            newRing.getBond(5).setOrder(IBond.Order.DOUBLE);
        } else { // assume Order.DOUBLE, so only need to add 2 double bonds
            newRing.getBond(2).setOrder(IBond.Order.DOUBLE);
            newRing.getBond(4).setOrder(IBond.Order.DOUBLE);
        }
        // add the new atoms and bonds
        for (IAtom ringAtom : newRing.atoms()) {
            if (ringAtom != firstAtom && ringAtom != secondAtom) {
                sourceContainer.addAtom(ringAtom);
            }
        }
        for (IBond ringBond : newRing.bonds()) {
            if (ringBond != bond) {
                sourceContainer.addBond(ringBond);
            }
        }
        updateAtoms(newRing, newRing.atoms());
        
        RendererModel rModel = this.getRenderer().getRenderer2DModel();
        double d = rModel.getParameter(
                HighlightAtomDistance.class).getValue()/
                   rModel.getParameter(Scale.class).getValue();
        for (IAtom atom : newRing.atoms()) {
        	if (atom != firstAtom 
        	        && atom != secondAtom
                    && getClosestAtom(atom) != null) {
        		atom.getPoint2d().x += d;
        	}
        }
        structureChanged();
        return newRing;
    }

    public void removeBondWithoutUndo(IBond bond) {
        IAtomContainer sourceContainer = ChemModelManipulator
            .getRelevantAtomContainer(chemModel, bond);
        if(sourceContainer!=null)
        	sourceContainer.removeBond(bond);
        updateAtom(bond.getAtom(0));
        updateAtom(bond.getAtom(1));
        structureChanged();
    }

    public void removeBond(IBond bond) {
        removeBondWithoutUndo(bond);
        IAtomContainer undAtomContainer = bond.getBuilder().newInstance(IAtomContainer.class);
        undAtomContainer.addBond(bond);
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
		    IUndoRedoable undoredo = getUndoRedoFactory().getRemoveAtomsAndBondsEdit(getIChemModel(), undAtomContainer,"Remove Bond",this);
		    getUndoRedoHandler().postEdit(undoredo);
	    }
    }

    public void addPhantomAtom( IAtom atom ) {
        this.phantoms.addAtom(atom);
    }

    public void addPhantomBond( IBond bond ) {
        this.phantoms.addBond(bond);
    }

    public void clearPhantoms() {
        this.phantoms.removeAllElements();
    }

    public IAtomContainer getPhantoms() {
        return this.phantoms;
    }

	public void adjustBondOrders()  throws IOException, ClassNotFoundException, CDKException {
		//TODO also work on reactions ?!?
		SaturationChecker satChecker = new SaturationChecker();
		List<IAtomContainer> containersList = ChemModelManipulator.getAllAtomContainers(chemModel);
		Iterator<IAtomContainer> iterator = containersList.iterator();
		Map<IBond, IBond.Order[]> changedBonds = new HashMap<IBond, IBond.Order[]>();
		while(iterator.hasNext())
		{
			IAtomContainer ac = (IAtomContainer)iterator.next();
			for(IBond bond : ac.bonds()){
				IBond.Order[] orders=new IBond.Order[2];
				orders[1]=bond.getOrder();
				changedBonds.put(bond,orders);
			}
			satChecker.saturate(ac);
			for(IBond bond : ac.bonds()){
				IBond.Order[] orders=changedBonds.get(bond);
				orders[0]=bond.getOrder();
				changedBonds.put(bond,orders);
			}
		}
		if(this.getController2DModel().getAutoUpdateImplicitHydrogens())
			updateImplicitHydrogenCounts();
	    if(undoredofactory!=null && undoredohandler!=null){
	    	IUndoRedoable undoredo = undoredofactory.
	    	    getAdjustBondOrdersEdit(
	    	    	changedBonds, new HashMap<IBond, IBond.Stereo[]>(),
	    	    	"Adjust Bond Order of Molecules",this
	    	    );
		    undoredohandler.postEdit(undoredo);
	    }
	}

	public void resetBondOrders(){
		//TODO also work on reactions ?!?
		List<IAtomContainer> containersList = ChemModelManipulator.getAllAtomContainers(chemModel);
		Iterator<IAtomContainer> iterator = containersList.iterator();
		Map<IBond, IBond.Order[]> changedBonds = new HashMap<IBond, IBond.Order[]>();
		while(iterator.hasNext())
		{
			IAtomContainer ac = iterator.next();
			for(IBond bond : ac.bonds()){
				IBond.Order[] orders=new IBond.Order[2];
				orders[1]=bond.getOrder();
				orders[0]=Order.SINGLE;
				changedBonds.put(bond,orders);
                bond.setOrder(Order.SINGLE);
			}
		}
		if(this.getController2DModel().getAutoUpdateImplicitHydrogens())
			updateImplicitHydrogenCounts();
	    if(undoredofactory!=null && undoredohandler!=null){
	    	IUndoRedoable undoredo = undoredofactory.
	    		getAdjustBondOrdersEdit(
	    			changedBonds,
	    			new HashMap<IBond, IBond.Stereo[]>(),
	    			"Reset Bond Order of Molecules",this
	    		);
		    undoredohandler.postEdit(undoredo);
	    }
	}

	public void replaceAtom(IAtom atomnew, IAtom atomold) {
        IAtomContainer relevantContainer = ChemModelManipulator.getRelevantAtomContainer(chemModel, atomold);
        AtomContainerManipulator.replaceAtomByAtom(relevantContainer,
            atomold, atomnew);
        updateAtom(atomnew);
        structureChanged();
	    if(undoredofactory!=null && undoredohandler!=null){
	    	IUndoRedoable undoredo = undoredofactory.getReplaceAtomEdit(chemModel, atomold, atomnew, "Replace Atom");
		    undoredohandler.postEdit(undoredo);
	    }
	}

	public void addSingleElectron(IAtom atom) {
        IAtomContainer relevantContainer = ChemModelManipulator.getRelevantAtomContainer(chemModel, atom);
    	ISingleElectron singleElectron = atom.getBuilder().newInstance(
    		ISingleElectron.class, atom
    	);
        relevantContainer.addSingleElectron(singleElectron);
	    if(undoredofactory!=null && undoredohandler!=null){
	    	IUndoRedoable undoredo = undoredofactory.getConvertToRadicalEdit(relevantContainer, singleElectron, "Convert to Radical");
		    undoredohandler.postEdit(undoredo);
	    }
	}

	public void clearValidation() {
		Iterator<IAtomContainer> containers = ChemModelManipulator.getAllAtomContainers(chemModel).iterator();
		while (containers.hasNext()) {
			IAtomContainer atoms = containers.next();
			for (int i = 0; i < atoms.getAtomCount(); i++)
			{
				ProblemMarker.unmark(atoms.getAtom(i));
			}
		}
	}

	public void flip(boolean horizontal){
		HashMap<IAtom, Point2d[]> atomCoordsMap = new HashMap<IAtom, Point2d[]>();
		RendererModel renderModel = renderer.getRenderer2DModel();
		IAtomContainer toflip;
        if (renderModel.getSelection().getConnectedAtomContainer()!=null) {
            toflip = renderModel.getSelection().getConnectedAtomContainer();
        }else{
        	List<IAtomContainer> toflipall = ChemModelManipulator.getAllAtomContainers(chemModel);
        	toflip=toflipall.get(0).getBuilder().newInstance(IAtomContainer.class);
        	for (IAtomContainer atomContainer : toflipall) {
				toflip.add(atomContainer);
			}
        }
        Point2d center = GeometryTools.get2DCenter(toflip);
        for (int i=0; i<toflip.getAtomCount(); i++) {
        	IAtom atom = toflip.getAtom(i);
            Point2d p2d = atom.getPoint2d();
            Point2d oldCoord = new Point2d(p2d.x, p2d.y);
            if (horizontal) {
            	p2d.y = 2.0*center.y - p2d.y;
            } else {
            	p2d.x = 2.0*center.x - p2d.x;
            }
            Point2d newCoord = p2d;
            if (!oldCoord.equals(newCoord)) {
                Point2d[] coords = new Point2d[2];
                coords[0] = newCoord;
                coords[1] = oldCoord;
                atomCoordsMap.put(atom, coords);
            }
        }
        coordinatesChanged();
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
		    IUndoRedoable undoredo = getUndoRedoFactory().getChangeCoordsEdit(atomCoordsMap, "Clean Up");
		    getUndoRedoHandler().postEdit(undoredo);
	    }
	}

    public void setEventHandler(IChemModelEventRelayHandler handler) {
        this.changeHandler = handler;
    }

    private void structureChanged() {
        if(renderer.getRenderer2DModel().getSelection() instanceof IncrementalSelection)
            select( (IncrementalSelection)renderer.getRenderer2DModel().getSelection() );
        if (changeHandler != null) changeHandler.structureChanged();
    }

    public void fireZoomEvent() {
        changeHandler.zoomChanged();
    }
    public void fireStructureChangedEvent() {
    
         changeHandler.structureChanged();
    }

    private void structurePropertiesChanged() {
        if (changeHandler != null) changeHandler.structurePropertiesChanged();
    }

    private void coordinatesChanged() {
        if (changeHandler != null) changeHandler.coordinatesChanged();
    }

	public IUndoRedoFactory getUndoRedoFactory() {
		return undoredofactory;
	}

	public UndoRedoHandler getUndoRedoHandler() {
		return undoredohandler;
	}

    private void selectionChanged() {
        if (changeHandler != null) changeHandler.selectionChanged();
    }

    /*
     * Fill a selection by passing it to the hub, which uses its internal
     * chem model to do so.
     */
    public void select(IChemObjectSelection selection) {
        if(selection != null)
            selection.select(this.chemModel);
        selectionChanged();
    }

	public void addFragment(IAtomContainer toPaste) {
        IMoleculeSet moleculeSet = chemModel.getMoleculeSet();
        if (moleculeSet == null) {
            moleculeSet = chemModel.getBuilder().newInstance(IMoleculeSet.class);
        }
        moleculeSet.addAtomContainer(toPaste);
	    if(undoredofactory!=null && undoredohandler!=null){
		    IUndoRedoable undoredo = undoredofactory.getAddAtomsAndBondsEdit(
		    	getIChemModel(),
		    	toPaste.getBuilder().newInstance(IAtomContainer.class, toPaste),
		    	"Paste", this
		    );
		    undoredohandler.postEdit(undoredo);
	    }
	    updateAtoms(toPaste, toPaste.atoms());
	    structureChanged();
	}

	public IAtomContainer deleteFragment(IAtomContainer selected) {
		IAtomContainer removed = selected.getBuilder().newInstance(IAtomContainer.class);
		for (int i = 0; i < selected.getAtomCount(); i++) {
			removed.addAtom(selected.getAtom(i));
			Iterator<IBond> it = ChemModelManipulator.getRelevantAtomContainer(chemModel, selected.getAtom(i)).getConnectedBondsList(selected.getAtom(i)).iterator();
			while(it.hasNext()){
				IBond bond=it.next();
				if(!removed.contains(bond))
					removed.addBond(bond);
			}
			ChemModelManipulator.removeAtomAndConnectedElectronContainers(chemModel, selected.getAtom(i));
		}
	    if(undoredofactory!=null && undoredohandler!=null){
		    IUndoRedoable undoredo = undoredofactory.getRemoveAtomsAndBondsEdit(chemModel, removed, "Cut",this);
		    undoredohandler.postEdit(undoredo);
	    }
	    structureChanged();
	    return removed;
	}

	public void makeReactantInNewReaction(IAtomContainer newContainer, IAtomContainer oldcontainer) {
		IReaction reaction = newContainer.getBuilder().newInstance(IReaction.class);
		reaction.setID("reaction-" + System.currentTimeMillis());
		IMolecule mol=newContainer.getBuilder().newInstance(IMolecule.class, newContainer);
		mol.setID(newContainer.getID());
		reaction.addReactant(mol);
		IReactionSet reactionSet = chemModel.getReactionSet();
		if (reactionSet == null)
		{
			reactionSet = chemModel.getBuilder().newInstance(IReactionSet.class);
		}
		reactionSet.addReaction(reaction);
		chemModel.setReactionSet(reactionSet);
		chemModel.getMoleculeSet().removeAtomContainer(oldcontainer);
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
		    IUndoRedoable undoredo = getUndoRedoFactory().getMakeReactantOrProductInNewReactionEdit(chemModel, newContainer, oldcontainer, true, "Make Reactant in new Reaction");
		    getUndoRedoHandler().postEdit(undoredo);
	    }
	    structureChanged();
	}

	/* (non-Javadoc)
	 * @see org.openscience.cdk.controller.IChemModelRelay#makeReactantInExistingReaction(java.lang.String, org.openscience.cdk.interfaces.IAtomContainer, org.openscience.cdk.interfaces.IAtomContainer)
	 */
	public void makeReactantInExistingReaction(String reactionId,
			IAtomContainer newContainer, IAtomContainer oldcontainer) {
		IReaction reaction = ReactionSetManipulator.getReactionByReactionID(chemModel.getReactionSet(), reactionId);
		IMolecule mol=newContainer.getBuilder().newInstance(IMolecule.class, newContainer);
		mol.setID(newContainer.getID());
		reaction.addReactant(mol);
		chemModel.getMoleculeSet().removeAtomContainer(oldcontainer);
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
		    IUndoRedoable undoredo = getUndoRedoFactory().getMakeReactantOrProductInExistingReactionEdit(chemModel, newContainer, oldcontainer, reactionId, true, "Make Reactant in "+reactionId);
		    getUndoRedoHandler().postEdit(undoredo);
	    }
	    structureChanged();
	}

	public void makeProductInNewReaction(IAtomContainer newContainer,
			IAtomContainer oldcontainer) {
		IReaction reaction = newContainer.getBuilder().newInstance(IReaction.class);
		reaction.setID("reaction-" + System.currentTimeMillis());
		IMolecule mol=newContainer.getBuilder().newInstance(IMolecule.class, newContainer);
		mol.setID(newContainer.getID());
		reaction.addProduct(mol);
		IReactionSet reactionSet = chemModel.getReactionSet();
		if (reactionSet == null)
		{
			reactionSet = chemModel.getBuilder().newInstance(IReactionSet.class);
		}
		reactionSet.addReaction(reaction);
		chemModel.setReactionSet(reactionSet);
		chemModel.getMoleculeSet().removeAtomContainer(oldcontainer);
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
		    IUndoRedoable undoredo = getUndoRedoFactory().getMakeReactantOrProductInNewReactionEdit(chemModel, newContainer, oldcontainer, false, "Make Reactant in new Reaction");
		    getUndoRedoHandler().postEdit(undoredo);
	    }
	    structureChanged();
	}

	/* (non-Javadoc)
	 * @see org.openscience.cdk.controller.IChemModelRelay#makeProductInExistingReaction(java.lang.String, org.openscience.cdk.interfaces.IAtomContainer, org.openscience.cdk.interfaces.IAtomContainer)
	 */
	public void makeProductInExistingReaction(String reactionId,
			IAtomContainer newContainer, IAtomContainer oldcontainer) {
		IReaction reaction = ReactionSetManipulator.getReactionByReactionID(chemModel.getReactionSet(), reactionId);
		IMolecule mol=newContainer.getBuilder().newInstance(IMolecule.class,newContainer);
		mol.setID(newContainer.getID());
		reaction.addProduct(mol);
		chemModel.getMoleculeSet().removeAtomContainer(oldcontainer);
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
		    IUndoRedoable undoredo = getUndoRedoFactory().getMakeReactantOrProductInExistingReactionEdit(chemModel, newContainer, oldcontainer, reactionId, false, "Make Reactant in "+reactionId);
		    getUndoRedoHandler().postEdit(undoredo);
	    }
	    structureChanged();
	}

	/**
	 *  Updates an array of atoms with respect to its hydrogen count
	 *
	 *@param  container  The AtomContainer to work on
	 *@param  atoms       The Atoms to update
	 */
	public void updateAtoms(IAtomContainer container, Iterable<IAtom> atoms) {
        for (IAtom atom : atoms) {
            updateAtom(container, atom);
        }
    }


	/**
	 *  Updates an atom with respect to its hydrogen count
	 *
	 *@param  container  The AtomContainer to work on
	 *@param  atom       The Atom to update
	 */
	public void updateAtom(IAtom atom) {
        IAtomContainer container = 
            ChemModelManipulator.getRelevantAtomContainer(chemModel, atom);
        if (container != null) {
            updateAtom(container, atom);
        }
    }

	/**
	 *  Updates an atom with respect to its hydrogen count
	 *
	 *@param  container  The AtomContainer to work on
	 *@param  atom       The Atom to update
	 */
	private void updateAtom(IAtomContainer container, IAtom atom) {
        if (this.getController2DModel().getAutoUpdateImplicitHydrogens()) {
            atom.setImplicitHydrogenCount(0);
            try {
                IAtomType type = matcher.findMatchingAtomType(container, atom);
                if (type != null) {
                    Integer neighbourCount = type.getFormalNeighbourCount(); 
                    if (neighbourCount != null) {
                        atom.setImplicitHydrogenCount(
                                neighbourCount
                                - container.getConnectedAtomsCount(atom));
                    }
                }
            } catch (CDKException e) {
                e.printStackTrace();
            }
        }
    }

	public void makeAllExplicitImplicit() {
		IAtomContainer undoRedoContainer = chemModel.getBuilder().newInstance(IAtomContainer.class);
		List<IAtomContainer> containers = ChemModelManipulator.getAllAtomContainers(chemModel);
		for(int i=0;i<containers.size();i++){
			IAtomContainer removeatoms = chemModel.getBuilder().newInstance(IAtomContainer.class);
			for(IAtom atom : containers.get(i).atoms()){
				if(atom.getSymbol().equals("H")){
					removeatoms.addAtom(atom);
					removeatoms.addBond(containers.get(i).getConnectedBondsList(atom).get(0));
					containers.get(i).getConnectedAtomsList(atom).get(0).
					    setImplicitHydrogenCount(containers.get(i).
					        getConnectedAtomsList(atom).get(0).
					        getImplicitHydrogenCount()+1
					    );
				}
			}
			containers.get(i).remove(removeatoms);
			undoRedoContainer.add(removeatoms);
		}
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
		    IUndoRedoable undoredo = getUndoRedoFactory().getRemoveAtomsAndBondsEdit(chemModel, undoRedoContainer, "Make explicit Hs implicit",this);
		    getUndoRedoHandler().postEdit(undoredo);
	    }
		structureChanged();
	}

	public void makeAllImplicitExplicit() {
		IAtomContainer undoRedoContainer = chemModel.getBuilder().newInstance(IAtomContainer.class);
		List<IAtomContainer> containers = ChemModelManipulator.getAllAtomContainers(chemModel);
		for(int i=0;i<containers.size();i++){
			for(IAtom atom : containers.get(i).atoms()){
				int hcount=atom.getImplicitHydrogenCount();
				for(int k=0;k<hcount;k++){
					IAtom newAtom = this.addAtomWithoutUndo("H", atom);
			        IAtomContainer atomContainer =
			            ChemModelManipulator.getRelevantAtomContainer(
			                    getIChemModel(), newAtom);
			        IBond newBond = atomContainer.getBond(atom, newAtom);
			        undoRedoContainer.addAtom(newAtom);
			        undoRedoContainer.addBond(newBond);
				}
			}
		}
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
		    IUndoRedoable undoredo = getUndoRedoFactory().getAddAtomsAndBondsEdit(chemModel, undoRedoContainer, "Make implicit Hs explicit", this);
		    getUndoRedoHandler().postEdit(undoredo);
	    }
		structureChanged();
	}

	public void setHydrogenCount(IAtom atom, int intValue) {
	    if(getUndoRedoFactory()!=null && getUndoRedoHandler()!=null){
	    	HashMap<IAtom, Integer[]> atomhydrogenmap = new HashMap<IAtom, Integer[]>();
	    	atomhydrogenmap.put(atom, new Integer[]{
	    		intValue, atom.getImplicitHydrogenCount()
	    	});
		    IUndoRedoable undoredo = getUndoRedoFactory().getChangeHydrogenCountEdit(atomhydrogenmap, "Change hydrogen count to "+intValue);
		    getUndoRedoHandler().postEdit(undoredo);
	    }
		atom.setImplicitHydrogenCount(intValue);
		structureChanged();
	}
	
   /**
     * Merge molecules when a selection is moved onto another part 
     * of the molecule set
     *  
     * TODO: move this function into class MoveModule.java
     * 
     */
    public void mergeMolecules(Vector2d movedDistance) {
        RendererModel model = getRenderer().getRenderer2DModel();

        // First try to shift the selection to be exactly on top of
        // the target of the merge. This makes the end results visually
        // more attractive and avoid tilted rings
        //
        Map<IAtom, IAtom> mergeMap = model.getMerge();
        Iterator<IAtom> it = model.getMerge().keySet().iterator();
        if (it.hasNext()) {
            IAtomContainer movedAtomContainer = renderer.getRenderer2DModel()
                    .getSelection().getConnectedAtomContainer();
            if (movedAtomContainer != null) {
                IAtom atomA = (IAtom) it.next();
                IAtom atomB = mergeMap.get(atomA);
                Vector2d shift = new Vector2d();
                shift.sub(atomB.getPoint2d(), atomA.getPoint2d());
                
                for (IAtom shiftAtom : movedAtomContainer.atoms()) {
                    shiftAtom.getPoint2d().add( shift );
                }
            }
        }
        
        //Done shifting, now the actual merging.
        it = model.getMerge().keySet().iterator();
        while (it.hasNext()) {
            List<IBond> removedBonds = new ArrayList<IBond>();
            Map<IBond, Integer> bondsWithReplacedAtoms = new HashMap<IBond, Integer>();
            IAtom mergedAtom = (IAtom) it.next();
            IAtom mergedPartnerAtom = model.getMerge().get(mergedAtom);

            IAtomContainer container = ChemModelManipulator
                    .getRelevantAtomContainer(chemModel, mergedAtom);
            IAtomContainer container2 = ChemModelManipulator
                    .getRelevantAtomContainer(chemModel, mergedPartnerAtom);

            // In the next loop we remove bonds that are redundant, that is
            // to say bonds that exist on both sides of the parts to be merged
            // and would cause duplicate bonding in the end result.
            for (IAtom atom : container.atoms()) {

                if (!atom.equals(mergedAtom)) {
                    if (container.getBond(mergedAtom, atom) != null) {
                        if (model.getMerge().containsKey(atom)) {
                            for (IAtom atom2 : container2.atoms()) {
                                if (!atom2.equals(mergedPartnerAtom)) {
                                    if (container.getBond(mergedPartnerAtom,
                                            atom2) != null) {
                                        if (model.getMerge().get(atom).equals(
                                                atom2)) {
                                            System.out.println("removing ");
                                            IBond redundantBond = container
                                                    .getBond(atom, mergedAtom);
                                            container.removeBond(redundantBond);
                                            removedBonds.add(redundantBond);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //After the removal of redundant bonds, the actual merge is done.
            //One half of atoms in the merge map are removed and their bonds
            //are mapped to their replacement atoms.
            for (IBond bond : container.bonds()) {
                if (bond.contains(mergedAtom)) {
                    if (bond.getAtom(0).equals(mergedAtom)) {
                        bond.setAtom(mergedPartnerAtom, 0);
                        bondsWithReplacedAtoms.put(bond, 0);
                    } else {
                        bond.setAtom(mergedPartnerAtom, 1);
                        bondsWithReplacedAtoms.put(bond, 1);
                    }
                }
            }
            container.removeAtom(mergedAtom);
            updateAtom(mergedPartnerAtom);

            // Undo section to undo/redo the merge
            IUndoRedoFactory factory = getUndoRedoFactory();
            UndoRedoHandler handler = getUndoRedoHandler();
            if (factory != null && handler != null) {
                IUndoRedoable undoredo = factory.getMergeMoleculesEdit(
                        mergedAtom, container, removedBonds,
                        bondsWithReplacedAtoms, movedDistance,
                        mergedPartnerAtom, "Merge atom", this);
                handler.postEdit(undoredo);
            }
        }
        model.getMerge().clear();
        structureChanged();
    }



    public void execute( IEdit edit ) {

        IAtomContainer ac = chemModel.getMoleculeSet().getAtomContainer( 0 );
        edit.execute(ac);
        if( edit instanceof OptionalUndoEdit
            && !((OptionalUndoEdit)edit).isFinal())
            ;//The optional edit is not final so do not put in on the undo stack
        else
            postEdit( edit );

        for(IAtom atom:edit.getAtomsToUpdate()) {
            updateAtom( ac, atom );
        }
        fireEvents( edit.getTypeOfChanges() );
    }

    private void fireEvents(Collection<Changed> events) {
        for(Changed changed:events) {
            switch(changed) {
                case Structure: changeHandler.structureChanged();break;
                case Properties: changeHandler.structurePropertiesChanged();break;
                case Coordinates: changeHandler.coordinatesChanged();break;
                case Selection: changeHandler.selectionChanged();break;
                case Zoom: changeHandler.zoomChanged();break;
            }
        }
    }

    private void postEdit(final IEdit edit) {
        IUndoRedoFactory factory = getUndoRedoFactory();
        UndoRedoHandler handler = getUndoRedoHandler();
        if(factory!=null && handler != null) {
            handler.postEdit(factory.getEditOperation( edit));
        }
    }
}
