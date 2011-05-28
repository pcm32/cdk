/* $Revision: 7636 $ $Author: nielsout $ $Date: 2007-09-02 11:46:10 +0100 (su, 02 sep 2007) $
 *
 * Copyright (C) 2007  Egon Willighagen <egonw@users.lists.sf>
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

import javax.vecmath.Point2d;

import org.openscience.cdk.controller.edit.IEdit;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.renderer.IRenderer;
import org.openscience.cdk.renderer.RendererModel;

/**
 * @cdk.module control
 */
public interface IChemModelRelay extends IAtomBondEdits, IOldChemModelRelay {

	public enum Direction { UP, DOWN, UNDEFINED, EZ_UNDEFINED };

    public RendererModel getRenderModel();
    public IControllerModel getControlModel();

    /* Interaction*/
    public IRenderer getRenderer();
    public IAtom getClosestAtom(Point2d worldCoord);
    public IBond getClosestBond(Point2d worldCoord);

    /**
     * Adds an temporary atom which can be cleared later, when the final
     * atom is added. Controllers can use this to draw temporary atoms, for
     * example while drawing new bonds.
     *
     * @param atom atom to add as phantom
     */
    public void addPhantomAtom(IAtom atom);

    /**
     * Adds an temporary bond which can be cleared later, when the final
     * bond is added. Controllers can use this to draw temporary bonds, for
     * example while drawing new bonds.
     *
     * @param bond bond to add as phantom
     */
    public void addPhantomBond(IBond bond);

    /**
     * Returns an IAtomContainer containing all phantom atoms and bonds.
     */
    public IAtomContainer getPhantoms();
    /**
     * Deletes all temporary atoms.
     */
    public void clearPhantoms();

    public void setEventHandler(IChemModelEventRelayHandler handler);

    public void execute( IEdit edit );

}
