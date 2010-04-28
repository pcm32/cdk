/* $RCSfile$
 * $Author$    
 * $Date$    
 * $Revision$
 * 
 * Copyright (C) 1997-2007  The Chemistry Development Kit (CDK) project
 * 
 * Contact: cdk-devel@lists.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA. 
 * 
 */
package org.openscience.cdk.interfaces;

import org.junit.Assert;
import org.junit.Test;

/**
 * Checks the functionality of {@link IChemModel} implementations.
 *
 * @cdk.module test-interfaces
 */
public abstract class AbstractChemModelTest extends AbstractChemObjectTest {

    @Test public void testSetMoleculeSet_IMoleculeSet() {
	    IChemModel chemModel = (IChemModel)newChemObject();
	    IMoleculeSet crystal = chemModel.getBuilder().newInstance(IMoleculeSet.class);
        chemModel.setMoleculeSet(crystal);
        Assert.assertEquals(crystal, chemModel.getMoleculeSet());
    }
    @Test public void testGetMoleculeSet() {
    	testSetMoleculeSet_IMoleculeSet();
    }
    
    @Test public void testSetReactionSet_IReactionSet() {
	    IChemModel chemModel = (IChemModel)newChemObject();
	    IReactionSet crystal = chemModel.getBuilder().newInstance(IReactionSet.class);
        chemModel.setReactionSet(crystal);
        Assert.assertEquals(crystal, chemModel.getReactionSet());
    }
    @Test public void testGetReactionSet() {
    	testSetReactionSet_IReactionSet();
    }
    
    @Test public void testSetRingSet_IRingSet() {
	    IChemModel chemModel = (IChemModel)newChemObject();
	    IRingSet crystal = chemModel.getBuilder().newInstance(IRingSet.class);
        chemModel.setRingSet(crystal);
        Assert.assertEquals(crystal, chemModel.getRingSet());
    }
    @Test public void testGetRingSet() {
        testSetRingSet_IRingSet();
    }
    
    @Test public void testSetCrystal_ICrystal() {
	    IChemModel chemModel = (IChemModel)newChemObject();
	    ICrystal crystal = chemModel.getBuilder().newInstance(ICrystal.class);
        chemModel.setCrystal(crystal);
        Assert.assertEquals(crystal, chemModel.getCrystal());
    }
    @Test public void testGetCrystal() {
        testSetCrystal_ICrystal();
    }
    
    @Test public void testToString() {
        IChemModel model = (IChemModel)newChemObject();
        String description = model.toString();
        for (int i=0; i< description.length(); i++) {
            Assert.assertTrue(description.charAt(i) != '\n');
            Assert.assertTrue(description.charAt(i) != '\r');
        }
    }

	@Test public void testClone() throws Exception {
        IChemModel model = (IChemModel)newChemObject();
        Object clone = model.clone();
        Assert.assertNotNull(clone);
        Assert.assertTrue(clone instanceof IChemModel);
    }    
        
    @Test public void testClone_IMoleculeSet() throws Exception {
        IChemModel model = (IChemModel)newChemObject();
        IChemModel clone = (IChemModel)model.clone();
        Assert.assertNull(clone.getMoleculeSet());
        
		model.setMoleculeSet(model.getBuilder().newInstance(IMoleculeSet.class));
        clone = (IChemModel)model.clone();
        Assert.assertNotNull(clone.getMoleculeSet());
        Assert.assertNotSame(model.getMoleculeSet(), clone.getMoleculeSet());
    }

    @Test public void testClone_IReactionSet() throws Exception {
        IChemModel model = (IChemModel)newChemObject();
        IChemModel clone = (IChemModel)model.clone();
        Assert.assertNull(clone.getReactionSet());
        
		model.setReactionSet(model.getBuilder().newInstance(IReactionSet.class));
        clone = (IChemModel)model.clone();
        Assert.assertNotNull(clone.getReactionSet());
        Assert.assertNotSame(model.getReactionSet(), clone.getReactionSet());
    }

    @Test public void testClone_Crystal() throws Exception {
		IChemModel model = (IChemModel)newChemObject();
        IChemModel clone = (IChemModel)model.clone();
        Assert.assertNull(clone.getCrystal());
        
		model.setCrystal(model.getBuilder().newInstance(ICrystal.class));
        clone = (IChemModel)model.clone();
        Assert.assertNotNull(clone.getCrystal());
        Assert.assertNotSame(model.getCrystal(), clone.getCrystal());
    }

    @Test public void testClone_RingSet() throws Exception {
        IChemModel model = (IChemModel)newChemObject();
        IChemModel clone = (IChemModel)model.clone();
        Assert.assertNull(clone.getRingSet());
        
		model.setRingSet(model.getBuilder().newInstance(IRingSet.class));
        clone = (IChemModel)model.clone();
        Assert.assertNotNull(clone.getRingSet());
        Assert.assertNotSame(model.getRingSet(), clone.getRingSet());
    }

}
