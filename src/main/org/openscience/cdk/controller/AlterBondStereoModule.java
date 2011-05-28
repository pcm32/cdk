/* $Revision$ $Author$ $Date$
 *
 * Copyright (C) 2008  Gilleain Torrance <gilleain.torrance@gmail.com>
 * Copyright (C) 2008  Stefan Kuhn (undo redo)
 * Copyright (C) 2009-2010  Arvid Berg <goglepox@users.sf.net>
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

import org.openscience.cdk.controller.IChemModelRelay.Direction;
import org.openscience.cdk.controller.edit.AppendAtom;
import org.openscience.cdk.controller.edit.SetStereo;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IBond.Stereo;
import org.openscience.cdk.renderer.selection.AbstractSelection;


/**
 * Alters the chirality of a bond, setting it to be into or out of the plane.
 *
 * @author maclean
 * @cdk.svnrev $Revision$
 * @cdk.module controlbasic
 */
public class AlterBondStereoModule extends ControllerModuleAdapter {

    Stereo[] switchOrderUp = new Stereo[] {Stereo.UP,Stereo.UP_INVERTED,Stereo.NONE};
    Stereo[] switchOrderDown = new Stereo[] {Stereo.DOWN,Stereo.DOWN_INVERTED,Stereo.NONE};

    private IBond.Stereo desiredDirection;

	public AlterBondStereoModule(
			IChemModelRelay chemModelRelay, Direction desiredDirection) {
		super(chemModelRelay);
		this.desiredDirection = desiredDirection==Direction.UP
		                ?IBond.Stereo.UP:IBond.Stereo.DOWN;
	}

	public void mouseClickedDown(Point2d worldCoord) {
		IAtom atom = this.chemModelRelay.getClosestAtom(worldCoord);
		IBond bond = this.chemModelRelay.getClosestBond(worldCoord);

		IChemObject singleSelection = getHighlighted(worldCoord, atom, bond);

		if (singleSelection == null) {
			setSelection(AbstractSelection.EMPTY_SELECTION);
		} else if (singleSelection instanceof IAtom) {
		    chemModelRelay.execute( AppendAtom.appendAtom( "C", atom, desiredDirection ) );
		} else if (singleSelection instanceof IBond) {
		    Stereo bondStereo = bond.getStereo();
		    switch(desiredDirection) {
		        case UP: setStereo(bond, findNextStereoMode(bondStereo, switchOrderUp));break;
		        case DOWN: setStereo(bond, findNextStereoMode(bondStereo, switchOrderDown));break;
		    }
		} else {
			// by default, change the bond
		    setStereo( bond, desiredDirection );
		}

	}

	private void setStereo(IBond bond, Stereo mode) {
	    super.chemModelRelay.execute(SetStereo.setStereo(bond, mode));
	}

  private Stereo findNextStereoMode(Stereo current,Stereo[] switchOrder) {
      for(int i=0;i<switchOrder.length;i++) {
          if(current == switchOrder[i])
              return switchOrder[i+1==switchOrder.length?0:i+1];
      }
      return switchOrder[0];
  }

	public String getDrawModeString() {
		if (desiredDirection == IBond.Stereo.UP) {
			return "Add or convert to bond up";
		} else {
			return "Add or convert to bond down";
		}
	}
}
