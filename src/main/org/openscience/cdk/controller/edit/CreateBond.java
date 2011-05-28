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

import java.util.Set;

import javax.vecmath.Point2d;

import org.openscience.cdk.controller.Changed;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;

/**
 * Edit representing the creation of a bond.
 * @author Arvid
 * @cdk.module controlbasic
 */
public class CreateBond extends AbstractEdit implements IEdit{

    Point2d first;
    Point2d second;

    IAtom firstAtom;
    IAtom secondAtom;
    IBond bond;

    /**
     * Creates an edit representing the creation of a bond and it's atoms.<p>
     * Creates an atom at first and second point and a bond between them.
     * @param first position of the first atom in this bond.
     * @param second position of the second atom in this bond.
     * @return edit representing the creation.
     */
    public static CreateBond edit(Point2d first, Point2d second) {
        return new CreateBond( first, second );
    }

    private CreateBond(Point2d first,Point2d second) {
        this.first = first;
        this.second = second;
    }
    public void redo() {

        if(firstAtom==null && secondAtom ==null && bond==null) {
            firstAtom = model.getBuilder().newInstance(IAtom.class, "C", first );
            secondAtom = model.getBuilder().newInstance(IAtom.class, "C", second );
            bond = model.getBuilder().newInstance(IBond.class, firstAtom, secondAtom );
        }
         model.addAtom( firstAtom );
         model.addAtom( secondAtom );
         model.addBond( bond );

         updateHydrogenCount( firstAtom, secondAtom );
    }

    public void undo() {

        model.removeBond( bond );
        model.removeAtom( firstAtom );
        model.removeAtom( secondAtom );

    }

    public Set<Changed> getTypeOfChanges() {
        return changed( Changed.Structure );
    }
}
